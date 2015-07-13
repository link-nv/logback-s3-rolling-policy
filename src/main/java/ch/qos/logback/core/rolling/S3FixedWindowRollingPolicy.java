package ch.qos.logback.core.rolling;


import ch.qos.logback.core.rolling.shutdown.RollingPolicyContextListener;
import ch.qos.logback.core.rolling.shutdown.RollingPolicyJVMListener;
import ch.qos.logback.core.rolling.shutdown.RollingPolicyShutdownListener;
import ch.qos.logback.core.rolling.shutdown.ShutdownHookType;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class S3FixedWindowRollingPolicy extends FixedWindowRollingPolicy implements RollingPolicyShutdownListener {

    private String awsAccessKey;
    private String awsSecretKey;
    private String s3BucketName;
    private String s3FolderName;
    private boolean rolloverOnExit;
    private ShutdownHookType shutdownHookType;

    private ExecutorService executor;
    private AmazonS3Client s3Client;

    public S3FixedWindowRollingPolicy() {

        super();

        executor = Executors.newFixedThreadPool( 1 );
        rolloverOnExit = true;
        shutdownHookType = ShutdownHookType.NONE;
    }

    protected AmazonS3Client getS3Client() {

        if( s3Client == null ) {

            AWSCredentials cred = new BasicAWSCredentials( getAwsAccessKey(), getAwsSecretKey() );
            s3Client = new AmazonS3Client( cred );
        }

        return s3Client;
    }

    @Override
    public void start() {

        super.start();

        //Register shutdown hook so the log gets uploaded on shutdown, if needed
        switch( shutdownHookType ) {

            case SERVLET_CONTEXT:

                RollingPolicyContextListener.registerShutdownListener( this );
                break;

            case JVM_SHUTDOWN_HOOK:

                Runtime.getRuntime().addShutdownHook( new Thread( new RollingPolicyJVMListener( this ) ) );
                break;

            case NONE:
            default:

                //Do nothing
                break;
        }
    }

    @Override
    public void rollover()
            throws RolloverFailure {

        super.rollover();

        //Upload the current log file into S3
        uploadFileToS3Async( fileNamePattern.convertInt( getMinIndex() ) );
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

    protected void uploadFileToS3Async( final String filename ) {

        final File file = new File( filename );

        //If file does not exist or if empty, do nothing
        if( !file.exists() || file.length() == 0 ) {

            return;
        }

        //Build S3 path
        final StringBuffer s3ObjectName = new StringBuffer();
        if( getS3FolderName() != null ) {

            s3ObjectName.append( getS3FolderName() ).append( "/" );
        }

        s3ObjectName.append( new SimpleDateFormat( "yyyyMMdd_HHmmss" ).format( new Date() ) ).append( "_" );
        s3ObjectName.append( file.getName() );

        //Queue thread to upload
        Runnable uploader = new Runnable() {

            @Override
            public void run() {

                try {

                    getS3Client().putObject( getS3BucketName(), s3ObjectName.toString(), file );
                }
                catch( Exception ex ) {

                    ex.printStackTrace();
                }
            }
        };

        executor.execute( uploader );
    }

    public String getAwsAccessKey() {

        return awsAccessKey;
    }

    public void setAwsAccessKey( String awsAccessKey ) {

        this.awsAccessKey = awsAccessKey;
    }

    public String getAwsSecretKey() {

        return awsSecretKey;
    }

    public void setAwsSecretKey( String awsSecretKey ) {

        this.awsSecretKey = awsSecretKey;
    }

    public String getS3BucketName() {

        return s3BucketName;
    }

    public void setS3BucketName( String s3BucketName ) {

        this.s3BucketName = s3BucketName;
    }

    public String getS3FolderName() {

        return s3FolderName;
    }

    public void setS3FolderName( String s3FolderName ) {

        this.s3FolderName = s3FolderName;
    }

    public boolean isRolloverOnExit() {

        return rolloverOnExit;
    }

    public void setRolloverOnExit( boolean rolloverOnExit ) {

        this.rolloverOnExit = rolloverOnExit;
    }

    public ShutdownHookType getShutdownHookType() {

        return shutdownHookType;
    }

    public void setShutdownHookType( ShutdownHookType shutdownHookType ) {

        this.shutdownHookType = shutdownHookType;
    }
}
