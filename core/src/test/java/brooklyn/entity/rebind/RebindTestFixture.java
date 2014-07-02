package brooklyn.entity.rebind;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import brooklyn.entity.basic.Entities;
import brooklyn.entity.basic.StartableApplication;
import brooklyn.entity.rebind.persister.BrooklynMementoPersisterToObjectStore;
import brooklyn.entity.rebind.persister.FileBasedObjectStore;
import brooklyn.entity.rebind.persister.PersistMode;
import brooklyn.internal.BrooklynFeatureEnablement;
import brooklyn.management.ManagementContext;
import brooklyn.management.ha.HighAvailabilityMode;
import brooklyn.management.internal.LocalManagementContext;
import brooklyn.mementos.BrooklynMementoManifest;
import brooklyn.test.entity.LocalManagementContextForTests;
import brooklyn.util.os.Os;
import brooklyn.util.time.Duration;

public abstract class RebindTestFixture<T extends StartableApplication> {

    private static final Logger LOG = LoggerFactory.getLogger(RebindTestFixture.class);

    protected static final Duration TIMEOUT_MS = Duration.TEN_SECONDS;

    protected ClassLoader classLoader = getClass().getClassLoader();
    protected LocalManagementContext origManagementContext;
    protected File mementoDir;
    
    protected T origApp;
    protected T newApp;
    protected ManagementContext newManagementContext;

    private boolean origPolicyPersistenceEnabled;
    private boolean origEnricherPersistenceEnabled;
    
    @BeforeMethod(alwaysRun=true)
    public void setUp() throws Exception {
        origPolicyPersistenceEnabled = BrooklynFeatureEnablement.enable(BrooklynFeatureEnablement.FEATURE_POLICY_PERSISTENCE_PROPERTY);
        origEnricherPersistenceEnabled = BrooklynFeatureEnablement.enable(BrooklynFeatureEnablement.FEATURE_ENRICHER_PERSISTENCE_PROPERTY);
        
        mementoDir = Os.newTempDir(getClass());
        origManagementContext = RebindTestUtils.newPersistingManagementContext(mementoDir, classLoader, getPersistPeriodMillis());
        origApp = createApp();
        
        LOG.info("Test "+getClass()+" persisting to "+mementoDir);
    }
    
    protected int getPersistPeriodMillis() {
        return 1;
    }
    
    /** optionally, create the app as part of every test; can be no-op if tests wish to set origApp themselves */
    protected abstract T createApp();

    @AfterMethod(alwaysRun=true)
    public void tearDown() throws Exception {
        try {
            if (origApp != null) Entities.destroyAll(origApp.getManagementContext());
            if (newApp != null) Entities.destroyAll(newApp.getManagementContext());
            if (newManagementContext != null) Entities.destroyAll(newManagementContext);
            origApp = null;
            newApp = null;
            newManagementContext = null;
    
            if (origManagementContext != null) Entities.destroyAll(origManagementContext);
            if (mementoDir != null) FileBasedObjectStore.deleteCompletely(mementoDir);
            origManagementContext = null;
        } finally {
            BrooklynFeatureEnablement.setEnablement(BrooklynFeatureEnablement.FEATURE_POLICY_PERSISTENCE_PROPERTY, origPolicyPersistenceEnabled);
            BrooklynFeatureEnablement.setEnablement(BrooklynFeatureEnablement.FEATURE_ENRICHER_PERSISTENCE_PROPERTY, origEnricherPersistenceEnabled);
        }
    }

    /** rebinds, and sets newApp */
    protected T rebind() throws Exception {
        if (newApp!=null || newManagementContext!=null) throw new IllegalStateException("already rebinded");
        newApp = rebind(true);
        newManagementContext = newApp.getManagementContext();
        return newApp;
    }

    protected T rebind(boolean checkSerializable) throws Exception {
        // TODO What are sensible defaults?!
        return rebind(checkSerializable, false);
    }
    
    @SuppressWarnings("unchecked")
    protected T rebind(boolean checkSerializable, boolean terminateOrigManagementContext) throws Exception {
        RebindTestUtils.waitForPersisted(origApp);
        if (checkSerializable) {
            RebindTestUtils.checkCurrentMementoSerializable(origApp);
        }
        if (terminateOrigManagementContext) {
            origManagementContext.terminate();
        }
        return (T) RebindTestUtils.rebind(mementoDir, getClass().getClassLoader());
    }

    @SuppressWarnings("unchecked")
    protected T rebind(RebindExceptionHandler exceptionHandler) throws Exception {
        RebindTestUtils.waitForPersisted(origApp);
        return (T) RebindTestUtils.rebind(mementoDir, getClass().getClassLoader(), exceptionHandler);
    }

    @SuppressWarnings("unchecked")
    protected T rebind(ManagementContext newManagementContext, RebindExceptionHandler exceptionHandler) throws Exception {
        RebindTestUtils.waitForPersisted(origApp);
        return (T) RebindTestUtils.rebind(newManagementContext, mementoDir, getClass().getClassLoader(), exceptionHandler);
    }
    
    protected BrooklynMementoManifest loadMementoManifest() throws Exception {
        newManagementContext = new LocalManagementContextForTests();
        FileBasedObjectStore objectStore = new FileBasedObjectStore(mementoDir);
        objectStore.injectManagementContext(newManagementContext);
        objectStore.prepareForSharedUse(PersistMode.AUTO, HighAvailabilityMode.DISABLED);
        BrooklynMementoPersisterToObjectStore persister = new BrooklynMementoPersisterToObjectStore(objectStore, classLoader);
        RebindExceptionHandler exceptionHandler = new RecordingRebindExceptionHandler(RebindManager.RebindFailureMode.FAIL_AT_END, RebindManager.RebindFailureMode.FAIL_AT_END);
        BrooklynMementoManifest mementoManifest = persister.loadMementoManifest(exceptionHandler);
        persister.stop();
        return mementoManifest;
    }
}
