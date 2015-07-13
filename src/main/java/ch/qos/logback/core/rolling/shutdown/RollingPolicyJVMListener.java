/*
 * SafeOnline project.
 *
 * Copyright 2006-2013 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package ch.qos.logback.core.rolling.shutdown;

/**
 * User: gvhoecke <gianni.vanhoecke@lin-k.net>
 * Date: 13/07/15
 * Time: 16:20
 */
public class RollingPolicyJVMListener implements Runnable {

    private final RollingPolicyShutdownListener listener;

    /**
     * Registers a new shutdown hook.
     *
     * @param listener The shutdown hook to register.
     */
    public RollingPolicyJVMListener( final RollingPolicyShutdownListener listener ) {

        this.listener = listener;
    }

    @Override
    public void run() {

        listener.doShutdown();
    }
}
