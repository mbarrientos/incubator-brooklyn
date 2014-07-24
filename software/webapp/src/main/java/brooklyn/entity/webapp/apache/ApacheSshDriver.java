package brooklyn.entity.webapp.apache;

import brooklyn.entity.webapp.JavaWebAppSshDriver;
import brooklyn.entity.webapp.PhpWebAppSshDriver;
import brooklyn.entity.webapp.WebAppService;
import brooklyn.entity.webapp.jboss.JBoss7ServerImpl;
import brooklyn.location.basic.SshMachineLocation;
import brooklyn.util.collections.MutableMap;
import brooklyn.util.ssh.BashCommands;
import brooklyn.util.stream.StreamGobbler;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;


public class ApacheSshDriver extends PhpWebAppSshDriver implements ApacheDriver {

   private static final Logger LOG = LoggerFactory.getLogger(ApacheSshDriver.class);

    public ApacheSshDriver(ApacheServerImpl entity, SshMachineLocation machine) {
        super(entity, machine);
    }

    @Override
    public ApacheServerImpl getEntity() {
        return (ApacheServerImpl) super.getEntity();
    }

    @Override
    protected Map<String, Integer> getPortMap() {
        return ImmutableMap.of("httpPort", getEntity().getHttpPort());
    }

    @Override
    protected String getDeploySubdir() {
        return "";
    }

    @Override
    public Integer getHttpPort(){
        return getEntity().getHttpPort();
    }

    @Override
    public String getRunDir(){
        return getEntity().getConfig(ApacheServer.DEPLOY_RUN_DIR);
    }

    @Override
    public void install(){
        super.install();
        if(!isApacheInstalled()){
            LOG.info("Apache is not installing, so install in {}", new Object[]{this});
            /*Maybe, we find a old apache configuration file which have to be removed to install the new
            * server without errors.*/
            removeOldConfigFile();
            installApacheServer();
        }
    }

    private void removeOldConfigFile(){
        String oldApacheConfigurationFilePath=getEntity().getConfigurationDir()+"/"+"apache2.conf";
        getMachine().execCommands("deleteOldApacheConfigFile", ImmutableList.of("rm -f"+oldApacheConfigurationFilePath));
    }

    private boolean isApacheInstalled(){
        boolean apacheIsInstalled=false;
        int result= getMachine().execCommands("apacheInstalled", ImmutableList.of("apache2 -v"));
        if (result==0)
            apacheIsInstalled=true;
        return apacheIsInstalled;
    }

    private int installApacheServer(){
        int result;
        LOG.warn("Installing Apache Server {}", new Object[]{getEntity()});
        List<String> commands= ImmutableList.<String>builder().add(BashCommands.
                installPackage(MutableMap.of("apt", "apache2"), null)).build();
        result=newScript(INSTALLING).body.append(commands).execute();
        if(result!=0)
            log.warn("Problem installing {} for {}: result {}", new Object[]{ entity, result});
        else
            log.info("Installed {} for {}: result {} commands {}\n", new Object[]{ entity, result}, commands);
        return result;
    }


    @Override
    public void customize(){
        startApacheIfIsNotRunning();
        newScript(CUSTOMIZING)
                .body.append(
                disableCurrentDeployRunDir(),
                removeAvailableSitesConfigurationFolder(),
                addDeployRunDirConfiguration(),
                enableAvailableDeploymentRunDir(),
                enableServerStatusServerModule(),
                configureHttpPort(),
                realoadApacheService()
        ).execute();

        LOG.info("deployInit initial applications from {}", new Object[]{this});
        //getEntity().deployInitialApplications();
    }

    private void startApacheIfIsNotRunning(){
        if(!isRunning())
            startApache();
    }

    private int startApache(){
        return  getMachine().execCommands("startApacheIfItIsNeeded", ImmutableList.of("service apache2 start"));
    }

