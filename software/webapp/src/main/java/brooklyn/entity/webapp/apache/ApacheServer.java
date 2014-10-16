package brooklyn.entity.webapp.apache;


import brooklyn.catalog.Catalog;
import brooklyn.config.ConfigKey;
import brooklyn.entity.basic.ConfigKeys;
import brooklyn.entity.basic.SoftwareProcess;
import brooklyn.entity.proxying.ImplementedBy;
import brooklyn.entity.trait.HasShortName;
import brooklyn.entity.webapp.PhpWebAppService;
import brooklyn.entity.webapp.PhpWebAppSoftwareProcess;
import brooklyn.event.AttributeSensor;
import brooklyn.event.basic.BasicAttributeSensorAndConfigKey.StringAttributeSensorAndConfigKey;
import brooklyn.event.basic.BasicAttributeSensorAndConfigKey;
import brooklyn.event.basic.BasicConfigKey;
import brooklyn.event.basic.PortAttributeSensorAndConfigKey;
import brooklyn.event.basic.Sensors;
import brooklyn.util.flags.SetFromFlag;

@Catalog(name="HTTP Server", description="Apache HTTP Server Project is an open-source HTTP server")
@ImplementedBy(ApacheServerImpl.class)
public interface ApacheServer extends PhpWebAppSoftwareProcess, PhpWebAppService, HasShortName {

    @SetFromFlag("version")
    ConfigKey<String> SUGGESTED_VERSION =
            ConfigKeys.newConfigKeyWithDefault(SoftwareProcess.SUGGESTED_VERSION, "2.4.7");

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
    @SetFromFlag("configuration_available_sites_file")
    ConfigKey<String> AVAILABLES_SITES_CONFIGURATION_FILE =
            ConfigKeys.newConfigKey("available.sites.configuration.file", "configuration apache file where the application folders are configured", "BrooklynDeployRunDir.conf");

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

    @SetFromFlag("monitor_url")
    ConfigKey<String> MONITOR_URL =
            ConfigKeys.newConfigKey("apache.monitor.url", "data page about server status", "server-status?auto");

    @SetFromFlag("monitor_url_up")
    AttributeSensor<Boolean> MONITOR_URL_UP =
            Sensors.newBooleanSensor("apache.monitor.up", "Monitor server is responding with OK");

    @SetFromFlag("total_accesses")
    AttributeSensor<Long> TOTAL_ACCESSES=
            Sensors.newLongSensor("apache.total.accesses", "Accesses to apache");

    @SetFromFlag("total_kbyte")
    AttributeSensor<Long> TOTAL_KBYTE=
            Sensors.newLongSensor("apache.total.kbyte", "Total traffic in KB");

    @SetFromFlag("cpu_load")
    AttributeSensor<Double> CPU_LOAD=
            Sensors.newDoubleSensor("apache.cpu.load", "CPU load percent");

    @SetFromFlag("up_time")
    AttributeSensor<Long> UP_TIME=
            Sensors.newLongSensor("apache.up.time", "Time spent setting up the server");

    @SetFromFlag("request_per_sec")
    AttributeSensor<Double> REQUEST_PER_SEC=
            Sensors.newDoubleSensor("apache.request.per.sec", "Request per sec managed by the server");

    @SetFromFlag("bytes_per_sec")
    AttributeSensor<Double> BYTES_PER_SEC=
            Sensors.newDoubleSensor("apache.bytes.per.sec", "Bytes per second");

    @SetFromFlag("bytes_per_req")
    AttributeSensor<Double> BYTES_PER_REQ=
            Sensors.newDoubleSensor("apache.bytes.per.req", "Bytes per requests");

    @SetFromFlag("busy_workers")
        AttributeSensor<Integer> BUSY_WORKERS=
            Sensors.newIntegerSensor("apache.busy.workers", "Number of busy worker");



}
