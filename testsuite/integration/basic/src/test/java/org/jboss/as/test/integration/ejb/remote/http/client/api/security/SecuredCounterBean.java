package org.jboss.as.test.integration.ejb.remote.http.client.api.security;

import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.security.RolesAllowed;
import javax.ejb.Remote;
import javax.ejb.Stateful;

import org.jboss.ejb3.annotation.SecurityDomain;

@Stateful
@RolesAllowed({ "Users" })
@SecurityDomain("other")
@Remote(SecuredCounterRemote.class)
public class SecuredCounterBean implements SecuredCounterRemote {

    private AtomicInteger counter = new AtomicInteger(0);

    @Override
    public int addAndGet(int delta) {
        return counter.addAndGet(delta);
    }


}
