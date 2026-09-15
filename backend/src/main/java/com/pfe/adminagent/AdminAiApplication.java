package com.pfe.adminagent;

import com.pfe.adminagent.config.SecurityProperties;
import com.pfe.adminagent.config.AiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Entry point for the AdminAI backend — the AI Administrative Agent API.
 */
@SpringBootApplication
@EnableConfigurationProperties({SecurityProperties.class, AiProperties.class})
public class AdminAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminAiApplication.class, args);
    }
}
