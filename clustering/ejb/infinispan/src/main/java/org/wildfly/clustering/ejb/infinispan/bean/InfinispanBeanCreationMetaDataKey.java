/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.bean;

import org.wildfly.clustering.ee.infinispan.GroupedKey;
import org.wildfly.clustering.ejb.cache.bean.BeanCreationMetaDataKey;

/**
 * The key used to cache the creation metadata of a bean.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 */
public class InfinispanBeanCreationMetaDataKey<K> extends GroupedKey<K> implements BeanCreationMetaDataKey<K> {

    public InfinispanBeanCreationMetaDataKey(K id) {
        super(id);
    }
}
