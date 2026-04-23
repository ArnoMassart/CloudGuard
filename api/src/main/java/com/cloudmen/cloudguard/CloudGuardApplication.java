package com.cloudmen.cloudguard;

import com.cloudmen.cloudguard.configuration.NotificationProjectionProperties;
import com.cloudmen.cloudguard.configuration.NotificationReminderProperties;
import com.cloudmen.cloudguard.configuration.SolvedNotificationCleanupProperties;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({
    NotificationProjectionProperties.class,
    NotificationReminderProperties.class,
    SolvedNotificationCleanupProperties.class
})
public class CloudGuardApplication {
    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        dotenv.entries().forEach(dotenvEntry -> System.setProperty(dotenvEntry.getKey(), dotenvEntry.getValue()));

        SpringApplication.run(CloudGuardApplication.class, args);
    }
}