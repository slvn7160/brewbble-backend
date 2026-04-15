package com.brewbble.config;

import com.squareup.square.SquareClient;
import com.squareup.square.SquareClientBuilder;
import com.squareup.square.core.Environment;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "square")
@Data
public class SquareConfig {

    private String accessToken;
    private String locationId;
    private String environment = "SANDBOX";
    private String webhookSignatureKey;
    private String webhookUrl;

    @Bean
    public SquareClient squareClient() {
        Environment env = "PRODUCTION".equalsIgnoreCase(environment)
                ? Environment.PRODUCTION
                : Environment.SANDBOX;

        return new SquareClientBuilder()
                .token(accessToken)
                .environment(env)
                .build();
    }
}
