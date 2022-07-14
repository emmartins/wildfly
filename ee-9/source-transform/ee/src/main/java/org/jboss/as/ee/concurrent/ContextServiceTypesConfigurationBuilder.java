/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.ee.concurrent;

import jakarta.enterprise.concurrent.ContextServiceDefinition;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * TODO javadocs
 * @author emmartins
 */
public class ContextServiceTypesConfigurationBuilder {

    private Set<String> cleared;
    private Set<String> propagated;
    private Set<String> unchanged;

    public ContextServiceTypesConfigurationBuilder setCleared(Set<String> cleared) {
        this.cleared = cleared;
        return this;
    }

    public ContextServiceTypesConfigurationBuilder setCleared(String[] cleared) {
        if (cleared == null || cleared.length == 0) {
            this.cleared = null;
        } else {
            this.cleared = new HashSet<>();
            Collections.addAll(this.cleared, cleared);
        }
        return this;
    }

    public ContextServiceTypesConfigurationBuilder setPropagated(Set<String> propagated) {
        this.propagated = propagated;
        return this;
    }

    public ContextServiceTypesConfigurationBuilder setPropagated(String[] propagated) {
        if (propagated == null || propagated.length == 0) {
            this.propagated = null;
        } else {
            this.propagated = new HashSet<>();
            Collections.addAll(this.propagated, propagated);
        }
        return this;
    }

    public ContextServiceTypesConfigurationBuilder setUnchanged(Set<String> unchanged) {
        this.unchanged = unchanged;
        return this;
    }

    public ContextServiceTypesConfigurationBuilder setUnchanged(String[] unchanged) {
        if (unchanged == null || unchanged.length == 0) {
            this.unchanged = null;
        } else {
            this.unchanged = new HashSet<>();
            Collections.addAll(this.unchanged, unchanged);
        }
        return this;
    }

    public ContextServiceTypesConfiguration build() throws IllegalStateException {
        int remainingCount = 0;
        if (cleared != null && cleared.contains(ContextServiceDefinition.ALL_REMAINING)) {
            remainingCount++;
        }
        if (propagated == null || propagated.isEmpty() || propagated.contains(ContextServiceDefinition.ALL_REMAINING)) {
            remainingCount++;
        }
        if (unchanged != null && unchanged.contains(ContextServiceDefinition.ALL_REMAINING)) {
            remainingCount++;
        }
        if (remainingCount > 1) {
            // TODO add to logger
            throw new IllegalStateException("Multiple usage of ContextServiceDefinition.ALL_REMAINING");
        }
        if ((cleared == null || cleared.isEmpty()) && (propagated == null || propagated.isEmpty()) && (unchanged == null || unchanged.isEmpty())) {
            return ContextServiceTypesConfiguration.DEFAULT;
        } else {
            return new ContextServiceTypesConfigurationImpl(cleared, propagated, unchanged);
        }
    }
}
