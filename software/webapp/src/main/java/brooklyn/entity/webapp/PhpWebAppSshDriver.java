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

    @Override
    public Set<String> getEnabledProtocols() {
        return entity.getAttribute(PhpWebAppSoftwareProcess.ENABLED_PROTOCOLS);
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

    //TODO refactor. Duplicate code

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
    public void deploy(File file) {
        deploy(file, null);
    }

    @Override
    public void deploy(File f, String targetName) {
        if (targetName == null) {
            targetName = f.getName();
        }
        deploy(f.toURI().toASCIIString(), targetName);
    }


    /**
     * Deploys a URL as a webapp at the appserver.
     *
     * Returns a token which can be used as an argument to undeploy,
     * typically the web context with leading slash where the app can be reached (just "/" for ROOT)
     *
     */
//    @Override
//    we need modified the methods to generate the canonicalTargetName, because by default they are defined to
//    work with Java applications (war, ear and so on)
//    public String deploy(String url, String targetName) {
//        String canonicalTargetName = getFilenameContextMapper().convertDeploymentTargetNameToFilename(targetName);
//        String dest = getDeployDir() + "/" + canonicalTargetName;
//        log.info("{} deploying {} to {}:{}", new Object[]{entity, url, getHostname(), dest});
//        // create a backup
//        getMachine().execCommands("backing up old war", ImmutableList.of(String.format("mv -f %s %s.bak > /dev/null 2>&1", dest, dest))); //back up old file/directory
//        int result = copyResource(url, dest);
//        log.debug("{} deployed {} to {}:{}: result {}", new Object[]{entity, url, getHostname(), dest, result});
//        if (result!=0) log.warn("Problem deploying {} to {}:{} for {}: result {}", new Object[]{url, getHostname(), dest, entity, result});
//        return getFilenameContextMapper().convertDeploymentTargetNameToContext(canonicalTargetName);
//    }


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
                .add(BashCommands.installPackage(MutableMap.of("yum", "git php"), null))
                .build();

        newScript(INSTALLING)
                .body.append(commands)
                .execute();
    }

    @Override
    public void customize() {
        log.debug("Customising {}", getEntity());

//        String appUser = getEntity().getConfig(PhpWebAppSoftwareProcess.APP_USER);
//        String appName = getEntity().getConfig(PhpWebAppService.APP_NAME);
//
//        //The application could be stored in any place.
//        List<String> commands = ImmutableList.<String>builder()
//                .add(String.format("git clone %s %s", getEntity().getConfig(PhpWebAppService.APP_GIT_REPOSITORY_URL), appName))
//                .add(BashCommands.sudo(String.format("chown -R %1$s:%1$s %2$s", appUser, appName)))
//                .build();
//
//        newScript(CUSTOMIZING)
//                .body.append(commands)
//                .execute();
    }

    @Override
    public void launch() {
        log.debug("Launching {}", getEntity());

        String appUser = getEntity().getConfig(PhpWebAppSoftwareProcess.APP_USER);
        String appName = getEntity().getConfig(PhpWebAppService.APP_NAME);

        List<String> commands = ImmutableList.<String>builder()
                .add(String.format("cd %s", Os.mergePathsUnix(getRunDir(), appName)))
                .add(BashCommands.sudoAsUser(appUser, "nohup node " + getEntity().getConfig(PhpWebAppService.APP_START_FILE) + " &"))
                .build();

        newScript(LAUNCHING)
                .body.append(commands)
                .execute();
    }

    @Override
    public boolean isRunning() {
        return newScript(CHECK_RUNNING).execute() == 0;
    }

    @Override
    public void stop() {
        newScript(STOPPING).execute();
    }

    @Override
    public Map<String, String> getShellEnvironment() {
        return MutableMap.<String, String>builder().putAll(super.getShellEnvironment())
                .put("PORT", Integer.toString(getHttpPort()))
                .build();
    }

}



