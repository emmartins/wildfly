/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.infinispan.scheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.wildfly.clustering.ee.Scheduler;
import org.wildfly.clustering.ee.infinispan.GroupedKey;
import org.wildfly.clustering.infinispan.distribution.Locality;

/**
 * Abstract {@link CacheEntryScheduler} that delegates to a local scheduler.
 * @author Paul Ferraro
 */
public abstract class AbstractCacheEntryScheduler<I, M> implements CacheEntryScheduler<I, M> {

    private final Scheduler<I, Instant> scheduler;
    private final Function<M, Instant> instant;

    protected AbstractCacheEntryScheduler(Scheduler<I, Instant> scheduler, Function<M, Duration> duration, Predicate<Duration> skip, Function<M, Instant> basis) {
        this(scheduler, new AdditionFunction<>(duration, skip, basis));
    }

    protected AbstractCacheEntryScheduler(Scheduler<I, Instant> scheduler, Function<M, Instant> instant) {
        this.scheduler = scheduler;
        this.instant = instant;
    }

    @Override
    public void schedule(I id, M metaData) {
        Instant instant = this.instant.apply(metaData);
        if (instant != null) {
            this.scheduler.schedule(id, instant);
        }
    }

    @Override
    public void cancel(I id) {
        this.scheduler.cancel(id);
    }

    @Override
    public void cancel(Locality locality) {
        try (Stream<I> stream = this.scheduler.stream()) {
            Iterator<I> entries = stream.iterator();
            while (entries.hasNext()) {
                if (Thread.currentThread().isInterrupted()) break;
                I id = entries.next();
                if (!locality.isLocal(new GroupedKey<>(id))) {
                    this.scheduler.cancel(id);
                }
            }
        }
    }

    @Override
    public Stream<I> stream() {
        return this.scheduler.stream();
    }

    @Override
    public void close() {
        this.scheduler.close();
    }

    @Override
    public String toString() {
        return this.scheduler.toString();
    }

    private static class AdditionFunction<M> implements Function<M, Instant> {
        private final Function<M, Duration> duration;
        private final Predicate<Duration> skip;
        private final Function<M, Instant> basis;

        AdditionFunction(Function<M, Duration> duration, Predicate<Duration> skip, Function<M, Instant> basis) {
            this.duration = duration;
            this.skip = skip;
            this.basis = basis;
        }

        @Override
        public Instant apply(M metaData) {
            Duration duration = this.duration.apply(metaData);
            return !this.skip.test(duration) ? this.basis.apply(metaData).plus(duration) : null;
        }
    }
}
