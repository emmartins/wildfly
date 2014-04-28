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

package org.jboss.as.naming;

import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

import javax.naming.Binding;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A service based naming store
 * @author Eduardo Martins
 */
public class ServiceBasedNamingStoreComposition extends ServiceBasedNamingStore {

    private final List<ServiceBasedNamingStore> otherNamingStores;

    public ServiceBasedNamingStoreComposition(final ServiceRegistry serviceRegistry, final Name baseName, final ServiceName serviceNameBase, final List<ServiceBasedNamingStore> otherNamingStores) {
        super(serviceRegistry, baseName, serviceNameBase);
        this.otherNamingStores = otherNamingStores != null ? otherNamingStores : Collections.EMPTY_LIST;
    }

    @Override
    public Object lookup(Name name, boolean dereference) throws NamingException {
        try {
            return super.lookup(name, dereference);
        } catch (NameNotFoundException e) {
            for (ServiceBasedNamingStore otherNamingStore : otherNamingStores) {
                try {
                    return otherNamingStore.lookup(name, dereference);
                } catch (NameNotFoundException e1) {
                    // ignore
                }
            }
            throw e;
        }
    }

    @Override
    public List<NameClassPair> list(Name name) throws NamingException {
        Map<String, String> map = new HashMap<>();
        boolean listWithoutException = false;
        for (ServiceBasedNamingStore otherNamingStore : otherNamingStores) {
            try {
                map.putAll(otherNamingStore.listMap(name));
                listWithoutException = true;
            } catch (Throwable e) {
                // ignore
            }
        }
        try {
            map.putAll(super.listMap(name));
        } catch (NamingException e) {
            if (!listWithoutException) {
                throw e;
            }
        }
        final List<NameClassPair> results = new ArrayList<>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            results.add(new NameClassPair(entry.getKey(), entry.getValue()));
        }
        return results;
    }

    @Override
    public List<Binding> listBindings(Name name) throws NamingException {
        Map<String, Object> map = new HashMap<>();
        boolean listWithoutException = false;
        for (ServiceBasedNamingStore otherNamingStore : otherNamingStores) {
            try {
                map.putAll(otherNamingStore.listBindingsMap(name));
                listWithoutException = true;
            } catch (Throwable e) {
                // ignore
            }
        }
        try {
            map.putAll(super.listBindingsMap(name));
        } catch (NamingException e) {
            if (!listWithoutException) {
                throw e;
            }
        }
        final List<Binding> results = new ArrayList<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            results.add(new Binding(entry.getKey(), entry.getValue()));
        }
        return results;
    }

}
