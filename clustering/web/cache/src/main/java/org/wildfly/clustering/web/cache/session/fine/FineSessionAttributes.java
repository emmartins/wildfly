/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.cache.session.fine;

import java.io.IOException;
import java.io.NotSerializableException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.wildfly.clustering.ee.Immutability;
import org.wildfly.clustering.ee.MutatorFactory;
import org.wildfly.clustering.ee.UUIDFactory;
import org.wildfly.clustering.ee.cache.CacheProperties;
import org.wildfly.clustering.marshalling.spi.Marshaller;
import org.wildfly.clustering.web.cache.session.SessionAttributeActivationNotifier;
import org.wildfly.clustering.web.cache.session.SessionAttributes;

/**
 * Exposes session attributes for fine granularity sessions.
 * @author Paul Ferraro
 */
public class FineSessionAttributes<NK, K, V> implements SessionAttributes {
    private final NK key;
    private final Map<NK, Map<String, UUID>> namesCache;
    private final Function<UUID, K> keyFactory;
    private final Map<K, V> attributeCache;
    private final Map<K, Optional<Object>> mutations = new HashMap<>();
    private final Marshaller<Object, V> marshaller;
    private final MutatorFactory<K, V> mutatorFactory;
    private final Immutability immutability;
    private final CacheProperties properties;
    private final SessionAttributeActivationNotifier notifier;
    private final AtomicReference<Map<String, UUID>> names;

    public FineSessionAttributes(NK key, AtomicReference<Map<String, UUID>> names, Map<NK, Map<String, UUID>> namesCache, Function<UUID, K> keyFactory, Map<K, V> attributeCache, Marshaller<Object, V> marshaller, MutatorFactory<K, V> mutatorFactory, Immutability immutability, CacheProperties properties, SessionAttributeActivationNotifier notifier) {
        this.key = key;
        this.names = names;
        this.namesCache = namesCache;
        this.keyFactory = keyFactory;
        this.attributeCache = attributeCache;
        this.marshaller = marshaller;
        this.mutatorFactory = mutatorFactory;
        this.immutability = immutability;
        this.properties = properties;
        this.notifier = notifier;
    }

    @Override
    public Object removeAttribute(String name) {
        UUID attributeId = this.names.get().get(name);
        if (attributeId == null) return null;

        synchronized (this.mutations) {
            this.setNames(this.namesCache.compute(this.key, this.properties.isTransactional() ? new CopyOnWriteSessionAttributeMapRemoveFunction(name) : new ConcurrentSessionAttributeMapRemoveFunction(name)));

            K key = this.keyFactory.apply(attributeId);

            Object result = this.read(this.attributeCache.remove(key));
            if (result != null) {
                this.mutations.remove(key);

                if (this.properties.isPersistent()) {
                    this.notifier.postActivate(result);
                }
            }
            return result;
        }
    }

    @Override
    public Object setAttribute(String name, Object attribute) {
        if (attribute == null) {
            return this.removeAttribute(name);
        }
        if (this.properties.isMarshalling() && !this.marshaller.isMarshallable(attribute)) {
            throw new IllegalArgumentException(new NotSerializableException(attribute.getClass().getName()));
        }

        UUID attributeId = this.names.get().get(name);

        synchronized (this.mutations) {
            if (attributeId == null) {
                UUID newAttributeId = UUIDFactory.INSECURE.get();
                this.setNames(this.namesCache.compute(this.key, this.properties.isTransactional() ? new CopyOnWriteSessionAttributeMapPutFunction(name, newAttributeId) : new ConcurrentSessionAttributeMapPutFunction(name, newAttributeId)));
                attributeId = this.names.get().get(name);
            }

            K key = this.keyFactory.apply(attributeId);
            V value = this.write(attribute);

            if (this.properties.isPersistent()) {
                this.notifier.prePassivate(attribute);
            }

            Object result = this.read(this.attributeCache.put(key, value));

            if (this.properties.isTransactional()) {
                // Add an empty value to prevent any subsequent mutable getAttribute(...) from triggering a redundant mutation on close.
                this.mutations.put(key, Optional.empty());
            } else {
                // If the object is mutable, we need to indicate trigger a mutation on close
                if (this.immutability.test(attribute)) {
                    this.mutations.remove(key);
                } else {
                    this.mutations.put(key, Optional.of(attribute));
                }
            }

            if (this.properties.isPersistent()) {
                this.notifier.postActivate(attribute);

                if (result != attribute) {
                    this.notifier.postActivate(result);
                }
            }

            return result;
        }
    }

    @Override
    public Object getAttribute(String name) {
        UUID attributeId = this.names.get().get(name);
        if (attributeId == null) return null;

        synchronized (this.mutations) {
            K key = this.keyFactory.apply(attributeId);

            // Return mutable value if present, this preserves referential integrity when this member is not an owner.
            Optional<Object> mutableValue = this.mutations.get(key);
            if ((mutableValue != null) && mutableValue.isPresent()) {
                return mutableValue.get();
            }

            Object result = this.read(this.attributeCache.get(key));
            if (result != null) {
                if (this.properties.isPersistent()) {
                    this.notifier.postActivate(result);
                }

                // If the object is mutable, we need to trigger a mutation on close
                if (!this.immutability.test(result)) {
                    this.mutations.putIfAbsent(key, Optional.of(result));
                }
            }
            return result;
        }
    }

    @Override
    public Set<String> getAttributeNames() {
        return this.names.get().keySet();
    }

    @Override
    public void close() {
        synchronized (this.mutations) {
            this.notifier.close();
            for (Map.Entry<K, Optional<Object>> entry : this.mutations.entrySet()) {
                Optional<Object> optional = entry.getValue();
                if (optional.isPresent()) {
                    K key = entry.getKey();
                    V value = this.write(optional.get());
                    this.mutatorFactory.createMutator(key, value).mutate();
                }
            }
            this.mutations.clear();
        }
    }

    private void setNames(Map<String, UUID> names) {
        this.names.set((names != null) ? Collections.unmodifiableMap(names) : Collections.emptyMap());
    }

    private V write(Object value) {
        try {
            return this.marshaller.write(value);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private Object read(V value) {
        try {
            return this.marshaller.read(value);
        } catch (IOException e) {
            // This should not happen here, since attributes were pre-activated during session construction
            throw new IllegalStateException(e);
        }
    }
}
