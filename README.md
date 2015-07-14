Logback RollingPolicy with S3 upload
====================================

logback-s3-rolling-policy automatically uploads rolled log files to S3. Each uploaded file will be prefixed by a timestamp formatted as `yyyyMMdd_HHmmss`.

There are 2 rolling policies which can be used:
* `S3FixedWindowRollingPolicy`
* `S3TimeBasedRollingPolicy`

logback-s3-rolling-policy was forked from logback-s3 (https://github.com/shuwada/logback-s3) but transfered into a new project because changes were getting too big.

Configuration
-------------

### logback.xml variables

Whether you implement one of any available S3 policies, the following extra variables (on top of Logback's) are mandatory:

* `awsAccessKey` Your AWS access key.
* `awsSecretKey` Your AWS secret key.
* `s3BucketName` The S3 bucket name to upload your log files to.

There are few optional variables:

* `s3FolderName` The S3 folder name in your S3 bucket to put the log files in.
* `shutdownHookType` Defines which type of shutdown hook you want to use. This variable is mandatory when you user `rolloverOnExit`. Defaults to `NONE`. Possible values are:
  * `NONE` This will not add a shutdown hook. Please note that your most up to date log file won't be uploaded to S3!
  * `JVM_SHUTDOWN_HOOK` This will add a runtime shutdown hook. If you're using a webapplication, please use the `SERVLET_CONTEXT`, as the JVM shutdown hook is not really safe to use here.
  * `SERVLET_CONTEXT` This will register a shutdown hook to the context destroyed method of `RollingPolicyContextListener`. Don't forget to actually add the context listener to you `web.xml`. (see below)
* `rolloverOnExit` Whether to rollover when your application is being shut down or not. Boolean value, defaults to `false`. If this is set to `false`, and you have defined a `shutdownHookType`, then the log file will be uploaded as is.

### web.xml

If you're using the shutdown hook `SERVLET_CONTEXT` as defined above, you'll need to add the context listener class to your `web.xml`:

```xml
<listener>
   <listener-class>ch.qos.logback.core.rolling.shutdown.RollingPolicyContextListener</listener-class>
</listener>
```

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
    <s3FolderName>log</s3FolderName>
    <rolloverOnExit>true</rolloverOnExit>
    <shutdownHookType>SERVLET_CONTEXT</shutdownHookType>
  </rollingPolicy>
  <triggeringPolicy class="ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy">
    <maxFileSize>10MB</maxFileSize>
  </triggeringPolicy>
</appender>
```
An uploaded

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
    <timeBasedFileNamingAndTriggeringPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP">
      <maxFileSize>10MB</maxFileSize>
    </timeBasedFileNamingAndTriggeringPolicy>
  </rollingPolicy>
</appender>
```

### AWS Credentials

It is a good idea to create an IAM user only allowed to upload S3 object to a specific S3 bucket.
It improves the control and reduces the risk of unauthorized access to your S3 bucket.

The following is an example IAM policy.
```
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
