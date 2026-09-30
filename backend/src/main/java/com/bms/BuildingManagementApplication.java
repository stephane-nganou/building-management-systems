package com.bms;

import com.bms.config.FrontendProperties;
import com.bms.identity.KeycloakProperties;
import com.bms.subscription.SubscriptionProperties;
import com.bms.subscription.SupportContacts;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
@EnableConfigurationProperties({KeycloakProperties.class, FrontendProperties.class, SubscriptionProperties.class,
        SupportContacts.class})
public class BuildingManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(BuildingManagementApplication.class, args);
    }
}
