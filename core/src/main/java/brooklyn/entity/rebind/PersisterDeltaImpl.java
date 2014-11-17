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
package brooklyn.entity.rebind;

import java.util.Collection;

import brooklyn.mementos.BrooklynMementoPersister.Delta;
import brooklyn.mementos.CatalogItemMemento;
import brooklyn.mementos.EnricherMemento;
import brooklyn.mementos.EntityMemento;
import brooklyn.mementos.FeedMemento;
import brooklyn.mementos.LocationMemento;
import brooklyn.mementos.Memento;
import brooklyn.mementos.PolicyMemento;

import com.google.common.collect.Sets;

public class PersisterDeltaImpl implements Delta {
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private final PersisterDeltaImpl delta = new PersisterDeltaImpl();

        public Builder locations(Collection<? extends LocationMemento> vals) {
            delta.locations.addAll(vals);
            return this;
        }
        public Builder entities(Collection<? extends EntityMemento> vals) {
            delta.entities.addAll(vals);
            return this;
        }
        public Builder policies(Collection<? extends PolicyMemento> vals) {
            delta.policies.addAll(vals);
            return this;
        }
        public Builder enrichers(Collection<? extends EnricherMemento> vals) {
            delta.enrichers.addAll(vals);
            return this;
        }
        public Builder feeds(Collection<? extends FeedMemento> vals) {
            delta.feeds.addAll(vals);
            return this;
        }
        public Builder catalogItems(Collection<? extends CatalogItemMemento> vals) {
            delta.catalogItems.addAll(vals);
            return this;
        }
        public Builder removedLocationIds(Collection<String> vals) {
            delta.removedLocationIds.addAll(vals);
            return this;
        }
        public Builder removedEntityIds(Collection<String> vals) {
            delta.removedEntityIds.addAll(vals);
            return this;
        }
        public Builder removedPolicyIds(Collection<String> vals) {
            delta.removedPolicyIds.addAll(vals);
            return this;
        }
        public Builder removedEnricherIds(Collection<String> vals) {
            delta.removedEnricherIds.addAll(vals);
            return this;
        }
        public Builder removedFeedIds(Collection<String> vals) {
            delta.removedFeedIds.addAll(vals);
            return this;
        }
        public Builder removedCatalogItemIds(Collection<String> vals) {
            delta.removedCatalogItemIds.addAll(vals);
            return this;
        }
        public Delta build() {
            return delta;
        }
    }
    
    Collection<LocationMemento> locations = Sets.newLinkedHashSet();
    Collection<EntityMemento> entities = Sets.newLinkedHashSet();
    Collection<PolicyMemento> policies = Sets.newLinkedHashSet();
    Collection<EnricherMemento> enrichers = Sets.newLinkedHashSet();
    Collection<FeedMemento> feeds = Sets.newLinkedHashSet();
    Collection<CatalogItemMemento> catalogItems = Sets.newLinkedHashSet();
    Collection<String> removedLocationIds = Sets.newLinkedHashSet();
    Collection<String> removedEntityIds = Sets.newLinkedHashSet();
    Collection<String> removedPolicyIds = Sets.newLinkedHashSet();
    Collection<String> removedEnricherIds = Sets.newLinkedHashSet();
    Collection <String> removedFeedIds = Sets.newLinkedHashSet();
    Collection<String> removedCatalogItemIds = Sets.newLinkedHashSet();

    @Override
    public Collection<LocationMemento> locations() {
        return locations;
    }

    @Override
    public Collection<EntityMemento> entities() {
        return entities;
    }

    @Override
    public Collection<PolicyMemento> policies() {
        return policies;
    }

    @Override
    public Collection<EnricherMemento> enrichers() {
        return enrichers;
    }
    
    @Override
    public Collection<FeedMemento> feeds() {
        return feeds;
    }

    @Override
    public Collection<CatalogItemMemento> catalogItems() {
        return catalogItems;
    }

    @Override
    public Collection<String> removedLocationIds() {
        return removedLocationIds;
    }

    @Override
    public Collection<String> removedEntityIds() {
        return removedEntityIds;
    }
    
    @Override
    public Collection<String> removedPolicyIds() {
        return removedPolicyIds;
    }
    
    @Override
    public Collection<String> removedEnricherIds() {
        return removedEnricherIds;
    }
    
    @Override
    public Collection<String> removedFeedIds() {
        return removedFeedIds;
    }

    @Override
    public Collection<String> removedCatalogItemIds() {
        return removedCatalogItemIds;
    }
    
    @Override
    public Collection<? extends Memento> getObjectsOfType(BrooklynObjectType type) {
        switch (type) {
        case ENTITY: return entities();
        case LOCATION: return locations();
        case POLICY: return policies();
        case ENRICHER: return enrichers();
        case FEED: return feeds();
        case CATALOG_ITEM: return catalogItems();
        case UNKNOWN: 
        default:
            throw new IllegalArgumentException(type+" not supported");
        }
    }
    
    @Override
    public Collection<String> getRemovedObjectsOfType(BrooklynObjectType type) {
        switch (type) {
        case ENTITY: return removedEntityIds();
        case LOCATION: return removedLocationIds();
        case POLICY: return removedPolicyIds();
        case ENRICHER: return removedEnricherIds();
        case FEED: return removedFeedIds();
        case CATALOG_ITEM: return removedCatalogItemIds();
        case UNKNOWN: 
        default:
            throw new IllegalArgumentException(type+" not supported");
        }
    }
}
