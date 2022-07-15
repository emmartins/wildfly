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

import java.io.Serializable;

/**
 * TODO javadocs
 * @author emmartins
 */
public interface ContextServiceTypesConfiguration extends Serializable  {

    boolean isCleared(String contextType);

    boolean isPropagated(String contextType);

    boolean isUnchanged(String contextType);

    ContextServiceTypesConfiguration DEFAULT = new ContextServiceTypesConfiguration() {

        private static final long serialVersionUID = 6522054354610550805L;

        @Override
        public boolean isCleared(String contextType) {
            return ContextServiceDefinition.TRANSACTION.equals(contextType);
        }

        @Override
        public boolean isPropagated(String contextType) {
            return !isCleared(contextType);
        }

        @Override
        public boolean isUnchanged(String contextType) {
            return false;
        }
    };
}
