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
package brooklyn.management.internal;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.entity.Application;
import brooklyn.entity.Entity;
import brooklyn.entity.basic.EntityInternal;
import brooklyn.entity.basic.Lifecycle;
import brooklyn.internal.storage.BrooklynStorage;
import brooklyn.location.Location;
import brooklyn.location.basic.LocationConfigKeys;
import brooklyn.location.basic.LocationInternal;
import brooklyn.management.usage.ApplicationUsage;
import brooklyn.management.usage.LocationUsage;
import brooklyn.util.exceptions.Exceptions;
import brooklyn.util.flags.TypeCoercions;
import brooklyn.util.javalang.Reflections;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class LocalUsageManager implements UsageManager {

    // TODO Threading model needs revisited.
    // Synchronizes on updates to storage; but if two Brooklyn nodes were both writing to the same
    // ApplicationUsage or LocationUsage record there'd be a race. That currently won't happen
    // (at least for ApplicationUsage?) because the app is mastered in just one node at a time,
    // and because location events are just manage/unmanage which should be happening in just 
    // one place at a time for a given location.
    
    private static final Logger log = LoggerFactory.getLogger(LocalUsageManager.class);

    // Register a coercion from String->UsageListener, so that USAGE_LISTENERS defined in brooklyn.properties
    // will be instantiated, given their class names.
    static {
        TypeCoercions.registerAdapter(String.class, UsageListener.class, new Function<String, UsageListener>() {
            @Override public UsageListener apply(String input) {
                // TODO Want to use classLoader = mgmt.getCatalog().getRootClassLoader();
                ClassLoader classLoader = LocalUsageManager.class.getClassLoader();
                Optional<Object> result = Reflections.invokeConstructorWithArgs(classLoader, input);
                if (result.isPresent()) {
                    return (UsageListener) result.get();
                } else {
                    throw new IllegalStateException("Failed to create UsageListener from class name '"+input+"' using no-arg constructor");
                }
            }
        });
    }
    
    @VisibleForTesting
    public static final String APPLICATION_USAGE_KEY = "usage-application";
    
    @VisibleForTesting
    public static final String LOCATION_USAGE_KEY = "usage-location";

    private final LocalManagementContext managementContext;
    
    private final Object mutex = new Object();

    private final List<UsageListener> listeners = Lists.newCopyOnWriteArrayList();
    
    private ExecutorService listenerExecutor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
            .setNameFormat("brooklyn-usagemanager-listener-%d")
            .build());

    public LocalUsageManager(LocalManagementContext managementContext) {
        this.managementContext = checkNotNull(managementContext, "managementContext");
        
        Collection<UsageListener> listeners = managementContext.getBrooklynProperties().getConfig(UsageManager.USAGE_LISTENERS);
        if (listeners != null) {
            for (UsageListener listener : listeners) {
                addUsageListener(listener);
            }
        }
    }

    @Override
    public void recordApplicationEvent(final Application app, final Lifecycle state) {
        log.debug("Storing application lifecycle usage event: application {} in state {}", new Object[] {app, state});
        ConcurrentMap<String, ApplicationUsage> eventMap = managementContext.getStorage().getMap(APPLICATION_USAGE_KEY);
        synchronized (mutex) {
            ApplicationUsage usage = eventMap.get(app.getId());
            if (usage == null) {
                usage = new ApplicationUsage(app.getId(), app.getDisplayName(), app.getEntityType().getName(), ((EntityInternal)app).toMetadataRecord());
            }
            final ApplicationUsage.ApplicationEvent event = new ApplicationUsage.ApplicationEvent(state);
            usage.addEvent(event);        
            eventMap.put(app.getId(), usage);
            
            for (final UsageListener listener : listeners) {
                listenerExecutor.execute(new Runnable() {
                    public void run() {
                        try {
                            listener.onApplicationEvent(app.getId(), app.getDisplayName(), app.getEntityType().getName(), ((EntityInternal)app).toMetadataRecord(), event);
                        } catch (Exception e) {
                            log.error("Problem notifying listener "+listener+" of applicationEvent("+app+", "+state+")", e);
                            Exceptions.propagateIfFatal(e);
                        }
                    }});
            }
        }
    }
    
    /**
     * Adds this location event to the usage record for the given location (creating the usage 
     * record if one does not already exist).
     */
    @Override
    public void recordLocationEvent(final Location loc, final Lifecycle state) {
        // TODO This approach (i.e. recording events on manage/unmanage would not work for
        // locations that are reused. For example, in a FixedListMachineProvisioningLocation
        // the ssh machine location is returned to the pool and handed back out again.
        // But maybe the solution there is to hand out different instances so that one user
        // can't change the config of the SshMachineLocation to subsequently affect the next 
        // user.
        //
        // TODO Should perhaps extract the location storage methods into their own class,
        // but no strong enough feelings yet...
        
        checkNotNull(loc, "location");
        checkNotNull(state, "state of location %s", loc);
        if (loc.getId() == null) {
            log.error("Ignoring location lifecycle usage event for {} (state {}), because location has no id", loc, state);
            return;
        }
        if (managementContext.getStorage() == null) {
            log.warn("Cannot store location lifecycle usage event for {} (state {}), because storage not available", loc, state);
            return;
        }
        
        Object callerContext = loc.getConfig(LocationConfigKeys.CALLER_CONTEXT);
        
        if (callerContext != null && callerContext instanceof Entity) {
            log.debug("Storing location lifecycle usage event: location {} in state {}; caller context {}", new Object[] {loc, state, callerContext});
            
            Entity caller = (Entity) callerContext;
            String entityTypeName = caller.getEntityType().getName();
            String appId = caller.getApplicationId();
            final LocationUsage.LocationEvent event = new LocationUsage.LocationEvent(state, caller.getId(), entityTypeName, appId);
            
            ConcurrentMap<String, LocationUsage> usageMap = managementContext.getStorage().<String, LocationUsage>getMap(LOCATION_USAGE_KEY);
            synchronized (mutex) {
                LocationUsage usage = usageMap.get(loc.getId());
                if (usage == null) {
                    usage = new LocationUsage(loc.getId(), ((LocationInternal)loc).toMetadataRecord());
                }
                usage.addEvent(event);
                usageMap.put(loc.getId(), usage);
                
                for (final UsageListener listener : listeners) {
                    listenerExecutor.execute(new Runnable() {
                        public void run() {
                            try {
                                listener.onLocationEvent(loc.getId(), ((LocationInternal)loc).toMetadataRecord(), event);
                            } catch (Exception e) {
                                log.error("Problem notifying listener "+listener+" of locationEvent("+loc+", "+state+")", e);
                                Exceptions.propagateIfFatal(e);
                            }
                        }});
                }
            }
        } else {
            // normal for high-level locations
            log.trace("Not recording location lifecycle usage event for {} in state {}, because no caller context", new Object[] {loc, state});
        }
    }

    /**
     * Returns the usage info for the location with the given id, or null if unknown.
     */
    @Override
    public LocationUsage getLocationUsage(String locationId) {
        BrooklynStorage storage = managementContext.getStorage();

        Map<String, LocationUsage> usageMap = storage.getMap(LOCATION_USAGE_KEY);
        return usageMap.get(locationId);
    }
    
    /**
     * Returns the usage info that matches the given predicate.
     * For example, could be used to find locations used within a given time period.
     */
    @Override
    public Set<LocationUsage> getLocationUsage(Predicate<? super LocationUsage> filter) {
        // TODO could do more efficient indexing, to more easily find locations in use during a given period.
        // But this is good enough for first-pass.

        Map<String, LocationUsage> usageMap = managementContext.getStorage().getMap(LOCATION_USAGE_KEY);
        Set<LocationUsage> result = Sets.newLinkedHashSet();
        
        for (LocationUsage usage : usageMap.values()) {
            if (filter.apply(usage)) {
                result.add(usage);
            }
        }
        return result;
    }
    
    /**
     * Returns the usage info for the location with the given id, or null if unknown.
     */
    @Override
    public ApplicationUsage getApplicationUsage(String appId) {
        BrooklynStorage storage = managementContext.getStorage();

        Map<String, ApplicationUsage> usageMap = storage.getMap(APPLICATION_USAGE_KEY);
        return usageMap.get(appId);
    }
    
    /**
     * Returns the usage info that matches the given predicate.
     * For example, could be used to find applications used within a given time period.
     */
    @Override
    public Set<ApplicationUsage> getApplicationUsage(Predicate<? super ApplicationUsage> filter) {
        // TODO could do more efficient indexing, to more easily find locations in use during a given period.
        // But this is good enough for first-pass.

        Map<String, ApplicationUsage> usageMap = managementContext.getStorage().getMap(APPLICATION_USAGE_KEY);
        Set<ApplicationUsage> result = Sets.newLinkedHashSet();
        
        for (ApplicationUsage usage : usageMap.values()) {
            if (filter.apply(usage)) {
                result.add(usage);
            }
        }
        return result;
    }

    @Override
    public void addUsageListener(UsageListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeUsageListener(UsageListener listener) {
        listeners.remove(listener);
    }
}
