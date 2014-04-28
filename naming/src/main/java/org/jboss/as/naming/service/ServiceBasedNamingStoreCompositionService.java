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

package org.jboss.as.naming.service;

import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.ServiceBasedNamingStoreComposition;
import org.jboss.as.naming.logging.NamingLogger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

import javax.naming.Name;
import javax.naming.NamingException;
import java.util.List;

/**
 * Service responsible for managing the creation and life-cycle of a read only service based naming store composition.
 *
 * @author Eduardo Martins
 */
public abstract class ServiceBasedNamingStoreCompositionService implements Service<ServiceBasedNamingStoreComposition> {

    protected final Name baseName;

    protected volatile ServiceBasedNamingStoreComposition store;

    public ServiceBasedNamingStoreCompositionService(Name baseName) {
        this.baseName = (Name) baseName.clone();
    }

    protected abstract List<ServiceBasedNamingStore> getOtherNamingStores();

    /**
     * Creates the naming store if not provided by the constructor.
     *
     * @param context The start context
     * @throws org.jboss.msc.service.StartException If any problems occur creating the context
     */
    public void start(final StartContext context) throws StartException {
        if(store == null) {
            final ServiceRegistry serviceRegistry = context.getController().getServiceContainer();
            final ServiceName serviceNameBase = context.getController().getName();
            store = new ServiceBasedNamingStoreComposition(serviceRegistry, baseName, serviceNameBase, getOtherNamingStores());
        }
    }

    /**
     * Destroys the naming store.
     *
     * @param context The stop context
     */
    public void stop(StopContext context) {
        if(store != null) {
            try {
                store.close();
                store = null;
            } catch (NamingException e) {
                throw NamingLogger.ROOT_LOGGER.failedToDestroyRootContext(e);
            }
        }
    }

    /**
     * Get the context value.
     *
     * @return The naming store
     * @throws IllegalStateException
     */
    public ServiceBasedNamingStoreComposition getValue() throws IllegalStateException {
        return store;
    }

}
