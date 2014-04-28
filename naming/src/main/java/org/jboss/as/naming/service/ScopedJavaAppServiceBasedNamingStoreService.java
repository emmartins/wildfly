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
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.msc.value.InjectedValue;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Eduardo Martins
 */
public class ScopedJavaAppServiceBasedNamingStoreService extends ServiceBasedNamingStoreCompositionService {

    private final InjectedValue<ServiceBasedNamingStore> sharedJavaAppServiceBasedNamingStore = new InjectedValue<>();

    public ScopedJavaAppServiceBasedNamingStoreService() {
        super(ContextNames.JAVA_APP_NAME);
    }

    @Override
    protected List<ServiceBasedNamingStore> getOtherNamingStores() {
        final List<ServiceBasedNamingStore> otherNamingStores = new ArrayList<>();
        otherNamingStores.add(sharedJavaAppServiceBasedNamingStore.getValue());
        return otherNamingStores;
    }

    public InjectedValue<ServiceBasedNamingStore> getSharedJavaAppServiceBasedNamingStore() {
        return sharedJavaAppServiceBasedNamingStore;
    }
}
