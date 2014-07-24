package brooklyn.entity.webapp.apache;

import brooklyn.entity.basic.ApplicationBuilder;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.entity.webapp.HttpsSslConfig;
import brooklyn.entity.webapp.jboss.JBoss7Server;
import brooklyn.location.LocationSpec;
import brooklyn.location.basic.LocalhostMachineProvisioningLocation;
import brooklyn.location.basic.SshMachineLocation;
import brooklyn.management.LocationManager;
import brooklyn.management.ManagementContext;
import brooklyn.test.HttpTestUtils;
import brooklyn.test.entity.TestApplication;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * Created by Jose on 18/07/2014.
 */
public class ApacheServerIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(ApacheServerIntegrationTest.class);


    private SshMachineLocation loc;
    private ManagementContext managementContext;
    private LocationManager locationManager;

    private TestApplication app;
    private String gitRepoURLApp="https://github.com/kiuby88/phpHelloWorld.git";

    @BeforeMethod(alwaysRun=true)
    public void setUp() throws Exception {
        app = ApplicationBuilder.newManagedApp(TestApplication.class);
        managementContext = app.getManagementContext();
        locationManager = managementContext.getLocationManager();
        loc = locationManager.createLocation(LocationSpec.create(SshMachineLocation.class)
                .configure("address", "localhost"));
    }

    @AfterMethod(alwaysRun=true)
    public void tearDown() throws Exception {
        log.info("Destroy all {}", new Object[]{this});
        if (app != null)
           Entities.destroyAll(app.getManagementContext());
    }

    @Test(groups = {"Integration"})
    public void testHttps() throws Exception {
        try {
            integrationTestHttp();
        }
        catch (Exception e){
            log.warn("Exception catched {} ", new Object[]{e.fillInStackTrace()});
            tearDown();
            throw e;
        }
    }

    private void integrationTestHttp() throws Exception{
        final ApacheServer server = app.createAndManageChild(EntitySpec.create(ApacheServer.class)
                .configure("app_git_repo_url", gitRepoURLApp)
                .configure("http_port", "81")
                .configure(ApacheServer.ENABLED_PROTOCOLS, ImmutableSet.of("http")));
        //.configure(JBoss7Server.HTTPS_SSL_CONFIG, new HttpsSslConfig().keyAlias("myname").keystorePassword("mypass").keystoreUrl(keystoreFile.getAbsolutePath())));

        //app.start(ImmutableList.of(localhostProvisioningLocation));
        app.start(ImmutableList.of(loc));

        String httpUrl = "http://"+server.getAttribute(ApacheServer.HOSTNAME)+":"+server.getAttribute(ApacheServer.HTTP_PORT)+"/";
        log.warn("httpURL - VALUE generate by composition: --> "+httpUrl);
        log.warn("ROOT_URL: --> "+server.getAttribute(ApacheServer.ROOT_URL).toLowerCase());
        assertEquals(server.getAttribute(ApacheServer.ROOT_URL).toLowerCase(), httpUrl.toLowerCase());
        System.out.println("JOSE--FinDelTest");

    }

}
