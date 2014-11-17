/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package brooklyn.entity.rebind;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;
import io.brooklyn.camp.BasicCampPlatform;
import io.brooklyn.camp.test.mock.web.MockWebPlatform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import brooklyn.camp.lite.CampPlatformWithJustBrooklynMgmt;
import brooklyn.camp.lite.TestAppAssemblyInstantiator;
import brooklyn.catalog.CatalogItem;
import brooklyn.catalog.CatalogLoadMode;
import brooklyn.config.BrooklynProperties;
import brooklyn.config.BrooklynServerConfig;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.internal.BrooklynFeatureEnablement;
import brooklyn.management.internal.LocalManagementContext;
import brooklyn.policy.basic.AbstractPolicy;
import brooklyn.test.entity.TestEntity;

import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;

public class RebindCatalogItemTest extends RebindTestFixtureWithApp {

    private static final Logger LOG = LoggerFactory.getLogger(RebindCatalogItemTest.class);
    public static class MyPolicy extends AbstractPolicy {}
    private boolean catalogPersistenceWasEnabled;

    @BeforeMethod(alwaysRun = true)
    @Override
    public void setUp() throws Exception {
        super.setUp();
        catalogPersistenceWasEnabled = BrooklynFeatureEnablement.isEnabled(BrooklynFeatureEnablement.FEATURE_CATALOG_PERSISTENCE_PROPERTY);
        BrooklynFeatureEnablement.enable(BrooklynFeatureEnablement.FEATURE_CATALOG_PERSISTENCE_PROPERTY);
        BasicCampPlatform platform = new CampPlatformWithJustBrooklynMgmt(origManagementContext);
        MockWebPlatform.populate(platform, TestAppAssemblyInstantiator.class);
        origApp.createAndManageChild(EntitySpec.create(TestEntity.class));
    }

    @AfterMethod(alwaysRun = true)
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        BrooklynFeatureEnablement.setEnablement(BrooklynFeatureEnablement.FEATURE_CATALOG_PERSISTENCE_PROPERTY, catalogPersistenceWasEnabled);
    }

    @Override
    protected LocalManagementContext createOrigManagementContext() {
        BrooklynProperties properties = BrooklynProperties.Factory.newDefault();
        properties.put(BrooklynServerConfig.BROOKLYN_CATALOG_URL, "classpath://brooklyn/entity/rebind/rebind-catalog-item-test-catalog.xml");
        properties.put(BrooklynServerConfig.CATALOG_LOAD_MODE, CatalogLoadMode.LOAD_BROOKLYN_CATALOG_URL);
        return RebindTestUtils.managementContextBuilder(mementoDir, classLoader)
                .properties(properties)
                .persistPeriodMillis(getPersistPeriodMillis())
                .forLive(useLiveManagementContext())
                .buildStarted();
    }

    @Override
    protected LocalManagementContext createNewManagementContext() {
        BrooklynProperties properties = BrooklynProperties.Factory.newDefault();
        properties.put(BrooklynServerConfig.BROOKLYN_CATALOG_URL, "classpath://brooklyn/entity/rebind/rebind-catalog-item-test-catalog.xml");
        properties.put(BrooklynServerConfig.CATALOG_LOAD_MODE, CatalogLoadMode.LOAD_BROOKLYN_CATALOG_URL_IF_NO_PERSISTED_STATE);
        return RebindTestUtils.managementContextBuilder(mementoDir, classLoader)
                .properties(properties)
                .forLive(useLiveManagementContext())
                .emptyCatalog(useEmptyCatalog())
                .buildUnstarted();
    }

    @Test
    public void testPersistsEntityFromCatalogXml() {
        assertEquals(Iterables.size(origManagementContext.getCatalog().getCatalogItems()), 1);
        rebindAndAssertCatalogsAreEqual();
    }

    @Test
    public void testAddAndRebindEntity() throws Exception {
        String yaml = "name: rebind-yaml-catalog-item-test\n" +
                "services:\n" +
                "- type: io.camp.mock:AppServer";
        CatalogItem<?, ?> added = origManagementContext.getCatalog().addItem(yaml);
        LOG.info("Added item to catalog: {}, id={}", added, added.getId());
        rebindAndAssertCatalogsAreEqual();
    }

    @Test(enabled = false)
    public void testAddAndRebindTemplate() {
        // todo: could use (deprecated, perhaps wrongly) BBC.addItem(Class/CatalogItem)
        fail("Unimplemented because the catalogue does not currently distinguish between application templates and entities");
    }

    @Test
    public void testAddAndRebindPolicy() {
        // Doesn't matter that SamplePolicy doesn't exist
        String yaml = "name: Test Policy\n" +
                "brooklyn.catalog:\n" +
                "  id: sample_policy\n" +
                "  version: 0.1.0\n" +
                "brooklyn.policies: \n" +
                "- type: brooklyn.entity.rebind.RebindCatalogItemTest$MyPolicy\n" +
                "  brooklyn.config:\n" +
                "    cfg1: 111\n" +
                "    cfg2: 222";
        CatalogItem<?, ?> added = origManagementContext.getCatalog().addItem(yaml);
        LOG.info("Added item to catalog: {}, id={}", added, added.getId());
        rebindAndAssertCatalogsAreEqual();
    }

    @Test(enabled = false)
    public void testAddAndRebindEnricher() {
        fail("unimplemented");
    }

    @Test(invocationCount = 3)
    public void testDeletedCatalogItemIsNotPersisted() {
        assertEquals(Iterables.size(origManagementContext.getCatalog().getCatalogItems()), 1);
        CatalogItem<Object, Object> toRemove = Iterables.getOnlyElement(origManagementContext.getCatalog().getCatalogItems());
        // Must make sure that the original catalogue item is not managed and unmanaged in the same
        // persistence window. Because BrooklynMementoPersisterToObjectStore applies writes/deletes
        // asynchronously the winner is down to a race and the test might pass or fail.
        origManagementContext.getRebindManager().forcePersistNow();
        origManagementContext.getCatalog().deleteCatalogItem(toRemove.getId());
        assertEquals(Iterables.size(origManagementContext.getCatalog().getCatalogItems()), 0);
        rebindAndAssertCatalogsAreEqual();
        assertEquals(Iterables.size(newManagementContext.getCatalog().getCatalogItems()), 0);
    }

    @Test(invocationCount = 3)
    public void testCanTagCatalogItemAfterRebind() {
        assertEquals(Iterables.size(origManagementContext.getCatalog().getCatalogItems()), 1);
        CatalogItem<Object, Object> toTag = Iterables.getOnlyElement(origManagementContext.getCatalog().getCatalogItems());
        final String tag = "tag1";
        toTag.tags().addTag(tag);
        assertTrue(toTag.tags().containsTag(tag));

        rebindAndAssertCatalogsAreEqual();

        toTag = Iterables.getOnlyElement(newManagementContext.getCatalog().getCatalogItems());
        assertTrue(toTag.tags().containsTag(tag));
        toTag.tags().removeTag(tag);
    }

    private void rebindAndAssertCatalogsAreEqual() {
        try {
            rebind();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
        assertCatalogsEqual(newManagementContext.getCatalog(), origManagementContext.getCatalog());
    }

}
