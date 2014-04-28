/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.naming.deployment;

import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.naming.InitialContext;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.service.BinderServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

import javax.naming.CompositeName;
import javax.naming.InvalidNameException;
import javax.naming.Name;

/**
 * @author John Bailey
 * @author Eduardo Martins
 */
public class ContextNames {

    /**
     * Parent ServiceName for all naming services.
     */
    public static final ServiceName NAMING = ServiceName.JBOSS.append("naming");

    /**
     * ServiceName for java: namespace
     */
    public static final ServiceName JAVA_CONTEXT_SERVICE_NAME = NAMING.append("context", "java");

    /**
     * Parent ServiceName for java:comp namespace
     */
    public static final ServiceName COMPONENT_CONTEXT_SERVICE_NAME = JAVA_CONTEXT_SERVICE_NAME.append("comp");

    /**
     * ServiceName for java:jboss namespace
     */
    public static final ServiceName JBOSS_CONTEXT_SERVICE_NAME = JAVA_CONTEXT_SERVICE_NAME.append("jboss");

    /**
     * ServiceName for java:global namespace
     */
    public static final ServiceName GLOBAL_CONTEXT_SERVICE_NAME = JAVA_CONTEXT_SERVICE_NAME.append("global");

    /**
     * Parent ServiceName for java:app namespace
     */
    public static final ServiceName APPLICATION_CONTEXT_SERVICE_NAME = JAVA_CONTEXT_SERVICE_NAME.append("app");

    /**
     * Parent ServiceName for java:module namespace
     */
    public static final ServiceName MODULE_CONTEXT_SERVICE_NAME = JAVA_CONTEXT_SERVICE_NAME.append("module");

    /**
     * ServiceName for java:jboss/exported namespace
     */
    public static final ServiceName EXPORTED_CONTEXT_SERVICE_NAME = JBOSS_CONTEXT_SERVICE_NAME.append("exported");

    /**
     * Parent ServiceName for shared namespaces
     */
    public static final ServiceName SHARED_CONTEXT_SERVICE_NAME = JAVA_CONTEXT_SERVICE_NAME.append("shared");

    /**
     * ServiceName for shared java:comp namespace
     */
    public static final ServiceName SHARED_COMP_CONTEXT_SERVICE_NAME = SHARED_CONTEXT_SERVICE_NAME.append("comp");

    /**
     * ServiceName for shared java:module namespace
     */
    public static final ServiceName SHARED_MODULE_CONTEXT_SERVICE_NAME = SHARED_CONTEXT_SERVICE_NAME.append("module");

    /**
     * ServiceName for shared java:app namespace
     */
    public static final ServiceName SHARED_APP_CONTEXT_SERVICE_NAME = SHARED_CONTEXT_SERVICE_NAME.append("app");

    // javax.naming Names

    /**
     * javax.naming Name for java:
     */
    public static final Name JAVA_NAME = createName("java:");

    /**
     * javax.naming Name for java:comp
     */
    public static final Name JAVA_COMP_NAME = createName("java:comp");

    /**
     * javax.naming Name for java:module
     */
    public static final Name JAVA_MODULE_NAME = createName("java:module");

    /**
     * javax.naming Name for java:app
     */
    public static final Name JAVA_APP_NAME = createName("java:app");

    /**
     * javax.naming Name for java:global
     */
    public static final Name JAVA_GLOBAL_NAME = createName("java:global");

    /**
     * javax.naming Name for java:jboss
     */
    public static final Name JAVA_JBOSS_NAME = createName("java:jboss");

    /**
     * javax.naming Name for java:jboss/exported
     */
    public static final Name JAVA_JBOSS_EXPORTED_NAME = createName("java:jboss/exported");

