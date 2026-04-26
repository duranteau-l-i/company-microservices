package com.company.companyservice.config;

import com.company.companyservice.security.JwtAuthenticationFilter;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor jwtForwardingInterceptor() {
        return template -> {
            String auth = JwtAuthenticationFilter.CURRENT_JWT.get();
            if (auth != null) {
                template.header("Authorization", auth);
            }
        };
    }
}
