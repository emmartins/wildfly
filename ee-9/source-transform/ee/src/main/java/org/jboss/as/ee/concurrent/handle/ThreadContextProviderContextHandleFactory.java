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
package org.jboss.as.ee.concurrent.handle;

import jakarta.enterprise.concurrent.ContextService;
import jakarta.enterprise.concurrent.spi.ThreadContextProvider;
import jakarta.enterprise.concurrent.spi.ThreadContextRestorer;
import jakarta.enterprise.concurrent.spi.ThreadContextSnapshot;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;

/**
 * The context handle factory responsible for saving and setting the context for a deployement's ThreadContextProvider.
 *
 * @author Eduardo Martins
 */
public class ThreadContextProviderContextHandleFactory implements EE10ContextHandleFactory {

    private static final int BASE_PRIORITY = 1000;

    private final ThreadContextProvider threadContextProvider;
    private final int priority;

    public ThreadContextProviderContextHandleFactory(ThreadContextProvider threadContextProvider, int priority) {
        this.threadContextProvider = threadContextProvider;
        this.priority = BASE_PRIORITY + priority;
    }

    @Override
    public String getContextType() {
        return threadContextProvider.getThreadContextType();
    }

    @Override
    public SetupContextHandle clearedContext(ContextService contextService, Map<String, String> contextObjectProperties) {
        return new ContextHandle(threadContextProvider, contextObjectProperties, true);
    }

    @Override
    public SetupContextHandle propagatedContext(ContextService contextService, Map<String, String> contextObjectProperties) {
        return new ContextHandle(threadContextProvider, contextObjectProperties, false);
    }

    @Override
    public String getName() {
        return getContextType();
    }

    @Override
    public int getChainPriority() {
        return priority;
    }

    @Override
    public void writeSetupContextHandle(SetupContextHandle contextHandle, ObjectOutputStream out) throws IOException {
        out.writeObject(contextHandle);
    }

    @Override
    public SetupContextHandle readSetupContextHandle(ObjectInputStream in) throws IOException, ClassNotFoundException {
        return (ContextHandle) in.readObject();
    }

    private static class ContextHandle implements SetupContextHandle {

        private static final long serialVersionUID = 842115413317072688L;
        private final String factoryName;
        private final ThreadContextSnapshot savedContextSnapshot;

        private ContextHandle(ThreadContextProvider threadContextProvider, Map<String, String> contextObjectProperties, boolean cleared) {
            this.factoryName = threadContextProvider.getThreadContextType();
            this.savedContextSnapshot = cleared ? threadContextProvider.clearedContext(contextObjectProperties) : threadContextProvider.currentContext(contextObjectProperties);
        }

        @Override
        public String getFactoryName() {
            return factoryName;
        }

        @Override
        public ResetContextHandle setup() throws IllegalStateException {
            final ThreadContextRestorer threadContextRestorer = savedContextSnapshot.begin();
            return new ResetContextHandle() {
                @Override
                public void reset() {
                    threadContextRestorer.endContext();
                }
                @Override
                public String getFactoryName() {
                    return getFactoryName();
                }
            };
        }
    }
}
