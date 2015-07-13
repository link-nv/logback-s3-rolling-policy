package ch.qos.logback.core.rolling;


import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import ch.qos.logback.core.rolling.shutdown.RollingPolicyShutdownListener;
import ch.qos.logback.core.rolling.shutdown.servlet.RollingPolicyContextListener;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;

public class S3FixedWindowRollingPolicy extends FixedWindowRollingPolicy implements RollingPolicyShutdownListener {

    ExecutorService executor = Executors.newFixedThreadPool(1);

    String awsAccessKey;
    String awsSecretKey;
    String s3BucketName;
    String s3FolderName;

    private boolean rolloverOnExit = true;

    AmazonS3Client s3Client;

    protected AmazonS3Client getS3Client() {
        if (s3Client == null) {
            AWSCredentials cred = new BasicAWSCredentials(getAwsAccessKey(), getAwsSecretKey());
            s3Client = new AmazonS3Client(cred);
        }
        return s3Client;
    }

    @Override
    public void start() {
        super.start();

        //Register so the log gets uploaded on shutdown
        RollingPolicyContextListener.registerShutdownListener( this );
    }

    @Override
    public void rollover() throws RolloverFailure {
        super.rollover();

        // upload the current log file into S3
        String rolledLogFileName = fileNamePattern.convertInt(getMinIndex());
        uploadFileToS3Async(rolledLogFileName);
    }

    protected void uploadFileToS3Async(String filename) {
        final File file = new File(filename);

        // if file does not exist or empty, do nothing
        if (!file.exists() || file.length() == 0) {
            return;
        }

        // add the S3 folder name in front if specified
        final StringBuffer s3ObjectName = new StringBuffer();
        if (getS3FolderName() != null) {
            s3ObjectName.append(getS3FolderName()).append("/");
        }
        s3ObjectName.append(file.getName());

        addInfo("Uploading " + filename);
        Runnable uploader = new Runnable() {
            @Override
            public void run() {
                try {
                    getS3Client().putObject(getS3BucketName(), s3ObjectName.toString(), file);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
        executor.execute(uploader);
    }

    /**
     * Shutdown hook that gets called when exiting the application.
     */
    @Override
    public void doShutdown() {

        try {

            if( isRolloverOnExit() ) {

                //Do rolling and upload the rolled file on exit
                rollover();
            }
            else {

                //Upload the active log file without rolling
                uploadFileToS3Async( getActiveFileName() );
            }

            //Wait until finishing the upload
            executor.shutdown();
            executor.awaitTermination( 10, TimeUnit.MINUTES );
        }
        catch( Exception ex ) {

            executor.shutdownNow();
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

    public void setRolloverOnExit( boolean rolloverOnExit ) {

        this.rolloverOnExit = rolloverOnExit;
    }
}
