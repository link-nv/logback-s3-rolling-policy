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

package ch.qos.logback.core.rolling.aws;

import ch.qos.logback.core.rolling.data.CustomData;
import ch.qos.logback.core.rolling.shutdown.RollingPolicyShutdownListener;
import ch.qos.logback.core.rolling.util.IdentifierUtil;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AmazonS3ClientImpl implements RollingPolicyShutdownListener {

    private final String awsAccessKey;
    private final String awsSecretKey;
    private final String s3BucketName;
    private final String s3FolderName;

    private final boolean prefixTimestamp;
    private final boolean prefixIdentifier;

    private final String identifier;

    private AmazonS3Client amazonS3Client;
    private ExecutorService executor;

    public AmazonS3ClientImpl(String awsAccessKey, String awsSecretKey, String s3BucketName, String s3FolderName, boolean prefixTimestamp,
                              boolean prefixIdentifier) {

        this.awsAccessKey = awsAccessKey;
        this.awsSecretKey = awsSecretKey;
        this.s3BucketName = s3BucketName;
        this.s3FolderName = s3FolderName;

        this.prefixTimestamp = prefixTimestamp;
        this.prefixIdentifier = prefixIdentifier;

        executor = Executors.newFixedThreadPool( 1 );
        amazonS3Client = null;

        identifier = prefixIdentifier? IdentifierUtil.getIdentifier(): null;
    }

    public void uploadFileToS3Async(final String filename, final Date date) {

        uploadFileToS3Async( filename, date, false );
    }

    public void uploadFileToS3Async(final String filename, final Date date, final boolean overrideTimestampSetting) {

        if (amazonS3Client == null) {

            // If the access and secret key is not specified then try to use other providers
            if (getAwsAccessKey() == null || getAwsAccessKey().trim().isEmpty()) {

                amazonS3Client = new AmazonS3Client();
            } else {

                AWSCredentials cred = new BasicAWSCredentials( getAwsAccessKey(), getAwsSecretKey() );
                amazonS3Client = new AmazonS3Client( cred );
            }
        }

        final File file = new File( filename );

        //If file does not exist or if empty, do nothing
        if (!file.exists() || file.length() == 0) {

            return;
        }

        //Build S3 path
        final StringBuffer s3ObjectName = new StringBuffer();
        if (getS3FolderName() != null) {

            s3ObjectName.append( format( getS3FolderName(), date ) ).append( "/" );
        }

        //Extra custom S3 (runtime) folder?
        if (CustomData.extraS3Folder.get() != null) {

            s3ObjectName.append( CustomData.extraS3Folder.get() ).append( "/" );
        }

        //Add timestamp prefix if desired
        if (prefixTimestamp || overrideTimestampSetting) {

            s3ObjectName.append( new SimpleDateFormat( "yyyyMMdd_HHmmss" ).format( date ) ).append( "_" );
        }

        //Add identifier prefix if desired
        if (prefixIdentifier) {

            s3ObjectName.append( identifier ).append( "_" );
        }

        s3ObjectName.append( file.getName() );

        //Queue thread to upload
        Runnable uploader = new Runnable() {

            @Override
            public void run() {

                try {

                    amazonS3Client.putObject(
                            new PutObjectRequest(getS3BucketName(), s3ObjectName.toString(), file)
                                    .withCannedAcl(CannedAccessControlList.BucketOwnerFullControl));
                }
                catch (Exception ex) {

                    ex.printStackTrace();
                }
            }
        };

        executor.execute( uploader );
    }

    /**
     * Shutdown hook that gets called when exiting the application.
     */
    @Override
    public void doShutdown() {

        try {

            //Wait until finishing the upload
            executor.shutdown();
            executor.awaitTermination( 10, TimeUnit.MINUTES );
        }
        catch (InterruptedException e) {

            executor.shutdownNow();
        }
    }

    private String format(String s, Date date) {

        Pattern pattern = Pattern.compile( "%d\\{(.*?)\\}" );
        Matcher matcher = pattern.matcher( s );

        while (matcher.find()) {

            String match = matcher.group( 1 );

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat( match );
            String replace = simpleDateFormat.format( date );

            s = s.replace( String.format( "%%d{%s}", match ), replace );
        }

        return s;
    }

    public String getAwsAccessKey() {

        return awsAccessKey;
    }

    public String getAwsSecretKey() {

        return awsSecretKey;
    }

    public String getS3BucketName() {

        return s3BucketName;
    }

    public String getS3FolderName() {

        return s3FolderName;
    }

    public boolean isPrefixTimestamp() {

        return prefixTimestamp;
    }

    public boolean isPrefixIdentifier() {

        return prefixIdentifier;
    }

    public String getIdentifier() {

        return identifier;
    }
}
