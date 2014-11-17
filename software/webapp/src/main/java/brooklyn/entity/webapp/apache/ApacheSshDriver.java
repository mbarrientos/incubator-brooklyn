/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package brooklyn.entity.webapp.apache;


import brooklyn.entity.webapp.PhpWebAppSshDriver;
import brooklyn.location.basic.SshMachineLocation;
import brooklyn.util.collections.MutableMap;
import brooklyn.util.ssh.BashCommands;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;


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

    private boolean isApacheInstalled(){
        boolean apacheIsInstalled=false;
        int result= getMachine().execCommands("apacheInstalled", ImmutableList.of("apache2 -v"));
        if (result==0)
            apacheIsInstalled=true;
        return apacheIsInstalled;
    }

    private void removeOldConfigFile(){
        String oldApacheConfigurationFilePath=getEntity().getConfigurationDir()+"/"+"apache2.conf";
        getMachine().execCommands("deleteOldApacheConfigFile", ImmutableList.of("rm -f"+oldApacheConfigurationFilePath));
    }

    private int installApacheServer(){
        int result;
        LOG.info("Installing Apache Server {}", new Object[]{getEntity()});
        List<String> commands= ImmutableList.<String>builder().add(BashCommands.
                installPackage(MutableMap.of("apt", "apache2"), null)).build();
        result=newScript(INSTALLING).body.append(commands).execute();
        if(result!=0)
            log.warn("Problem installing {} for {}: result {}", new Object[]{ entity, result});
        else
            log.info("Installed {} for {} commands {}\n", new Object[]{ result, entity, commands});
        return result;
    }

    @Override
    public void customize(){
        startApacheIfIsNotRunning();
        newScript(CUSTOMIZING)
                .body.append(
                disableCurrentDeployRunDir(),
                removeAvailableSitesConfigurationFolder(),
                createDeployRunDirConfigurationFile(),
                enableAvailableDeploymentRunDir(),
                enableServerStatusServerModule(),
                configureHttpPort(),
                realoadApacheService()
        ).execute();
        LOG.info("deployInit initial applications from {}", new Object[]{this});
        getEntity().deployInitialApplications();
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
                        "\n",
                getEntity().getConfigurationDir(),
                getEntity().getAvailableSitesConfigurationFolder());
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

   private String  addFolderToAvailablesSitesConfigurationFile(String targetName){
       String result= String.format(
               "cat <<EOT >> %s%s/%s\n" +
               "#"+targetName+"\n"+
               "<VirtualHost *:%s>\n" +
               "\tServerAdmin webmaster@localhost\n" +
               "\tDocumentRoot %s/"+targetName+"\n"+
               "\tErrorLog ${APACHE_LOG_DIR}/error-"+targetName+".log\n" +
               "\tCustomLog ${APACHE_LOG_DIR}/access-"+targetName+".log combined\n" +
               "</VirtualHost>\n" +
               "EOT",
               getEntity().getConfigurationDir(),
               getEntity().getAvailableSitesConfigurationFolder(),
               getEntity().getAvailablesSitesConfigurationFile(),
               getEntity().getHttpPort(),
               getEntity().getDeployRunDir(),
               changePermissionsOfFolder(getEntity().getDeployRunDir()+"/"+targetName));
       return result;
   }

   private String createDeployRunDirConfigurationFile(){
        String result= String.format(
                "%s\n"+
                "cat > %s%s/%s << \"EOF\"\n" +
                "#[Brooklyn] Configuration File\n" +
                        "#app_id\n" +
                        "#<VirtualHost * :${PORT}>\n" +
                        "\t#ServerAdmin webmaster@localhost\n" +
                        "\t#DocumentRoot ${DEPLOY_RUN_DIR}/app_id\n"+
                        "\t#ErrorLog ${APACHE_LOG_DIR} /error-app_id.log\n"+
                        "\t#ErrorLog ${APACHE_LOG_DIR} /access-app_id.log combined\n"+
                        "\n\n\t#SSL_CONFIGURATION\n" +
                        "\t# SSL SETUP\n" +
                        "\t# ServerName www.awesomesite.com/app_id\n" +
                        "\t# SSLEngine on\n" +
                        "\t#DEBIAN \n" +
                        "\t# SSLCertificateFile /etc/ssl/certs/cert.pem\n" +
                        "        # SSLCertificateKeyFile /etc/ssl/private/cert.key\n" +
                        "\n" +
                        "\t# REDHAT \n" +
                        "\t# SSLCertificateFile  /etc/pki/tls/certs/cert.pem\n" +
                        "\t# SSLCertificateKeyFile /etc/pki/tls/private/cert.key\n" +
                        "#</VirtualHost>\n\n\n" +
                        "<VirtualHost *:%s>\n" +
                        "\tDocumentRoot /var/www/\n" +
                        "</VirtualHost>\n\n" +
                "EOF",
                createFolderDeployRunDir(),
                getEntity().getConfigurationDir(),
                getEntity().getAvailableSitesConfigurationFolder(),
                getEntity().getAvailablesSitesConfigurationFile(),
                getEntity().getHttpPort());
       return result;
    }

    private String createFolderDeployRunDir(){
               return "mkdir -p "+getRunDir()+"\n";
   }

    private String changePermissionsOfFolder(String folder){
        return String.format("chown -R %s:%s %s\n",getEntity().getDefaultGroup(), getEntity().getDefaultGroup(),folder);
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
                "\tOrder deny,allow\n" +
                "\tDeny from all\n" +
                "\tAllow from localhost\n" +
                "\tAllow from %s\n" +
                "</Location>\n" +
                "ExtendedStatus On\n"+
                "EOT\n",
                getEntity().getConfigurationDir(),
                getBrooklynIPv4());
        return result;
    }

    //TODO the port configuration could be modified. So it is needed find Listen and change the port
    private String configureHttpPort(){
        String result;
        result= String.format(
                "sed -i 's@Listen[ 0-9]\\+@Listen %s@g' %s/ports.conf",
                getEntity().getHttpPort(),
                getEntity().getConfigurationDir()
        );
        return result;
    }

    //TODO
    @Override
    public void launch() {
        //Now this method does not return any value but It should run the app using
        //the startup files passed in the server of the application
    }

    @Override
    public String deployGitResource(String url, String targetName){
        super.deployGitResource(url, targetName);
        newScript(CUSTOMIZING)
                .body.append(
                addFolderToAvailablesSitesConfigurationFile(targetName),
                realoadApacheService(),
                enableAvailableDeploymentRunDir()).execute();

        return targetName;
    }

    @Override
    public String deployTarballResource(String url, String targetName){
        super.deployTarballResource(url, targetName);
        newScript(CUSTOMIZING)
                .body.append(
                addFolderToAvailablesSitesConfigurationFile(targetName),
                realoadApacheService(),
                enableAvailableDeploymentRunDir()).execute();

        return targetName;
    }

    @Override
    public boolean isRunning() {
        boolean isApacheRunning=false;
        //int resultOfCommand = getMachine().execCommands("apacheIsRunning", ImmutableList.of("service apache2 status"));
        String command="service apache2 status";
        int resultOfCommand=newScript(STOPPING)
                .body.append(command).execute();
        if (resultOfCommand==0)
            isApacheRunning=true;
        return  isApacheRunning;
    }


    //TODO merge with the stopApacheMethod in any way
    @Override
    public void stop() {

        String command="service apache2 stop";
        newScript(STOPPING)
                .body.append(command).execute();

    }

    @Override
    public void kill() {
        stop();
    }

    private String getBrooklynIPv4() {

            String ip = "127.0.0.1";
            try {
                Enumeration<NetworkInterface> n = NetworkInterface.getNetworkInterfaces();
                for (; n.hasMoreElements(); ) {
                    NetworkInterface e = n.nextElement();
                    System.out.println("Interface: " + e.getName());
                    Enumeration<InetAddress> a = e.getInetAddresses();
                    for (; a.hasMoreElements(); ) {
                        InetAddress addr = a.nextElement();
                        if ((addr instanceof Inet4Address)
                                && (!addr.getHostAddress().equals("127.0.0.1"))) {
                            return addr.getHostAddress();
                        }
                    }
                }

            } catch (Exception e) {
                log.warn("Unreachable IP - return {} in {} ", new Object[]{ip, this});
            }
            return ip;
        }
    }
