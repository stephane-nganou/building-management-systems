package com.bms;

import com.bms.identity.KeycloakAdminProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
@EnableConfigurationProperties(KeycloakAdminProperties.class)
public class BuildingManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(BuildingManagementApplication.class, args);
    }
}
