package brooklyn.entity.webapp.apache;

import brooklyn.enricher.Enrichers;
import brooklyn.entity.Entity;
import brooklyn.entity.basic.SoftwareProcess;
import brooklyn.entity.webapp.PhpWebAppSoftwareProcessImpl;
import brooklyn.entity.webapp.jboss.JBoss7Driver;
import brooklyn.event.feed.function.FunctionFeed;
import brooklyn.event.feed.function.FunctionPollConfig;
import brooklyn.event.feed.http.HttpFeed;
import brooklyn.event.feed.http.HttpPollConfig;
import brooklyn.event.feed.http.HttpValueFunctions;
import brooklyn.policy.Enricher;
import com.google.common.base.Functions;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * Created by Jose on 10/07/2014.
 */
public class ApacheServerImpl extends PhpWebAppSoftwareProcessImpl implements ApacheServer{

    public static final Logger log = LoggerFactory.getLogger(ApacheServerImpl.class);

    private volatile FunctionFeed functionFeed;
    private volatile HttpFeed httpFeed;
    private Enricher serviceUpEnricher;

    public ApacheServerImpl(){
        super();
    }

    public ApacheServerImpl(Map flags){
        this(flags, null);
    }

    public ApacheServerImpl(Map flags, Entity parent) {
        super(flags, parent);
    }

    @Override
    public Class getDriverInterface() {
        return ApacheDriver.class;
    }

    @Override
    public ApacheDriver getDriver() {
        return (ApacheDriver) super.getDriver();
    }

    public String getDefaultGroup(){
        return getConfig(DEFAULT_GROUP);
    }

    public void setAppUser(String appName){
        setConfig(APP_NAME, appName);
    }

    public String getInstallDir(){return getConfig(ApacheServer.INSTALL_DIR);}

    public String getConfigurationDir(){return getConfig(CONFIGURATION_DIR);}

    public String  getDeployRunDir(){ return getConfig(DEPLOY_RUN_DIR);}

    public String  getAvailableSitesConfigurationFolder(){ return getConfig(AVAILABLE_SITES_CONFIGURATION_FOLDER);}

    @Override
    public int getHttpPort(){ return getAttribute(ApacheServer.HTTP_PORT);}

    @Override
    protected void connectSensors() {
        super.connectSensors();
        //TODO connect sensors and deletehe the comment
        //conectar todos los sensores
        //Disconect sensors
        //Connect of element

        functionFeed = FunctionFeed.builder()
                    .entity(this)
                    .poll(new FunctionPollConfig<Object, Boolean>(SERVICE_UP)
                            .period(500, TimeUnit.MILLISECONDS)
                            .callable(new Callable<Boolean>() {
                                public Boolean call() throws Exception {
                                    return getDriver().isRunning();
                                }
                            })
                            .onException(Functions.constant(Boolean.FALSE)))
                            .build();
    }
    //añadir conectores y desconectores de sensores



    @Override
    public String getShortName() {
        return "ApacheWebServerHttpd";
    }
}
