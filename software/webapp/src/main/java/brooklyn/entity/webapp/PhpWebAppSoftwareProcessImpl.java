package brooklyn.entity.webapp;

import brooklyn.entity.Entity;
import brooklyn.entity.annotation.Effector;
import brooklyn.entity.annotation.EffectorParam;
import brooklyn.entity.basic.SoftwareProcessImpl;
import com.google.common.base.Throwables;
import com.google.common.collect.Sets;
import org.slf4j.LoggerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

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

    //TODO May Move this method to superclass
    protected Set<String> getEnabledProtocols() {
        return getAttribute(PhpWebAppSoftwareProcess.ENABLED_PROTOCOLS);
    }

    //TODO May Move this method to superclass
    protected Set<String> getDeployedPhpAppsAttribute() {
        return getAttribute(DEPLOYED_PHP_APP);
    }

    //TODO May Move this method to superclass
    protected void setDeployedPhpAppsAttribute(Set<String> deployedPhpApps){
        setAttribute(DEPLOYED_PHP_APP, deployedPhpApps);
    }

    @Override
    public void connectSensors(){
        super.connectSensors();
        WebAppServiceMethods.connectWebAppServerPolicies(this);
    }

    @Effector(description="Deploys the given artifact, from a source URL, to a given deployment filename/context")
    public void deploy(
            @EffectorParam(name="url", description="URL of WAR file") String url,
            @EffectorParam(name="targetName", description="context path where PHP_APP should be deployed (/ for ROOT)") String targetName) {
        try {
            deployPhpApp(url, targetName);
        } catch (RuntimeException e) {
            // Log and propagate, so that log says which entity had problems...
            LOG.warn("Error deploying '"+url+"' to "+targetName+" on "+toString()+"; rethrowing...", e);
            throw Throwables.propagate(e);
        }
    }

    private void deployPhpApp(String url, String targetName){

        checkNotNull(url, "url");
        checkNotNull(targetName, "targetName");
        PhpWebAppDriver driver =   getDriver();
        String deployedAppName = driver.deploy(url, targetName);
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
            @EffectorParam(name="targetName") String targetName) {
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

        Set<String> deployedPhpApps = getDeployedPhpAppsAttribute();
        PhpWebAppDriver driver = getDriver();
        if (deployedPhpApps == null)
            deployedPhpApps = Sets.newLinkedHashSet();

        deployedPhpApps.remove( driver.getFilenameContextMapper().convertDeploymentTargetNameToContext(targetName) );
        setDeployedPhpAppsAttribute(deployedPhpApps);
    }

    @Override
    protected void doStop(){
        super.doStop();
        //zero our workrate derived workrates.
        //TODO might not be enough, as a ploicy may still be executing and have a record of historic vals;
        // should remove policies
        //also nor sure we want this; implies more generally a resposibility for sensor to announce things
        //disconnected
        putEnricherValuesToNullValue();
    }

    private void putEnricherValuesToNullValue(){
        setAttribute(REQUESTS_PER_SECOND_LAST, 0D);
        setAttribute(REQUESTS_PER_SECOND_IN_WINDOW, 0D);
    }

}
