# Configuring Logging

The cashu-client library produces logs using [slf4j](https://www.slf4j.org/). Here is an example of a configuration file for logback that will send logs to a file called `cashuwallet.log` in the current directory. Place this file in `src/main/resources` and it will be picked up by the logback library.

```xml
 <configuration>

    <!-- File appender -->
    <appender name="FILE" class="ch.qos.logback.core.FileAppender">
        <file>cashuwallet.log</file>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <root level="DEBUG">
        <appender-ref ref="FILE"/>
    </root>

</configuration>
```
