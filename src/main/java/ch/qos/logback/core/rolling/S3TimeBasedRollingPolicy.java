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

import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.rolling.aws.AmazonS3ClientImpl;
import ch.qos.logback.core.rolling.shutdown.RollingPolicyShutdownListener;
import ch.qos.logback.core.rolling.shutdown.ShutdownHookType;
import ch.qos.logback.core.rolling.shutdown.ShutdownHookUtil;
import java.io.File;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class S3TimeBasedRollingPolicy<E> extends TimeBasedRollingPolicy<E> implements RollingPolicyShutdownListener {

    private String           awsAccessKey;
    private String           awsSecretKey;
    private String           s3BucketName;
    private String           s3FolderName;
    private ShutdownHookType shutdownHookType;
    private boolean          rolloverOnExit;
    private boolean          prefixTimestamp;
    private boolean          prefixIdentifier;

    private AmazonS3ClientImpl s3Client;
    private ExecutorService    executor;

    private Date lastPeriod;

    public S3TimeBasedRollingPolicy() {

        super();

        rolloverOnExit = false;
        shutdownHookType = ShutdownHookType.NONE;
        prefixTimestamp = false;
        prefixIdentifier = false;

        executor = Executors.newFixedThreadPool( 1 );

        lastPeriod = new Date();
    }

    @Override
    public void start() {

        super.start();

        lastPeriod = getLastPeriod();

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

        if (timeBasedFileNamingAndTriggeringPolicy.getElapsedPeriodsFileName() != null) {

            final String elapsedPeriodsFileName = String.format( "%s%s", timeBasedFileNamingAndTriggeringPolicy.getElapsedPeriodsFileName(),
                    getFileNameSuffix() );

            super.rollover();

            //Queue upload the current log file into S3
            //Because we need to wait for the file to be rolled over, use a thread so this doesn't block.
            executor.execute( new UploadQueuer( elapsedPeriodsFileName, lastPeriod ) );
        } else {

            //Upload the active log file without rolling
            s3Client.uploadFileToS3Async( getActiveFileName(), lastPeriod, true );
        }
    }

    public Date getLastPeriod() {

        Date lastPeriod = ((TimeBasedFileNamingAndTriggeringPolicyBase<E>) timeBasedFileNamingAndTriggeringPolicy).dateInCurrentPeriod;

        if (getParentsRawFileProperty() != null) {

            File file = new File( getParentsRawFileProperty() );

            if (file.exists() && file.canRead()) {

                lastPeriod = new Date( file.lastModified() );
            }
        }

        return lastPeriod;
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
            s3Client.uploadFileToS3Async( getActiveFileName(), lastPeriod, true );
        }

        //Shutdown executor
        try {

            executor.shutdown();
            executor.awaitTermination( 10, TimeUnit.MINUTES );
        }
        catch (InterruptedException e) {

            executor.shutdownNow();
        }

        //Wait until finishing the upload
        s3Client.doShutdown();
    }

    private void waitForAsynchronousJobToStop(Future<?> aFuture, String jobDescription) {

        if (aFuture != null) {

            try {

                aFuture.get( CoreConstants.SECONDS_TO_WAIT_FOR_COMPRESSION_JOBS, TimeUnit.SECONDS );
            }
            catch (TimeoutException e) {

                addError("Timeout while waiting for " + jobDescription + " job to finish", e);
            }
            catch (Exception e) {

                addError("Unexpected exception while waiting for " + jobDescription + " job to finish", e);
            }
        }

        lastPeriod = getLastPeriod();
    }

    private String getFileNameSuffix() {

        switch (compressionMode) {

            case GZ:

                return ".gz";

            case ZIP:

                return ".zip";

            case NONE:
            default:

                return "";
        }
    }

    class UploadQueuer implements Runnable {

        private final String elapsedPeriodsFileName;
        private final Date   date;

        public UploadQueuer(final String elapsedPeriodsFileName, final Date date) {

            this.elapsedPeriodsFileName = elapsedPeriodsFileName;
            this.date = date;
        }

        @Override
        public void run() {

            try {

                waitForAsynchronousJobToStop(compressionFuture, "compression");
                waitForAsynchronousJobToStop(cleanUpFuture, "clean-up");
                s3Client.uploadFileToS3Async( elapsedPeriodsFileName, date );
            }
            catch (Exception ex) {

                ex.printStackTrace();
            }
        }
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
