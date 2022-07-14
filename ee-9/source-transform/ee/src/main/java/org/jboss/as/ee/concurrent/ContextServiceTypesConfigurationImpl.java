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

import java.util.Objects;
import java.util.Set;

/**
 * TODO javadocs
 * @author emmartins
 */
class ContextServiceTypesConfigurationImpl implements ContextServiceTypesConfiguration {

    private static final long serialVersionUID = -8818025042707301480L;

    private final Set<String> cleared;
    private final Set<String> propagated;
    private final Set<String> unchanged;

    ContextServiceTypesConfigurationImpl(Set<String> cleared, Set<String> propagated, Set<String> unchanged) {
        this.cleared = cleared;
        this.propagated = propagated;
        this.unchanged = unchanged;
    }

    @Override
    public boolean isCleared(String contextType) {
        Objects.requireNonNull(contextType);
        if (cleared == null || cleared.isEmpty()) {
            return ContextServiceDefinition.TRANSACTION.equals(contextType);
        }
        if (cleared.contains(contextType)) {
            return true;
        }
        if (cleared.contains(ContextServiceDefinition.ALL_REMAINING)) {
            return typeNotIn(contextType, propagated) && typeNotIn(contextType, unchanged);
        }
        return false;
    }

    @Override
    public boolean isPropagated(String contextType) {
        Objects.requireNonNull(contextType);
        boolean allRemaining = false;
        if (propagated == null || propagated.isEmpty()) {
            allRemaining = true;
        } else {
            if (propagated.contains(contextType)) {
                return true;
            } else if (propagated.contains(ContextServiceDefinition.ALL_REMAINING)) {
                allRemaining = true;
            }
        }
        if (allRemaining) {
            return typeNotIn(contextType, cleared) && typeNotIn(contextType, unchanged);
        }
        return false;
    }

    @Override
    public boolean isUnchanged(String contextType) {
        Objects.requireNonNull(contextType);
        if (unchanged == null || unchanged.isEmpty()) {
            return false;
        }
        if (unchanged.contains(contextType)) {
            return true;
        }
        if (unchanged.contains(ContextServiceDefinition.ALL_REMAINING)) {
            return typeNotIn(contextType, cleared) && typeNotIn(contextType, propagated);
        }
        return false;
    }

    private boolean typeNotIn(String contextType, Set<String> contextTypes) {
        return contextTypes == null || !contextTypes.contains(contextType);
    }
}
