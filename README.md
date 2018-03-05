Logback RollingPolicy with S3 upload
====================================

logback-s3-rolling-policy automatically uploads rolled log files to S3.

There are 2 rolling policies which can be used:
* `S3FixedWindowRollingPolicy`
* `S3TimeBasedRollingPolicy`

logback-s3-rolling-policy was forked from logback-s3 (https://github.com/shuwada/logback-s3) but transfered into a new project because changes were getting too big.

Index
-----

* [Requirements](#requirements) 
* [Usage](#usage) 
* [Configuration](#configuration) 
  * [logback.xml variables](#logbackxml-variables) 
  * [web.xml](#webxml) 
  * [logback.xml rolling policy examples](#logbackxml-rolling-policy-examples) 
* [AWS Credentials](#aws-credentials) 
* [Libraries](#libraries) 

Requirements
------------

* Java 1.7+

Configuration
-------------

### logback.xml variables

Whether you implement one of any available S3 policies, the following extra variables (on top of Logback's) can be used:

* `s3BucketName` The S3 bucket name to upload your log files to (mandatory).
* `awsAccessKey` Your AWS access key. If not provided it falls back to the AWS SDK default provider chain.
* `awsSecretKey` Your AWS secret key. If not provided it falls back to the AWS SDK default provider chain.
* `s3FolderName` The S3 folder name in your S3 bucket to put the log files in. This variable supports dates, just put your [pattern](https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html) between `%d{}`. Example: `%d{yyyy/MM/dd}`.
* `shutdownHookType` Defines which type of shutdown hook you want to use. This variable is mandatory when you use `rolloverOnExit`. Defaults to `NONE`. Possible values are:
  * `NONE` This will not add a shutdown hook. Please note that your most up to date log file won't be uploaded to S3!
  * `JVM_SHUTDOWN_HOOK` This will add a runtime shutdown hook. If you're using a webapplication, please use the `SERVLET_CONTEXT`, as the JVM shutdown hook is not really safe to use here.
  * `SERVLET_CONTEXT` This will register a shutdown hook to the context destroyed method of `RollingPolicyContextListener`. Don't forget to actually add the context listener to you `web.xml`. (see below)
* `rolloverOnExit` Whether to rollover when your application is being shut down or not. Boolean value, defaults to `false`. If this is set to `false`, and you have defined a `shutdownHookType`, then the log file will be uploaded as is.
* `prefixTimestamp` Whether to prefix the uploaded filename with a timestamp formatted as `yyyyMMdd_HHmmss` or not. Boolean value, defaults to `false`.
* `prefixIdentifier` Whether to prefix the uploaded filename with an identifier or not. Boolean value, defaults to `false`. If running on an AWS EC2 instance, the instance ID will be used. If not running on an AWS EC2 instance, the hostname address will be used. If the hostname address can't be used, a UUID will be used. 

### web.xml

If you're using the shutdown hook `SERVLET_CONTEXT` as defined above, you'll need to add the context listener class to your `web.xml`:

```xml
<listener>
   <listener-class>ch.qos.logback.core.rolling.shutdown.RollingPolicyContextListener</listener-class>
</listener>
```

### Run-time variables

As of version `1.3` you can set run-time variables. For now you can only add an extra S3 folder.

Just use `CustomData.extraS3Folder.set( "extra_folder_name" );` somewhere in your code before the upload occurs. You can always change this value during run-time and it will be picked up on the next upload. set to `null` to ignore.

### logback.xml rolling policy examples

An example `logback.xml` appender for each available policy using `RollingFileAppender`.

* `ch.qos.logback.core.rolling.S3FixedWindowRollingPolicy`:  
```xml
<appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
  <file>logs/myapp.log</file>
  <encoder>
    <pattern>[%d] %-8relative %22c{0} [%-5level] %msg%xEx{3}%n</pattern>
  </encoder>
  <rollingPolicy class="ch.qos.logback.core.rolling.S3FixedWindowRollingPolicy">
    <fileNamePattern>logs/myapp.%i.log.gz</fileNamePattern>
    <awsAccessKey>ACCESS_KEY</awsAccessKey>
    <awsSecretKey>SECRET_KEY</awsSecretKey>
    <s3BucketName>myapp-logging</s3BucketName>
    <s3FolderName>logs/%d{yyyy/MM/dd}</s3FolderName>
    <rolloverOnExit>true</rolloverOnExit>
    <shutdownHookType>SERVLET_CONTEXT</shutdownHookType>
    <prefixTimestamp>true</prefixTimestamp>
    <prefixIdentifier>true</prefixIdentifier>
  </rollingPolicy>
  <triggeringPolicy class="ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy">
    <maxFileSize>10MB</maxFileSize>
  </triggeringPolicy>
</appender>
```
In this example you'll find the logs at `myapp-logging/logs/2015/08/18/`.

* `ch.qos.logback.core.rolling.S3TimeBasedRollingPolicy`:  
```xml
<appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
  <file>logs/myapp.log</file>
  <encoder>
    <pattern>[%d] %-8relative %22c{0} [%-5level] %msg%xEx{3}%n</pattern>
  </encoder>
  <rollingPolicy class="ch.qos.logback.core.rolling.S3TimeBasedRollingPolicy">
    <!-- Rollover every minute -->
    <fileNamePattern>logs/myapp.%d{yyyy-MM-dd_HH-mm}.%i.log.gz</fileNamePattern>
    <awsAccessKey>ACCESS_KEY</awsAccessKey>
    <awsSecretKey>SECRET_KEY</awsSecretKey>
    <s3BucketName>myapp-logging</s3BucketName>
    <s3FolderName>log</s3FolderName>
    <rolloverOnExit>true</rolloverOnExit>
    <shutdownHookType>SERVLET_CONTEXT</shutdownHookType>
    <prefixTimestamp>false</prefixTimestamp>
    <prefixIdentifier>true</prefixIdentifier>
    <timeBasedFileNamingAndTriggeringPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP">
      <maxFileSize>10MB</maxFileSize>
    </timeBasedFileNamingAndTriggeringPolicy>
  </rollingPolicy>
</appender>
```
In this example you'll find the logs at `myapp-logging/log/`.

### AWS Credentials

It is a good idea to create an IAM user only allowed to upload S3 object to a specific S3 bucket.
It improves the control and reduces the risk of unauthorized access to your S3 bucket.

The following is an example IAM policy.
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": [
        "s3:PutObject"
      ],
      "Sid": "Stmt1378251801000",
      "Resource": [
        "arn:aws:s3:::myapp-logging/log/*"
      ],
      "Effect": "Allow"
    }
  ]
}
```

Libraries
---------

This project uses the following libraries:
* `com.amazonaws:aws-java-sdk:1.11.7`
* `ch.qos.logback:logback-classic:1.2.3`
* `com.google.guava:guava:18.0`
* `javax.servlet:servlet-api:2.4` (scope provided)
* `org.jetbrains:annotations:15.0` (scope provided)
