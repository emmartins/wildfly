/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ee.naming;

import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.ee.component.BindingConfiguration;
import org.jboss.as.ee.component.InjectionSource;
import org.jboss.as.ee.logging.EeLogger;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.deployment.JndiName;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.CircularDependencyException;
import org.jboss.msc.service.DuplicateServiceException;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Binding configurations wrt EE deployment.
 * @author Eduardo Martins
 */
public class BindingConfigurations {

    protected final List<BindingConfiguration> deploymentBindings = new ArrayList<>();

    protected final List<BindingConfiguration> annotationBindings = new ArrayList<>();

    protected final List<BindingConfiguration> platformBindings = new ArrayList<>();

    /**
     * Adds the specified configuration to deployment bindings.
     * @param bindingConfiguration
     */
    public synchronized void addDeploymentBinding(BindingConfiguration bindingConfiguration) {
        deploymentBindings.add(bindingConfiguration);
    }

    /**
     * Adds the specified configuration to annotation bindings.
     * @param bindingConfiguration
     */
    public synchronized void addAnnotationBinding(BindingConfiguration bindingConfiguration) {
        annotationBindings.add(bindingConfiguration);
    }

    /**
     * Adds the specified configuration to platform bindings.
     * @param bindingConfiguration
     */
    public synchronized void addPlatformBinding(BindingConfiguration bindingConfiguration) {
        platformBindings.add(bindingConfiguration);
    }

    /**
     * Does the bindings.
     * @param phaseContext
     * @param resolutionContext
     * @param serviceVerificationHandler
     * @throws DeploymentUnitProcessingException if there is a failure resolving a binding source
     * @throws java.lang.IllegalArgumentException if a binding already exists with a different source
     * @return a set with the the service names of each binding done
     */
    public synchronized Set<ServiceName> doBindings(DeploymentPhaseContext phaseContext, InjectionSource.ResolutionContext resolutionContext, ServiceVerificationHandler serviceVerificationHandler) throws DeploymentUnitProcessingException, IllegalArgumentException {
        final Set<ServiceName> result = new HashSet<>();
        final Map<JndiName, InjectionSource> deploymentBindingsDone = new HashMap<>();
        // bind all deployment bindings
        for (BindingConfiguration bindingConfiguration : deploymentBindings) {
            final InjectionSource injectionSource = deploymentBindingsDone.put(bindingConfiguration.getName(), bindingConfiguration.getSource());
            if (injectionSource != null && !injectionSource.equals(bindingConfiguration.getSource())) {
                throw EeLogger.ROOT_LOGGER.conflictingBinding(bindingConfiguration.getName(), bindingConfiguration.getSource());
            }
            bind(bindingConfiguration.getName(), bindingConfiguration.getSource(), phaseContext, result, resolutionContext, serviceVerificationHandler);
        }
        // bind all annotation bindings not in deployment bindings
        final Map<JndiName, InjectionSource> annotationBindingsDone = new HashMap<>();
        for (BindingConfiguration bindingConfiguration : annotationBindings) {
            if (deploymentBindingsDone.containsKey(bindingConfiguration.getName())) {
                continue;
            }
            final InjectionSource injectionSource = annotationBindingsDone.put(bindingConfiguration.getName(), bindingConfiguration.getSource());
            if (injectionSource != null && !injectionSource.equals(bindingConfiguration.getSource())) {
                throw EeLogger.ROOT_LOGGER.conflictingBinding(bindingConfiguration.getName(), bindingConfiguration.getSource());
            }
            bind(bindingConfiguration.getName(), bindingConfiguration.getSource(), phaseContext, result, resolutionContext, serviceVerificationHandler);
        }
        // bind all platform bindings not in deployment and annotation bindings
        final Map<JndiName, InjectionSource> platformBindingsDone = new HashMap<>();
        for (BindingConfiguration bindingConfiguration : platformBindings) {
            if (deploymentBindingsDone.containsKey(bindingConfiguration.getName()) || annotationBindingsDone.containsKey(bindingConfiguration.getName())) {
                continue;
            }
            final InjectionSource injectionSource = platformBindingsDone.put(bindingConfiguration.getName(), bindingConfiguration.getSource());
            if (injectionSource != null && !injectionSource.equals(bindingConfiguration.getSource())) {
                throw EeLogger.ROOT_LOGGER.conflictingBinding(bindingConfiguration.getName(), bindingConfiguration.getSource());
            }
            bind(bindingConfiguration.getName(), bindingConfiguration.getSource(), phaseContext, result, resolutionContext, serviceVerificationHandler);
        }
        return result;
    }

