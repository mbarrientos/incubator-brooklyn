package brooklyn.entity.webapp;

import brooklyn.config.ConfigKey;
import brooklyn.event.basic.BasicConfigKey;
import brooklyn.util.flags.SetFromFlag;

import java.util.List;
import java.util.Map;

/**
 * Created by Jose on 04/07/2014.
 *
 */
public interface PhpWebAppService extends WebAppService {

    @SetFromFlag("app_url")
    public static final ConfigKey<String> APP_URL = new BasicConfigKey<String>(
            String.class, "php.app.url ", "The path where the deploment artifact is stored (supporting file: and classpath: prefixes)");

    @SetFromFlag("app_git_repo_url")
    public static final ConfigKey<String> APP_GIT_REPO_URL = new BasicConfigKey<String>(
            String.class, "php.app.git.repo.url ", "The Git repository where the application source code is stored (gitRepo)");

    @SetFromFlag("app_name")
    public static final ConfigKey<String> APP_NAME = new BasicConfigKey(
            List.class, "php.app.name", "The name of the PHP application");

    @SetFromFlag("app_start_file")
    public static final ConfigKey<List<String>> APP_START_FILE = new BasicConfigKey(
            List.class, "php.app.start.file", "PHP application file to start e.g. main.php, or launch.php");

    @SetFromFlag("app_by_context")
    public static final ConfigKey<Map<String,String>> APP_BY_CONTEXT = new BasicConfigKey(
            Map.class, "php.app.by.context", "Map of context keys (path in user-facing URL, typically without slashes) to archives (e.g. zip by URL) to deploy, supporting file: and classpath: prefixes)","");


}
