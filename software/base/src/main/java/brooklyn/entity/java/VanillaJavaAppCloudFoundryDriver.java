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
package brooklyn.entity.java;


import brooklyn.location.paas.cloudfoundry.CloudFoundryPaasLocation;
import brooklyn.util.ResourceUtils;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.Staging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The CloudFoundry implementation of the {@link brooklyn.entity.java.VanillaJavaAppDriver}.
 */
public class VanillaJavaAppCloudFoundryDriver implements VanillaJavaAppDriver {

    private static final Logger log = LoggerFactory.getLogger(VanillaJavaAppCloudFoundryDriver.class);

    private static final int DEFAULT_MEMORY = 512; // MB

    private final CloudFoundryPaasLocation location;
    private final ResourceUtils resource;
    VanillaJavaAppImpl entity;
    String applicationPath;
    String applicationName;
    CloudFoundryClient client;

    private boolean isRunning = false;

    public VanillaJavaAppCloudFoundryDriver(VanillaJavaAppImpl entity, CloudFoundryPaasLocation machine) {
        this.entity = checkNotNull(entity, "entity");
        this.location = checkNotNull(machine, "location");
        this.resource = ResourceUtils.create(entity);

        init();
    }

    private void init() {
        initApplicationParameters();
        client = location.getCloudFoundryClient();
        checkNotNull(client);
    }

    @SuppressWarnings("unchecked")
    private void initApplicationParameters() {
        List<String> list = ((List<String>) entity.getConfig(VanillaJavaApp.ARGS));
        if ((list != null) && (list.size() > 1)) {
            applicationPath = list.get(0);
            applicationName = list.get(1);
        } else {
            log.warn("Malformed application parameters in {}, it is necessary specify at least " +
                    "application name and application uri", new Object[]{this});
        }
    }

    @Override
    public boolean isJmxEnabled() {
        return false;
    }

    @Override
    public VanillaJavaAppImpl getEntity() {
        return entity;
    }

    @Override
    public CloudFoundryPaasLocation getLocation() {
        return location;
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public void rebind() {

    }

    @Override
    public void start() {
        Staging stagin = new Staging();
        List<String> applicationUris = new LinkedList<String>();
        applicationUris.add(applicationPath);
        client.createApplication(applicationName, stagin, DEFAULT_MEMORY, applicationUris, null);
        checkApplicationDeployed();
    }

    private void checkApplicationDeployed() {
        CloudApplication app = client.getApplication(applicationName);
        if (app != null) {
            changeRunningStatus(true);
        } else {
            changeRunningStatus(false);
        }
    }

    private void changeRunningStatus(boolean isRunning) {
        this.isRunning = isRunning;
    }

    @Override
    public void restart() {

    }

    @Override
    public void stop() {

    }

    @Override
    public void kill() {

    }
}