    private String disableCurrentDeployRunDir(){

        String result = String.format(
                "for file in %s%s/*.conf\n" +
                        "do\n" +
                        "FILENAME=$(basename $file)\n" +
                        "exec a2dissite $FILENAME | true\n" +
                        "done\n" +
                        "%s\n",
                getEntity().getConfigurationDir(),
                getEntity().getAvailableSitesConfigurationFolder(),
                realoadApacheService());
        return result;
    }

    private String realoadApacheService(){
        return "set +e\n" +
                "invoke-rc.d apache2 reload\n";
    }

   private String removeAvailableSitesConfigurationFolder(){
       String result=String.format(
               "rm -rf %s%s/*\n",
               getEntity().getConfigurationDir(),
               getEntity().getAvailableSitesConfigurationFolder());
       return result;
   }

   private String addDeployRunDirConfiguration(){
        String result= String.format(
                "%s"+
                "cat > %s%s/BrooklynDeployRunDir.conf << \"EOF\"\n" +
                "<VirtualHost *:80>\n" +
                "ServerAdmin webmaster@localhost\n" +
                "DocumentRoot %s\n" +
                "ErrorLog ${APACHE_LOG_DIR}/error.log\n" +
                "CustomLog ${APACHE_LOG_DIR}/access.log combined\n" +
                "</VirtualHost>\n" +
                "EOF\n"+
                "%s\n",
                createFolderDeployRunDir(),
                getEntity().getConfigurationDir(),
                getEntity().getAvailableSitesConfigurationFolder(),
                getEntity().getDeployRunDir(),
                changePermissionsOfDeployRunDir());
       return result;
    }

    private String createFolderDeployRunDir(){
               return "mkdir -p "+getRunDir()+"\n";
   }

    private String changePermissionsOfDeployRunDir(){
        return String.format("chown -R %s:%s %s\n",getEntity().getDefaultGroup(), getEntity().getDefaultGroup(),getRunDir());
    }

    private String enableAvailableDeploymentRunDir(){
        String result = String.format(
                "for file in %s%s/*.conf\n" +
                        "do\n" +
                        "FILENAME=$(basename $file)\n" +
                        "exec a2ensite $FILENAME | true\n" +
                        "done\n" +
                        "%s\n",
                getEntity().getConfigurationDir(),
                getEntity().getAvailableSitesConfigurationFolder(),
                realoadApacheService());
        return result;
    }

    private String enableServerStatusServerModule(){
        String result;
        result= String.format(
                "cat <<EOT >> %s/apache2.conf\n" +
                "<Location /server-status>\n" +
                "\tSetHandler server-status\n" +
                "</Location>\n" +
                "ExtendedStatus On\n"+
                "EOT\n",
                getEntity().getConfigurationDir());
        return result;
    }

    //TODO the port configuration could be modified. So it is needed find Listen and change the port
    private String configureHttpPort(){
        String result;
        result= String.format(
                "cat <<EOT >> %s/apache2.conf\n" +
                        "Listen %s\n" +
                        "EOT\n",
                getEntity().getConfigurationDir(),
                getEntity().getHttpPort());
        return result;
    }

    //TODO
    @Override
    public void launch() {
        //Now this method does not return any value but It should run the app using
        //the startup files passed in the server of the application
    }

    @Override
    public boolean isRunning() {
        boolean isApacheRunning=false;
        int resultOfCommand = getMachine().execCommands("apacheIsRunning", ImmutableList.of("service apache2 status"));
        if (resultOfCommand==0)
            isApacheRunning=true;
        return  isApacheRunning;
    }


    //TODO add service apache2 stop to the command
    @Override
    public void stop() {
        newScript(MutableMap.of(USE_PID_FILE, true), STOPPING).environmentVariablesReset().execute();
    }

    @Override
    public void kill() {
        newScript(MutableMap.of(USE_PID_FILE, true), KILLING).execute();
    }

}
