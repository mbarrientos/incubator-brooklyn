package brooklyn.entity.webapp;

import brooklyn.config.ConfigKey;
import brooklyn.entity.basic.ConfigKeys;
import brooklyn.entity.basic.SoftwareProcessDriver;
import brooklyn.entity.proxying.ImplementedBy;
import brooklyn.util.flags.SetFromFlag;

import java.io.File;
import java.util.Set;

/**
 * Created by Jose on 04/07/2014.
 */

public interface PhpWebAppDriver extends SoftwareProcessDriver {

    @SetFromFlag("app_user")
    ConfigKey<String> APP_USER = ConfigKeys.newStringConfigKey("php.app.user", "The user to run the PHP application as",
            "www-data");

    Set<String> getEnabledProtocols();

    Integer getHttpPort();

    Integer getHttpsPort();

    HttpsSslConfig getHttpsSslConfig();


    String deploy(String url);

    void undeploy(String targetName);

    FilenameToWebContextMapper getFilenameContextMapper();



}
