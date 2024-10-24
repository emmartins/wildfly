/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.infinispan.sso;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.infinispan.Cache;
import org.wildfly.clustering.ee.infinispan.InfinispanConfiguration;
import org.wildfly.clustering.marshalling.spi.Marshaller;
import org.wildfly.clustering.web.LocalContextFactory;
import org.wildfly.clustering.web.cache.sso.AuthenticationEntry;
import org.wildfly.clustering.web.cache.sso.CompositeSSO;
import org.wildfly.clustering.web.cache.sso.SSOFactory;
import org.wildfly.clustering.web.cache.sso.SessionsFactory;
import org.wildfly.clustering.web.infinispan.logging.InfinispanWebLogger;
import org.wildfly.clustering.web.sso.SSO;
import org.wildfly.clustering.web.sso.Sessions;

/**
 * @author Paul Ferraro
 */
public class InfinispanSSOFactory<AV, SV, A, D, S, L> implements SSOFactory<Map.Entry<A, AtomicReference<L>>, SV, A, D, S, L> {

    private final SessionsFactory<SV, D, S> sessionsFactory;
    private final Cache<AuthenticationKey, AuthenticationEntry<AV, L>> findCache;
    private final Cache<AuthenticationKey, AuthenticationEntry<AV, L>> writeCache;
    private final Marshaller<A, AV> marshaller;
    private final LocalContextFactory<L> localContextFactory;

    public InfinispanSSOFactory(InfinispanConfiguration configuration, Marshaller<A, AV> marshaller, LocalContextFactory<L> localContextFactory, SessionsFactory<SV, D, S> sessionsFactory) {
        this.writeCache = configuration.getWriteOnlyCache();
        this.findCache = configuration.getReadForUpdateCache();
        this.marshaller = marshaller;
        this.localContextFactory = localContextFactory;
        this.sessionsFactory = sessionsFactory;
    }

    @Override
    public SSO<A, D, S, L> createSSO(String id, Map.Entry<Map.Entry<A, AtomicReference<L>>, SV> value) {
        Map.Entry<A, AtomicReference<L>> authenticationEntry = value.getKey();
        Sessions<D, S> sessions = this.sessionsFactory.createSessions(id, value.getValue());
        return new CompositeSSO<>(id, authenticationEntry.getKey(), sessions, authenticationEntry.getValue(), this.localContextFactory, this);
    }

    @Override
    public Map.Entry<Map.Entry<A, AtomicReference<L>>, SV> createValue(String id, A authentication) {
        try {
            AuthenticationEntry<AV, L> entry = new AuthenticationEntry<>(this.marshaller.write(authentication));
            this.writeCache.put(new AuthenticationKey(id), entry);
            SV sessions = this.sessionsFactory.createValue(id, null);
            return new AbstractMap.SimpleImmutableEntry<>(new AbstractMap.SimpleImmutableEntry<>(authentication, entry.getLocalContext()), sessions);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Map.Entry<Map.Entry<A, AtomicReference<L>>, SV> findValue(String id) {
        AuthenticationEntry<AV, L> entry = this.findCache.get(new AuthenticationKey(id));
        if (entry != null) {
            SV sessions = this.sessionsFactory.findValue(id);
            if (sessions != null) {
                try {
                    A authentication = this.marshaller.read(entry.getAuthentication());
                    return new AbstractMap.SimpleImmutableEntry<>(new AbstractMap.SimpleImmutableEntry<>(authentication, entry.getLocalContext()), sessions);
                } catch (IOException e) {
                    InfinispanWebLogger.ROOT_LOGGER.failedToActivateAuthentication(e, id);
                    this.remove(id);
                }
            }
        }
        return null;
    }

    @Override
    public boolean remove(String id) {
        this.writeCache.remove(new AuthenticationKey(id));
        this.sessionsFactory.remove(id);
        return true;
    }

    @Override
    public SessionsFactory<SV, D, S> getSessionsFactory() {
        return this.sessionsFactory;
    }
}
