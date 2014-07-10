package brooklyn.entity.webapp;

import brooklyn.entity.Entity;
import brooklyn.entity.annotation.Effector;
import brooklyn.entity.annotation.EffectorParam;
import brooklyn.entity.basic.SoftwareProcessImpl;
import brooklyn.location.access.BrooklynAccessUtils;
import com.google.common.base.Throwables;
import com.google.common.collect.Sets;
import com.google.common.net.HostAndPort;
import org.slf4j.LoggerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by Jose on 07/07/2014.
 */
public abstract class PhpWebAppSoftwareProcessImpl extends SoftwareProcessImpl implements PhpWebAppSoftwareProcess{
    private static final Logger LOG= LoggerFactory.getLogger(PhpWebAppSoftwareProcessImpl.class);

    public PhpWebAppSoftwareProcessImpl(){ super(); }

    public PhpWebAppSoftwareProcessImpl(Entity parent){
        this(new LinkedHashMap(),parent);
    }

    public PhpWebAppSoftwareProcessImpl(Map flags){
        this(flags, null);
    }

    public PhpWebAppSoftwareProcessImpl(Map flags, Entity parent) {
        super(flags, parent);
    }

    public PhpWebAppDriver getDriver(){
        return (PhpWebAppDriver) super.getDriver();
    }

    protected Set<String> getEnabledProtocols() {
        return getAttribute(PhpWebAppSoftwareProcess.ENABLED_PROTOCOLS);
    }

    protected Set<String> getDeployedPhpAppsAttribute() {
        return getAttribute(DEPLOYED_PHP_APPS);
    }

    protected void setDeployedPhpAppsAttribute(Set<String> deployedPhpApps){
        setAttribute(DEPLOYED_PHP_APPS, deployedPhpApps);
    }

    @Override
    public void connectSensors(){
        super.connectSensors();
        WebAppServiceMethods.connectWebAppServerPolicies(this);
    }

    @Override
    protected void doStop(){
        super.doStop();
        //zero our workrate derived workrates.
        //TODO might not be enough, as a policy may still be executing and have a record of historic vals;
        // should remove policies
        //also nor sure we want this; implies more generally a resposibility for sensor to announce things
        //disconnected
        putEnricherValuesToNullValue();
    }

    private void putEnricherValuesToNullValue(){
        setAttribute(REQUESTS_PER_SECOND_LAST, 0D);
        setAttribute(REQUESTS_PER_SECOND_IN_WINDOW, 0D);
    }

    // TODO thread-safety issues: if multiple concurrent calls, may break (e.g. deployment_wars being reset)
    public void deployInitialWars() {
        initDeployAppAttribteIfIsNull();
        deployInitialAppGitSource();
    }

    private void initDeployAppAttribteIfIsNull(){
        if (getDeployedPhpAppsAttribute() == null)
            setDeployedPhpAppsAttribute(Sets.<String>newLinkedHashSet());
    }

    private void deployInitialAppGitSource() {
        String gitRepoUrl = getConfig(APP_GIT_REPO_URL);
        if (gitRepoUrl!=null)
            deploy(gitRepoUrl);
    }

    @Effector(description="Deploys the given artifact, from a source URL, using the ")
    public void deploy(
            @EffectorParam(name="url", description="URL of WAR file") String url) {
        try {
            deployPhpApp(url);
        } catch (RuntimeException e) {
            // Log and propagate, so that log says which entity had problems...
            LOG.warn("Error deploying '"+url+"' on "+toString()+"; rethrowing...", e);
            throw Throwables.propagate(e);
        }
    }

    private void deployPhpApp(String url){
        checkNotNull(url, "url");
        PhpWebAppDriver driver =   getDriver();
        String deployedAppName = driver.deploy(url);
        updateDeploymentSensorToDeployAnApp(deployedAppName);
    }

    private void updateDeploymentSensorToDeployAnApp(String deployedAppName){
        Set<String> deployedPhpApps = getDeployedPhpAppsAttribute();
        if (deployedPhpApps == null) {
            deployedPhpApps = Sets.newLinkedHashSet();
        }
        deployedPhpApps.add(deployedAppName);
        setDeployedPhpAppsAttribute(deployedPhpApps);
    }

    /** For the DEPLOYED_PHP_APP to be updated, the input must match the result of the call to deploy */
    @Override
    @Effector(description="Undeploys the given context/artifact")
    public void undeploy(
            @EffectorParam(name="deployedAppName") String targetName) {
        try {

            undeployPhpApp(targetName);

        } catch (RuntimeException e) {
            // Log and propagate, so that log says which entity had problems...
            LOG.warn("Error undeploying '"+targetName+"' on "+toString()+"; rethrowing...", e);
            throw Throwables.propagate(e);
        }
    }
    private void undeployPhpApp(String targetName){
        PhpWebAppDriver driver = getDriver();
        driver.undeploy(targetName);
        updateDeploymentSensorToUndeployAnApp(targetName);
    }

    private void updateDeploymentSensorToUndeployAnApp(String targetName){
        initDeployAppAttribteIfIsNull();
        Set<String> deployedPhpApps=getDeployedPhpAppsAttribute();
        PhpWebAppDriver driver = getDriver();
        deployedPhpApps.remove( driver.getFilenameContextMapper().convertDeploymentTargetNameToContext(targetName) );
        setDeployedPhpAppsAttribute(deployedPhpApps);
    }

}
