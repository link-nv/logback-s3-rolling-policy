package ch.qos.logback.core.rolling.shutdown;

/**
 * User: gvhoecke <gianni.vanhoecke@lin-k.net>
 * Date: 13/07/15
 * Time: 16:15
 */
public enum ShutdownHookType {

    NONE,
    SERVLET_CONTEXT,
    JVM_SHUTDOWN_HOOK
}
