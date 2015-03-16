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

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The CloudFoundry implementation of the {@link brooklyn.entity.java.VanillaJavaAppDriver}.
 */
public class VanillaJavaAppCloudFoundryDriver implements VanillaJavaAppDriver {

    private final CloudFoundryPaasLocation location;
    private final ResourceUtils resource;
    VanillaJavaAppImpl entity;

    public VanillaJavaAppCloudFoundryDriver(VanillaJavaAppImpl entity, CloudFoundryPaasLocation machine) {
        this.entity = checkNotNull(entity, "entity");
        this.location = checkNotNull(machine, "location");
        this.resource = ResourceUtils.create(entity);
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
        return false;
    }

    @Override
    public void rebind() {

    }

    @Override
    public void start() {

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
