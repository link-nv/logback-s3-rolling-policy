/*
 * SafeOnline project.
 *
 * Copyright 2006-2013 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package ch.qos.logback.core.rolling.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.util.UUID;

/**
 * User: gvhoecke <gianni.vanhoecke@lin-k.net>
 * Date: 15/07/15
 * Time: 08:43
 */
public class IdentifierUtil {

    @NotNull
    public static String getIdentifier() {

        String identifier;

        //
        // 1. Try AWS EC2 Instance ID
        //

        identifier = getContentOfWebpage( "http://instance-data/latest/meta-data/instance-id" );

        if( identifier != null )
            return identifier;

        //
        // 2. Try hostname
        //

        identifier = getHostname();

        if( identifier != null )
            return identifier;

        //
        // 3. When the above 2 methods failed, generate a unique ID
        //

        return UUID.randomUUID().toString();
    }

    @Nullable
    public static String getContentOfWebpage( String location ) {

        try {

            URL url = new URL( location );

            URLConnection con = url.openConnection();
            InputStream in = con.getInputStream();
            String encoding = con.getContentEncoding();
            encoding = encoding == null ? "UTF-8" : encoding;

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[ 8192 ];
            int len = 0;

            while( ( len = in.read( buf ) ) != -1 ) {

                baos.write( buf, 0, len );
            }

            String body = new String( baos.toByteArray(), encoding );

            if( body.trim().length() > 0 )
                return body.trim();
        } catch( Exception e ) {

            return null;
        }

        return null;
    }

    @Nullable
    public static String getHostname() {

        try {

            String hostname = InetAddress.getLocalHost().getHostAddress();

            if( hostname != null )
                hostname = hostname.replaceAll( "[^a-zA-Z0-9.]+", "" ).trim();

            if( hostname != null && hostname.length() > 0 )
                return hostname;
        } catch( Exception e ) {

            return null;
        }

        return null;
    }
}
