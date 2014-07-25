package brooklyn.entity.webapp.apache;


import brooklyn.catalog.Catalog;
import brooklyn.config.ConfigKey;
import brooklyn.entity.basic.ConfigKeys;
import brooklyn.entity.basic.SoftwareProcess;
import brooklyn.entity.proxying.ImplementedBy;
import brooklyn.entity.trait.HasShortName;
import brooklyn.entity.webapp.PhpWebAppService;
import brooklyn.entity.webapp.PhpWebAppSoftwareProcess;
import brooklyn.event.basic.BasicAttributeSensorAndConfigKey.StringAttributeSensorAndConfigKey;
import brooklyn.event.basic.BasicAttributeSensorAndConfigKey;
import brooklyn.event.basic.BasicConfigKey;
import brooklyn.event.basic.PortAttributeSensorAndConfigKey;
import brooklyn.util.flags.SetFromFlag;

@Catalog(name="Apache Web Server", description="ApacheServer:  is a web server application which can be connect" +
        "with ", iconUrl="classpath:///jboss-logo.png")
@ImplementedBy(ApacheServerImpl.class)
public interface ApacheServer extends PhpWebAppSoftwareProcess, PhpWebAppService, HasShortName {

    @SetFromFlag("version")
    ConfigKey<String> SUGGESTED_VERSION =
            ConfigKeys.newConfigKeyWithDefault(SoftwareProcess.SUGGESTED_VERSION, "2.4.7");

    @SetFromFlag("deployment_timeout")
    ConfigKey<Integer> DEPLOYMENT_TIMEOUT =
            ConfigKeys.newConfigKey("php.app.deploymenttimeout", "Deployment timeout, in seconds", 600);

    @SetFromFlag("server_status_url")
    public static final ConfigKey<String> SERVER_STATUS_URL = new BasicConfigKey<String>(
            String.class, "apache.server.status", "URL where the status of the server can be consulted");

    //TODO Review This definition
    @SetFromFlag("install_dir")
    BasicAttributeSensorAndConfigKey<String> INSTALL_DIR = new StringAttributeSensorAndConfigKey(
            SoftwareProcess.INSTALL_DIR, "/etc/apache2");
    //TODO Review This definition

    @SetFromFlag("configuration_dir")
    BasicAttributeSensorAndConfigKey<String> CONFIGURATION_DIR =
            new StringAttributeSensorAndConfigKey("apache.configuration.dir", "Configuration Dir", "/etc/apache2");

    @SetFromFlag("available_sites_configuration_folder")
    BasicAttributeSensorAndConfigKey<String> AVAILABLE_SITES_CONFIGURATION_FOLDER =
            new StringAttributeSensorAndConfigKey("apache.configuration.dir.available.sites", "Folder that contains the configuration and pointed to " +
                    "the deploy and run folders where deploy the apps ", "/sites-available");

    @SetFromFlag("deploy_run_dir")
    ConfigKey<String> DEPLOY_RUN_DIR =
            ConfigKeys.newConfigKey("apache.deploy.run.dir", "Folder to deploy and run the App. Pointed from " +
                    "AVAILABLE_SITES_CONFIGURATION_FOLDER ", "/var/www");

    @SetFromFlag("defaultGroup")
    public static final ConfigKey<String> DEFAULT_GROUP = ConfigKeys.newStringConfigKey(
            "apache.default.group", "default of Apache  to run the applications deployed in DEPLOY_RUN_DIR", "www-data");

    @SetFromFlag("http_port")
    PortAttributeSensorAndConfigKey HTTP_PORT=
            new PortAttributeSensorAndConfigKey("apache.http.port", "Http port where Apache is listening", "80");

}
