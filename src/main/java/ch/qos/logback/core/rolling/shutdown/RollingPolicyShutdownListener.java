package ch.qos.logback.core.rolling.shutdown;

/**
 * User: gvhoecke <gianni.vanhoecke@lin-k.net>
 * Date: 13/07/15
 * Time: 15:35
 */
public interface RollingPolicyShutdownListener {

    /**
     * Shutdown hook that gets called when exiting the application.
     */
    void doShutdown();
}
