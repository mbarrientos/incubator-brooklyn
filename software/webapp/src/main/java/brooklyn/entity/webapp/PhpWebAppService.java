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
