/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.hotrod.session.fine;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.infinispan.client.hotrod.RemoteCache;
import org.wildfly.clustering.ee.Immutability;
import org.wildfly.clustering.ee.MutatorFactory;
import org.wildfly.clustering.ee.cache.CacheProperties;
import org.wildfly.clustering.ee.hotrod.RemoteCacheMap;
import org.wildfly.clustering.ee.hotrod.RemoteCacheMutatorFactory;
import org.wildfly.clustering.marshalling.spi.Marshaller;
import org.wildfly.clustering.web.cache.session.CompositeImmutableSession;
import org.wildfly.clustering.web.cache.session.ImmutableSessionAttributeActivationNotifier;
import org.wildfly.clustering.web.cache.session.SessionAttributeActivationNotifier;
import org.wildfly.clustering.web.cache.session.SessionAttributes;
import org.wildfly.clustering.web.cache.session.SessionAttributesFactory;
import org.wildfly.clustering.web.cache.session.fine.FineImmutableSessionAttributes;
import org.wildfly.clustering.web.cache.session.fine.FineSessionAttributes;
import org.wildfly.clustering.web.hotrod.logging.Logger;
import org.wildfly.clustering.web.hotrod.session.HotRodSessionAttributesFactoryConfiguration;
import org.wildfly.clustering.web.session.HttpSessionActivationListenerProvider;
import org.wildfly.clustering.web.session.ImmutableSessionAttributes;
import org.wildfly.clustering.web.session.ImmutableSessionMetaData;

/**
 * {@link SessionAttributesFactory} for fine granularity sessions.
 * A given session's attributes are mapped to N+1 co-located cache entries, where N is the number of session attributes.
 * A separate cache entry stores the activate attribute names for the session.
 * @author Paul Ferraro
 */
public class FineSessionAttributesFactory<S, C, L, V> implements SessionAttributesFactory<C, AtomicReference<Map<String, UUID>>> {

    private final RemoteCache<SessionAttributeNamesKey, Map<String, UUID>> namesCache;
    private final RemoteCache<SessionAttributeKey, V> attributeCache;
    private final Marshaller<Object, V> marshaller;
    private final Immutability immutability;
    private final CacheProperties properties;
    private final MutatorFactory<SessionAttributeKey, V> mutatorFactory;
    private final HttpSessionActivationListenerProvider<S, C, L> provider;

    public FineSessionAttributesFactory(HotRodSessionAttributesFactoryConfiguration<S, C, L, Object, V> configuration) {
        this.namesCache = configuration.getCache();
        this.attributeCache = configuration.getCache();
        this.marshaller = configuration.getMarshaller();
        this.immutability = configuration.getImmutability();
        this.properties = configuration.getCacheProperties();
        this.mutatorFactory = new RemoteCacheMutatorFactory<>(this.attributeCache);
        this.provider = configuration.getHttpSessionActivationListenerProvider();
    }

    @Override
    public AtomicReference<Map<String, UUID>> createValue(String id, Void context) {
        return new AtomicReference<>(Collections.emptyMap());
    }

    @Override
    public AtomicReference<Map<String, UUID>> findValue(String id) {
        return this.getValue(id, true);
    }

    @Override
    public AtomicReference<Map<String, UUID>> tryValue(String id) {
        return this.getValue(id, false);
    }

    private AtomicReference<Map<String, UUID>> getValue(String id, boolean purgeIfInvalid) {
        Map<String, UUID> names = this.namesCache.get(new SessionAttributeNamesKey(id));
        if (names != null) {
            // Validate all attributes
            Map<SessionAttributeKey, String> attributes = new HashMap<>();
            for (Map.Entry<String, UUID> entry : names.entrySet()) {
                attributes.put(new SessionAttributeKey(id, entry.getValue()), entry.getKey());
            }
            Map<SessionAttributeKey, V> entries = this.attributeCache.getAll(attributes.keySet());
            for (Map.Entry<SessionAttributeKey, String> attribute : attributes.entrySet()) {
                V value = entries.get(attribute.getKey());
                if (value != null) {
                    try {
                        this.marshaller.read(value);
                        continue;
                    } catch (IOException e) {
                        Logger.ROOT_LOGGER.failedToActivateSessionAttribute(e, id, attribute.getValue());
                    }
                } else {
                    Logger.ROOT_LOGGER.missingSessionAttributeCacheEntry(id, attribute.getValue());
                }
                if (purgeIfInvalid) {
                    this.purge(id);
                }
                return null;
            }
            return new AtomicReference<>(names);
        }
        return new AtomicReference<>(Collections.emptyMap());
    }

    @Override
    public boolean remove(String id) {
        SessionAttributeNamesKey key = new SessionAttributeNamesKey(id);
        Map<String, UUID> names = this.namesCache.get(key);
        if (names != null) {
            for (UUID attributeId : names.values()) {
                this.attributeCache.remove(new SessionAttributeKey(id, attributeId));
            }
            this.namesCache.remove(key);
        }
        return true;
    }

    @Override
    public SessionAttributes createSessionAttributes(String id, AtomicReference<Map<String, UUID>> names, ImmutableSessionMetaData metaData, C context) {
        SessionAttributeActivationNotifier notifier = new ImmutableSessionAttributeActivationNotifier<>(this.provider, new CompositeImmutableSession(id, metaData, this.createImmutableSessionAttributes(id, names)), context);
        return new FineSessionAttributes<>(new SessionAttributeNamesKey(id), names, this.namesCache, getKeyFactory(id), new RemoteCacheMap<>(this.attributeCache), this.marshaller, this.mutatorFactory, this.immutability, this.properties, notifier);
    }

    @Override
    public ImmutableSessionAttributes createImmutableSessionAttributes(String id, AtomicReference<Map<String, UUID>> names) {
        return new FineImmutableSessionAttributes<>(names, getKeyFactory(id), this.attributeCache, this.marshaller);
    }

    private static Function<UUID, SessionAttributeKey> getKeyFactory(String id) {
        return new Function<>() {
            @Override
            public SessionAttributeKey apply(UUID attributeId) {
                return new SessionAttributeKey(id, attributeId);
            }
        };
    }
}