    /**
     * Does the specified bind.
     * @param jndiName
     * @param injectionSource
     * @param phaseContext
     * @param servicesBound
     * @param resolutionContext
     * @param serviceVerificationHandler
     * @throws DeploymentUnitProcessingException if there is a failure resolving a binding source
     * @throws java.lang.IllegalArgumentException if a binder service failed to install, because it already exists, and the binding value source of both binder services aren't equal
     */private
    void bind(final JndiName jndiName, final InjectionSource injectionSource, final DeploymentPhaseContext phaseContext, Set<ServiceName> servicesBound, final InjectionSource.ResolutionContext resolutionContext, final ServiceVerificationHandler serviceVerificationHandler) throws DeploymentUnitProcessingException, IllegalArgumentException {
        final boolean shared = !jndiName.isJavaComp() && !jndiName.isJavaModule() && !jndiName.isJavaApp();
        final ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(resolutionContext.getApplicationName(), resolutionContext.getModuleName(), resolutionContext.getComponentName(), !resolutionContext.isCompUsesModule(), jndiName);
        BinderService service = null;
        try {
            service = new BinderService(bindInfo.getBindName(), injectionSource, shared);
            final ServiceTarget serviceTarget = shared ? CurrentServiceContainer.getServiceContainer() : phaseContext.getServiceTarget();
            final ServiceBuilder<ManagedReferenceFactory> serviceBuilder = serviceTarget.addService(bindInfo.getBinderServiceName(), service);
            injectionSource.getResourceValue(resolutionContext, serviceBuilder, phaseContext, service.getManagedObjectInjector());
            serviceBuilder.addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, service.getNamingStoreInjector());
            serviceBuilder.addListener(serviceVerificationHandler);
            serviceBuilder.install();
        } catch (DuplicateServiceException e) {
            ServiceController<ManagedReferenceFactory> registered = (ServiceController<ManagedReferenceFactory>) CurrentServiceContainer.getServiceContainer().getService(bindInfo.getBinderServiceName());
            if (registered == null)
                throw e;
            service = (BinderService) registered.getService();
            if (!service.getSource().equals(injectionSource))
                throw EeLogger.ROOT_LOGGER.conflictingBinding(jndiName, injectionSource);
        } catch (CircularDependencyException e) {
            throw EeLogger.ROOT_LOGGER.circularDependency(jndiName.getAbsoluteName());
        }
        if (shared && service != null) {
            //as these bindings are not child services
            //we need to add a listener that released the service when the deployment stops
            service.acquire();
            ServiceController<?> unitService = CurrentServiceContainer.getServiceContainer().getService(phaseContext.getDeploymentUnit().getServiceName());
            unitService.addListener(new BinderReleaseListener(service));
        }
        servicesBound.add(bindInfo.getBinderServiceName());
    }

    private static class BinderReleaseListener<T> extends AbstractServiceListener<T> {

        private final BinderService binderService;

        public BinderReleaseListener(final BinderService binderService) {
            this.binderService = binderService;
        }

        @Override
        public void listenerAdded(final ServiceController<? extends T> serviceController) {
            if (serviceController.getState() == ServiceController.State.DOWN || serviceController.getState() == ServiceController.State.STOPPING) {
                binderService.release();
                serviceController.removeListener(this);
            }
        }

        @Override
        public void transition(final ServiceController<? extends T> serviceController, final ServiceController.Transition transition) {
            if (transition.getAfter() == ServiceController.Substate.STOPPING) {
                binderService.release();
                serviceController.removeListener(this);
            }
        }
    }

}
