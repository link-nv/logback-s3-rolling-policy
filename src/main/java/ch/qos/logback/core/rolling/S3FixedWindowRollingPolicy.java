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

package ch.qos.logback.core.rolling;

import ch.qos.logback.core.rolling.aws.AmazonS3ClientImpl;
import ch.qos.logback.core.rolling.shutdown.RollingPolicyShutdownListener;
import ch.qos.logback.core.rolling.shutdown.ShutdownHookType;
import ch.qos.logback.core.rolling.shutdown.ShutdownHookUtil;
import java.util.Date;


/**
 * User: gvhoecke <gianni.vanhoecke@lin-k.net>
 * Date: 13/07/15
 * Time: 16:53
 */
public class S3FixedWindowRollingPolicy extends FixedWindowRollingPolicy implements RollingPolicyShutdownListener {

    private String           awsAccessKey;
    private String           awsSecretKey;
    private String           s3BucketName;
    private String           s3FolderName;
    private ShutdownHookType shutdownHookType;
    private boolean          rolloverOnExit;
    private boolean          prefixTimestamp;
    private boolean          prefixIdentifier;

    private AmazonS3ClientImpl s3Client;

    public S3FixedWindowRollingPolicy() {

        super();

        rolloverOnExit = false;
        shutdownHookType = ShutdownHookType.NONE;
        prefixTimestamp = false;
        prefixIdentifier = false;
    }

    @Override
    public void start() {

        super.start();

        //Init S3 client
        s3Client = new AmazonS3ClientImpl( getAwsAccessKey(), getAwsSecretKey(), getS3BucketName(), getS3FolderName(), isPrefixTimestamp(),
                isPrefixIdentifier() );

        if (isPrefixIdentifier()) {

            addInfo( "Using identifier prefix \"" + s3Client.getIdentifier() + "\"" );
        }

        //Register shutdown hook so the log gets uploaded on shutdown, if needed
        ShutdownHookUtil.registerShutdownHook( this, getShutdownHookType() );
    }

    @Override
    public void rollover()
            throws RolloverFailure {

        super.rollover();

        //Upload the current log file into S3
        s3Client.uploadFileToS3Async( fileNamePattern.convertInt( getMinIndex() ), new Date() );
    }

    /**
     * Shutdown hook that gets called when exiting the application.
     */
    @Override
    public void doShutdown() {

        if (isRolloverOnExit()) {

            //Do rolling and upload the rolled file on exit
            rollover();
        } else {

            //Upload the active log file without rolling
            s3Client.uploadFileToS3Async( getActiveFileName(), new Date(), true );
        }

        //Wait until finishing the upload
        s3Client.doShutdown();
    }

    public String getAwsAccessKey() {

        return awsAccessKey;
    }

    public void setAwsAccessKey(String awsAccessKey) {

        this.awsAccessKey = awsAccessKey;
    }

    public String getAwsSecretKey() {

        return awsSecretKey;
    }

    public void setAwsSecretKey(String awsSecretKey) {

        this.awsSecretKey = awsSecretKey;
    }

    public String getS3BucketName() {

        return s3BucketName;
    }

    public void setS3BucketName(String s3BucketName) {

        this.s3BucketName = s3BucketName;
    }

    public String getS3FolderName() {

        return s3FolderName;
    }

    public void setS3FolderName(String s3FolderName) {

        this.s3FolderName = s3FolderName;
    }

    public boolean isRolloverOnExit() {

        return rolloverOnExit;
    }

    public void setRolloverOnExit(boolean rolloverOnExit) {

        this.rolloverOnExit = rolloverOnExit;
    }

    public ShutdownHookType getShutdownHookType() {

        return shutdownHookType;
    }

    public void setShutdownHookType(ShutdownHookType shutdownHookType) {

        this.shutdownHookType = shutdownHookType;
    }

    public boolean isPrefixTimestamp() {

        return prefixTimestamp;
    }

    public void setPrefixTimestamp(boolean prefixTimestamp) {

        this.prefixTimestamp = prefixTimestamp;
    }

    public boolean isPrefixIdentifier() {

        return prefixIdentifier;
    }

    public void setPrefixIdentifier(boolean prefixIdentifier) {

        this.prefixIdentifier = prefixIdentifier;
    }
}
