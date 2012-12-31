package org.jboss.as.test.integration.ejb.remote.http.client.api.security;

public interface SecuredCounterRemote {

    int addAndGet(int delta);

}
