package org.wildfly.concurrency;

import jakarta.enterprise.concurrent.ContextService;
import org.jboss.as.ee.concurrent.ContextServiceTypesConfiguration;

/**
 * Extension of the ContextService API.
 * @author emmartins
 */
public interface ContextServiceExt extends ContextService {

    public ContextServiceTypesConfiguration getContextServiceTypesConfiguration();
}
