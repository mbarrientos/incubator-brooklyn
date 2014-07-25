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



    Set<String> getEnabledProtocols();

    Integer getHttpPort();

    Integer getHttpsPort();


    HttpsSslConfig getHttpsSslConfig();


    String deploy(String url);

    void undeploy(String targetName);

   //FilenameToWebContextMapper getFilenameContextMapper();



}
