package brooklyn.entity.software;

import brooklyn.entity.Effector;
import brooklyn.entity.basic.ApplicationBuilder;
import brooklyn.entity.basic.Attributes;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.basic.EntityLocal;
import brooklyn.entity.effector.Effectors;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.entity.software.http.HttpRequestSensor;
import brooklyn.event.AttributeSensor;
import brooklyn.event.basic.Sensors;
import brooklyn.location.Location;
import brooklyn.test.Asserts;
import brooklyn.test.entity.TestApplication;
import brooklyn.test.entity.TestEntity;
import brooklyn.util.collections.MutableMap;
import brooklyn.util.config.ConfigBag;
import com.google.common.collect.ImmutableList;
import org.apache.http.HttpRequest;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import sun.rmi.runtime.Log;

import java.io.File;

import static org.testng.Assert.assertTrue;

/**
 * Created by MBarrientos on 01/07/2014.
 */
public class HttpRequestSensorTest {
    final static AttributeSensor<String> SENSOR_STRING = Sensors.newStringSensor("aString", "");
    final static AttributeSensor<String> SENSOR_JSON_OBJECT = Sensors.newStringSensor("aJSONObject","");
    final static AttributeSensor<String> SENSOR_URI = Sensors.newStringSensor("uri","");

    private TestApplication app;
    private EntityLocal entity;

    @BeforeMethod(alwaysRun=true)
    public void setUp() throws Exception {
        app = ApplicationBuilder.newManagedApp(TestApplication.class);
        entity = app.createAndManageChild(EntitySpec.create(TestEntity.class).location(app.newLocalhostProvisioningLocation().obtain()));
        app.start(ImmutableList.<Location>of());
    }

    @AfterMethod(alwaysRun=true)
    public void tearDown() throws Exception {
        if (app != null) Entities.destroyAll(app.getManagementContext());
    }

    @Test(groups="Integration")
    public void testHttpSensor() throws Exception {
        new HttpRequestSensor<String>(ConfigBag.newInstance()
                .configure(HttpRequestSensor.SENSOR_NAME, SENSOR_STRING.getName())
                .configure(HttpRequestSensor.SENSOR_JSON_OBJECT, "myKey")
                .configure(HttpRequestSensor.SENSOR_URI, "http://echo.jsontest.com/myKey/myValue"))
            .apply(entity);
        entity.setAttribute(Attributes.SERVICE_UP, true);


        Asserts.succeedsEventually(new Runnable() {
            public void run() {
                String val = entity.getAttribute(SENSOR_STRING);
                assertTrue(val != null);
            }
        });

        String val = entity.getAttribute(SENSOR_STRING);
        Assert.assertEquals(val, "myValue", "val=" + val);
    }
}
