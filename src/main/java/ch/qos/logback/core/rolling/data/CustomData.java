/*
 * SafeOnline project.
 *
 * Copyright 2006-2013 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package ch.qos.logback.core.rolling.data;

import java.util.concurrent.atomic.AtomicReference;

/**
 * User: gvhoecke <gianni.vanhoecke@lin-k.net>
 * Date: 03/12/15
 * Time: 09:54
 */
public class CustomData {

    public static final AtomicReference<String> extraS3Folder;

    static {

        extraS3Folder = new AtomicReference<String>( null );
    }
}