    private static Name createName(String s) {
        try {
            return new CompositeName(s);
        } catch (InvalidNameException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the base service name of a component's JNDI namespace.
     *
     * @param app    the application name (must not be {@code null})
     * @param module the module name (must not be {@code null})
     * @param comp   the component name (must not be {@code null})
     * @return the base service name
     */
    public static ServiceName contextServiceNameOfComponent(String app, String module, String comp) {
        return COMPONENT_CONTEXT_SERVICE_NAME.append(app, module, comp);
    }

    /**
     * Get the base service name of a module's JNDI namespace.
     *
     * @param app    the application name (must not be {@code null})
     * @param module the module name (must not be {@code null})
     * @return the base service name
     */
    public static ServiceName contextServiceNameOfModule(String app, String module) {
        return MODULE_CONTEXT_SERVICE_NAME.append(app, module);
    }

    /**
     * Get the base service name of an application's JNDI namespace.
     *
     * @param app the application name (must not be {@code null})
     * @return the base service name
     */
    public static ServiceName contextServiceNameOfApplication(String app) {
        return APPLICATION_CONTEXT_SERVICE_NAME.append(app);
    }

    /**
     * Get the service name of a jndiName, or {@code null} if there is no service mapping for the jndiName name.
     *
     * @param app     the application name
     * @param module  the module name
     * @param comp    the component name
     * @param jndiName the jndiName to check
     * @return the BindInfo
     */
    public static BindInfo bindInfoFor(String app, String module, String comp, String jndiName) {
        return bindInfoFor(app, module, comp, new JndiName(jndiName));
    }


    public static BindInfo bindInfoFor(String app, String module, String comp, JndiName jndiName) {
        if (!jndiName.isJava()) {
            return null;
        }
        final String absoluteJndiName = jndiName.getAbsoluteName();
        String bindName = absoluteJndiName;
        final ServiceName parentContextName;
        if (jndiName.isJavaComp()) {
            bindName = bindName.substring("java:comp/".length());
            if (comp != null) {
                parentContextName = contextServiceNameOfComponent(app, module, comp);
            } else {
                parentContextName = SHARED_COMP_CONTEXT_SERVICE_NAME;
            }
        } else if (jndiName.isJavaModule()) {
            bindName = bindName.substring("java:module/".length());
            if (module != null) {
                parentContextName = contextServiceNameOfModule(app, module);
            } else {
                parentContextName = SHARED_MODULE_CONTEXT_SERVICE_NAME;
            }
        } else if (jndiName.isJavaApp()) {
            bindName = bindName.substring("java:app/".length());
            if (app != null) {
                parentContextName = contextServiceNameOfApplication(app);
            } else {
                parentContextName = SHARED_APP_CONTEXT_SERVICE_NAME;
            }
        } else if (jndiName.isJavaGlobal()) {
            bindName = bindName.substring("java:global/".length());
            parentContextName = GLOBAL_CONTEXT_SERVICE_NAME;
        } else if (jndiName.isJavaJBossExported()) {
            bindName = bindName.substring("java:jboss/exported/".length());
            parentContextName = EXPORTED_CONTEXT_SERVICE_NAME;
        } else if (jndiName.isJavaJBoss()) {
            bindName = bindName.substring("java:jboss/".length());
            parentContextName = JBOSS_CONTEXT_SERVICE_NAME;
        } else {
            bindName = bindName.substring("java:".length());
            if (bindName.charAt(0) == '/') {
                bindName = bindName.substring(1);
            }
            parentContextName = JAVA_CONTEXT_SERVICE_NAME;
        }
        return new BindInfo(parentContextName, bindName, absoluteJndiName);
    }



    public static BindInfo bindInfoFor(String app, String module, String comp, boolean useCompNamespace, final String jndiName) {
        return bindInfoFor(app, module, comp, useCompNamespace, new JndiName(jndiName));
    }

    public static BindInfo bindInfoFor(String app, String module, final String jndiName) {
        return bindInfoFor(app, module, null, false, new JndiName(jndiName));
    }

    public static BindInfo bindInfoFor(String app, String module, final JndiName jndiName) {
        return bindInfoFor(app, module, null, false, jndiName);
    }

    public static BindInfo bindInfoFor(String app, final String jndiName) {
        return bindInfoFor(app, null, null, false, new JndiName(jndiName));
    }

    public static BindInfo bindInfoFor(String app, final JndiName jndiName) {
        return bindInfoFor(app, null, null, false, jndiName);
    }

    public static BindInfo bindInfoFor(String app, String module, String comp, boolean useCompNamespace, final JndiName jndiName) {
        if (jndiName.isJava()) {
            if (jndiName.isJavaComp() && !useCompNamespace) {
                return bindInfoFor(app, module, module, new JndiName("java:module" + jndiName.getAbsoluteName().substring("java:comp".length())));
            } else {
                return bindInfoFor(app, module, comp, jndiName);
            }
        } else {
            return null;
        }
    }

    /**
     * Get the service name of a NamingStore
     *
     * @param jndiName the jndi name
     * @return the bind info for the jndi name
     */
    public static BindInfo bindInfoFor(String jndiName) {
        // FIXME this hack should be removed, all relative names should be relative to same context, and java ee xml and annotations use java:comp/env
        final String scheme = InitialContext.getURLScheme(jndiName);
        if (scheme == null) {
            jndiName = "java:"+jndiName;
        }
        return bindInfoFor(new JndiName(jndiName));
    }

    public static BindInfo bindInfoFor(JndiName jndiName) {
        return bindInfoFor(null, null, null, jndiName);
    }

    public static ServiceName buildServiceName(final ServiceName parentName, final String relativeName) {
        return parentName.append(relativeName.split("/"));
    }

    public static class BindInfo {
        private final ServiceName parentContextServiceName;
        private final ServiceName binderServiceName;
        private final String bindName;
        // absolute jndi name inclusive of the namespace
        private final String absoluteJndiName;

        private BindInfo(final ServiceName parentContextServiceName, final String bindName, final String absoluteJndiName) {
            this.parentContextServiceName = parentContextServiceName;
            this.binderServiceName = buildServiceName(parentContextServiceName, bindName);
            this.bindName = bindName;
            this.absoluteJndiName = absoluteJndiName;
        }

        /**
         * The service name for the target namespace the binding will occur.
         *
         * @return The target service name
         */
        public ServiceName getParentContextServiceName() {
            return parentContextServiceName;
        }

        /**
         * The service name for binder
         *
         * @return the binder service name
         */
        public ServiceName getBinderServiceName() {
            return binderServiceName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            BindInfo bindInfo = (BindInfo) o;

            if (!binderServiceName.equals(bindInfo.binderServiceName)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return binderServiceName.hashCode();
        }

        /**
         * The name for the binding
         *
         * @return The binding name
         */
        public String getBindName() {
            return bindName;
        }

        /**
         * Returns the absolute jndi name of this {@link BindInfo}. The absolute jndi name is inclusive of the jndi namespace
         *
         * @return
         */
        public String getAbsoluteJndiName() {
            return this.absoluteJndiName;
        }

        public String toString() {
            return "BindInfo{" +
                    "parentContextServiceName=" + parentContextServiceName +
                    ", binderServiceName=" + binderServiceName +
                    ", bindName='" + bindName + '\'' +
                    ", absoluteJndiName='" + absoluteJndiName + '\'' +
                    '}';
        }

        // binding api

        /**
         * Creates a new binder service builder with the specified service target.
         * @param serviceTarget
         * @return
         */
        public BinderServiceBuilder builder(ServiceTarget serviceTarget) {
            return new BinderServiceBuilder(this, serviceTarget);
        }

        /**
         * Creates a new binder service builder with the specified service target and verification handler.
         * @param serviceTarget
         * @param verificationHandler
         * @return
         */
        public BinderServiceBuilder builder(ServiceTarget serviceTarget, ServiceVerificationHandler verificationHandler) {
            return new BinderServiceBuilder(this, serviceTarget, verificationHandler);
        }

        /**
         * Creates a new binder service builder with the specified service target and initial mode.
         * @param serviceTarget
         * @param initialMode
         * @return
         */
        public BinderServiceBuilder builder(ServiceTarget serviceTarget, ServiceController.Mode initialMode) {
            return new BinderServiceBuilder(this, serviceTarget, initialMode);
        }

        /**
         * Creates a new binder service builder with the specified service target, initial mode and verification handler.
         * @param serviceTarget
         * @param initialMode
         * @param verificationHandler
         * @return
         */
        public BinderServiceBuilder builder(ServiceTarget serviceTarget, ServiceController.Mode initialMode, ServiceVerificationHandler verificationHandler) {
            return new BinderServiceBuilder(this, serviceTarget, initialMode, verificationHandler);
        }

        /**
         * Installs a binder service, into the specified service target, where the value's source is an immediate managed reference factory for the specified value.
         * @param serviceTarget
         * @param value
         * @return
         */
        public ServiceController<ManagedReferenceFactory> bind(ServiceTarget serviceTarget, Object value) {
            return builder(serviceTarget).installService(value);
        }

        /**
         * Installs a binder service, into the specified service target, where the value's source is the specified managed reference factory.
         * @param serviceTarget
         * @param valueFactory
         * @param valueSource
         * @return
         */
        public ServiceController<ManagedReferenceFactory> bind(ServiceTarget serviceTarget, ManagedReferenceFactory valueFactory, Object valueSource) {
            return builder(serviceTarget).installService(valueFactory, valueSource);
        }

        /**
         * Installs a binder service, into the specified service target, where the value's source is obtained from the service with the specified name.
         * @param serviceTarget
         * @param valueServiceName
         * @return
         */
        public ServiceController<ManagedReferenceFactory> bind(ServiceTarget serviceTarget, ServiceName valueServiceName) {
            return builder(serviceTarget).installService(valueServiceName);
        }

        /**
         * Installs a binder service, into the specified service target, where the value's source is the binder service with specified jndi name.
         * @param serviceTarget
         * @param valueSource
         * @return
         */
        public ServiceController<ManagedReferenceFactory> bind(ServiceTarget serviceTarget, JndiName valueSource) {
            return builder(serviceTarget).installService(valueSource);
        }

    }
}
