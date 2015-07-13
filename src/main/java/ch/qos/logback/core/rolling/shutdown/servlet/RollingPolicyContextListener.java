/*
 * SafeOnline project.
 *
 * Copyright 2006-2013 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package ch.qos.logback.core.rolling.shutdown.servlet;

import ch.qos.logback.core.rolling.shutdown.RollingPolicyShutdownListener;
import com.google.common.collect.Lists;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.List;

/**
 * User: gvhoecke <gianni.vanhoecke@lin-k.net>
 * Date: 13/07/15
 * Time: 15:36
 */
public class RollingPolicyContextListener implements ServletContextListener {

    private static final List<RollingPolicyShutdownListener> listeners;

    static {

        listeners = Lists.newArrayList();
    }

    /**
     * Registers a new shutdown hook.
     *
     * @param listener The shutdown hook to register.
     */
    public static void registerShutdownListener( final RollingPolicyShutdownListener listener ) {

        if( !listeners.contains( listener ) ) {

            listeners.add( listener );
        }
    }

    /**
     * Deregisters a previously registered shutdown hook.
     *
     * @param listener The shutdown hook to deregister.
     */
    public static void deregisterShutdownListener( final RollingPolicyShutdownListener listener ) {

        if( listeners.contains( listener ) ) {

            listeners.remove( listener );
        }
    }

    @Override
    public void contextInitialized( ServletContextEvent servletContextEvent ) {

        //Empty
    }

    @Override
    public void contextDestroyed( ServletContextEvent servletContextEvent ) {

        //Upload
        for( RollingPolicyShutdownListener listener : listeners ) {

            listener.doShutdown();
        }
    }
}
