package brooklyn.entity.webapp;

import brooklyn.entity.basic.AbstractSoftwareProcessSshDriver;
import brooklyn.entity.basic.Attributes;
import brooklyn.location.basic.SshMachineLocation;
import brooklyn.util.collections.MutableMap;
import brooklyn.util.os.Os;
import brooklyn.util.ssh.BashCommands;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by Jose on 07/07/2014.
 */
public abstract class PhpWebAppSshDriver extends AbstractSoftwareProcessSshDriver implements PhpWebAppDriver{


    public PhpWebAppSshDriver(PhpWebAppSoftwareProcessImpl entity, SshMachineLocation machine){
        super(entity, machine);
    }

    @Override
    public PhpWebAppSoftwareProcessImpl getEntity() {
        return (PhpWebAppSoftwareProcessImpl) super.getEntity();
    }

    protected boolean isProtocolEnabled(String protocol) {
        Set<String> protocols = getEnabledProtocols();
        for (String contender : protocols) {
            if (protocol.equalsIgnoreCase(contender)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Set<String> getEnabledProtocols() {
        return entity.getAttribute(PhpWebAppSoftwareProcess.ENABLED_PROTOCOLS);
    }

    @Override
    public Integer getHttpPort() {
        return entity.getAttribute(Attributes.HTTP_PORT);
    }

    @Override
    public Integer getHttpsPort() {
        return entity.getAttribute(Attributes.HTTPS_PORT);
    }

    @Override
    public HttpsSslConfig getHttpsSslConfig() {
        return entity.getAttribute(WebAppServiceConstants.HTTPS_SSL_CONFIG);
    }

    protected String getSslKeystoreUrl() {
        HttpsSslConfig ssl = getHttpsSslConfig();
        return (ssl == null) ? null : ssl.getKeystoreUrl();
    }

    protected String getSslKeystorePassword() {
        HttpsSslConfig ssl = getHttpsSslConfig();
        return (ssl == null) ? null : ssl.getKeystorePassword();
    }

    protected String getSslKeyAlias() {
        HttpsSslConfig ssl = getHttpsSslConfig();
        return (ssl == null) ? null : ssl.getKeyAlias();
    }

    //TODO refactor this method (abstract super class)
    protected String inferRootUrl() {
        if (isProtocolEnabled("https")) {
            Integer port = getHttpsPort();
            checkNotNull(port, "HTTPS_PORT sensors not set; is an acceptable port available?");
            return String.format("https://%s:%s/", getHostname(), port);
        } else if (isProtocolEnabled("http")) {
            Integer port = getHttpPort();
            checkNotNull(port, "HTTP_PORT sensors not set; is an acceptable port available?");
            return String.format("http://%s:%s/", getHostname(), port);
        } else {
            throw new IllegalStateException("HTTP and HTTPS protocols not enabled for "+entity+"; enabled protocols are "+getEnabledProtocols());
        }
    }

    @Override
    public void postLaunch() {
        String rootUrl = inferRootUrl();
        entity.setAttribute(WebAppService.ROOT_URL, rootUrl);
    }

    protected abstract String getDeploySubdir();

    protected String getDeployDir() {
        if (getDeploySubdir()==null)
            throw new IllegalStateException("no deployment directory available for "+this);
        //getRunDir is configured in SoftwareProcess
        return getRunDir() + "/" + getDeploySubdir();
    }

    @Override
    public String deploy(String url){

        log.info("{} deploying {} to {}:{}", new Object[]{entity, url, getHostname()});
        // create a backup
        //getMachine().execCommands("backing up old war", ImmutableList.of(String.format("mv -f %s %s.bak > /dev/null 2>&1", dest, dest))); //back up old file/directory
        String appName=getNameOfRepositoryGitFromHttpsUrl(url);
        String deployTargetDir=getDeployDir()+"/"+appName;
        int copyResult = copyUsingProtocol(url, deployTargetDir);
        log.debug("{} deployed {} to {}:{}: result {}", new Object[]{entity, url, getHostname(), deployTargetDir, copyResult});
        if (copyResult!=0) log.warn("Problem deploying {} to {}:{} for {}: result {}", new Object[]{url, getHostname(), deployTargetDir, entity, copyResult});
        return appName;
    }

    protected Map<String, Integer> getPortMap() {
        return ImmutableMap.of("httpPort", entity.getAttribute(WebAppService.HTTP_PORT));
    }

    @Override
    public Set<Integer> getPortsUsed() {
        return ImmutableSet.<Integer>builder()
                .addAll(super.getPortsUsed())
                .addAll(getPortMap().values())
                .build();
    }

    @Override
    public void install() {
        log.debug("Installing {}", getEntity());

        List<String> commands = ImmutableList.<String>builder()
                .add(BashCommands.installPackage(MutableMap.of("apt", "php"), null))
                .build();

        newScript(INSTALLING)
                .body.append(commands)
                .execute();
    }

    @Override
    public void stop() {

        newScript(STOPPING).execute();
    }

    @Override
    public void undeploy(String targetName) {
        String dest = getDeployDir() + "/" + targetName;
        log.info("{} undeploying {}:{}", new Object[]{entity, getHostname(), dest});
        int result = getMachine().execCommands("removing war on undeploy", ImmutableList.of(String.format("rm -f %s", dest)));
        log.debug("{} undeployed {}:{}: result {}", new Object[]{entity, getHostname(), dest, result});
    }



}



