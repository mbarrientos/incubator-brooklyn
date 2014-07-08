package brooklyn.entity.webapp;


import brooklyn.entity.basic.SoftwareProcessDriver;
import java.util.Set;


public interface PhpWebAppDriver extends SoftwareProcessDriver {



    Set<String> getEnabledProtocols();

    Integer getHttpPort();

    Integer getHttpsPort();


    HttpsSslConfig getHttpsSslConfig();


    String deployGitResource(String url, String targetName);

    String deployTarballResource(String url, String targetName);

    void undeploy(String targetName);

    SourceNameResolver getSourceNameResolver();




}
