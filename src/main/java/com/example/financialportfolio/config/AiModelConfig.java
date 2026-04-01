package com.example.financialportfolio.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AiModelConfig {

    @Bean
    @Qualifier("aiRestTemplate")
    public RestTemplate aiRestTemplate(AiProperties aiProperties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(aiProperties.getConnectTimeout());
        factory.setReadTimeout(aiProperties.getReadTimeout());
        return new RestTemplate(factory);
    }
}