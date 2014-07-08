package brooklyn.entity.webapp;

import brooklyn.config.ConfigKey;
import brooklyn.entity.annotation.Effector;
import brooklyn.entity.annotation.EffectorParam;
import brooklyn.entity.basic.ConfigKeys;
import brooklyn.entity.basic.MethodEffector;
import brooklyn.entity.basic.SoftwareProcess;
import brooklyn.entity.basic.SoftwareProcessDriver;
import brooklyn.entity.proxying.ImplementedBy;
import brooklyn.event.AttributeSensor;
import brooklyn.event.basic.BasicAttributeSensor;
import brooklyn.event.basic.BasicAttributeSensorAndConfigKey;
import brooklyn.event.basic.PortAttributeSensorAndConfigKey;
import brooklyn.util.flags.SetFromFlag;
import brooklyn.event.basic.BasicAttributeSensorAndConfigKey.StringAttributeSensorAndConfigKey;


import java.util.Set;


@ImplementedBy(PhpWebAppSoftwareProcessImpl.class)
public interface PhpWebAppSoftwareProcess extends SoftwareProcess, PhpWebAppService{

    public static final AttributeSensor<Set<String>> DEPLOYED_PHP_APPS = new BasicAttributeSensor(
            Set.class, "webapp.deployedApps", "Names of archives/contexts that are currently deployed");
    public static final MethodEffector<Void> DEPLOY_GIT_RESOURCE = new MethodEffector<Void>(PhpWebAppSoftwareProcess.class, "deployGitResource");
    public static final MethodEffector<Void> DEPLOY_TARBALL_RESOURCE = new MethodEffector<Void>(PhpWebAppSoftwareProcess.class, "deployTarballResource");
    public static final MethodEffector<Void> UNDEPLOY = new MethodEffector<Void>(PhpWebAppSoftwareProcess.class, "undeploy");

    ConfigKey<String> SUGGESTED_VERSION= ConfigKeys.newConfigKeyWithDefault(SoftwareProcess.SUGGESTED_VERSION, "5");

    /**
     * So, the download method have to define the path.
     * E.g. a git repo create the folder when the app will be download.
     * @param url A url of the git repo where the application are stored. Currently, https url are supported.
     * @param targetName name of the application used to deploy it.
     */
    @Effector(description="Deploys the given artifact, from a source URL, to a given deployment filename/context")
    public void deployGitResource(
            @EffectorParam(name="url", description="URL of git Repo file") String url,
            @EffectorParam(name="targetName", description="Application Name") String targetName);

    /**
     * So, the download method have to define the path.
     * E.g. a git repo create the folder when the app will be download.
     * @param url A url of the git repo where the application are stored. Currently, https url are supported.
     * @param targetName name of the application used to deploy it.
     */
    @Effector(description="Deploys the given artifact, from a source URL, to a given deployment filename/context")
    public void deployTarballResource(
            @EffectorParam(name="url", description="URL of tarball resource") String url,
            @EffectorParam(name="targetName", description="Application Name") String targetName);

    /**
     * For the DEPLOYED_PHP_APP to be updated, the input must match the result of the call to deploy
     */
    @Effector(description="Undeploys the given context/artifact")
    public void undeploy(
            @EffectorParam(name="targetName") String targetName);

}
