/*
 * Copyright 2016 linkID Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.qos.logback.core.rolling.shutdown;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * User: gvhoecke <gianni.vanhoecke@lin-k.net>
 * Date: 14/07/15
 * Time: 08:54
 */
public class ShutdownHookUtil {

    public static void registerShutdownHook(@NotNull RollingPolicyShutdownListener listener, @Nullable ShutdownHookType shutdownHookType) {

        if (shutdownHookType == null) {

            shutdownHookType = ShutdownHookType.NONE;
        }

        switch (shutdownHookType) {

            case SERVLET_CONTEXT:

                RollingPolicyContextListener.registerShutdownListener( listener );
                break;

            case JVM_SHUTDOWN_HOOK:

                Runtime.getRuntime().addShutdownHook( new Thread( new RollingPolicyJVMListener( listener ) ) );
                break;

            case NONE:
            default:

                //Do nothing
                break;
        }
    }
}
