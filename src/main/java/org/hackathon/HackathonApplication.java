package org.hackathon;

import org.hackathon.config.GlobalProperties;
import org.hackathon.security.jwt.LocalJwtProperties;
import org.hackathon.security.jwt.SduPassJwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({SduPassJwtProperties.class, LocalJwtProperties.class, GlobalProperties.class})
public class HackathonApplication {

    public static void main(String[] args) {
        SpringApplication.run(HackathonApplication.class, args);
    }

}
