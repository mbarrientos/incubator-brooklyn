package brooklyn.entity.webapp;

import brooklyn.config.ConfigKey;
import brooklyn.event.basic.BasicConfigKey;
import brooklyn.util.flags.SetFromFlag;
import java.util.List;


public interface PhpWebAppService extends WebAppService {

    @SetFromFlag("app_tarball_url")
    public static final ConfigKey<String> APP_TARBALL_URL = new BasicConfigKey<String>(
            String.class, "php.app.tarball.url ", "The path where the deploment artifact (tarball) is stored (supporting file: and classpath: prefixes)");

    @SetFromFlag("app_git_repo_url")
    public static final ConfigKey<String> APP_GIT_REPO_URL = new BasicConfigKey<String>(
            String.class, "php.app.git.repo.url ", "The Git repository where the application source code is stored (gitRepo)");

    @SetFromFlag("app_name")
    public static final ConfigKey<String> APP_NAME = new BasicConfigKey(
            List.class, "php.app.name", "The name of the PHP application");

    @SetFromFlag("app_start_file")
    public static final ConfigKey<List<String>> APP_START_FILE = new BasicConfigKey(
            List.class, "php.app.start.file", "PHP application file to start e.g. main.php, or launch.php");

}
