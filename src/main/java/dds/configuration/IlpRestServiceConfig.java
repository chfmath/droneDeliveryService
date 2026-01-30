package dds.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class IlpRestServiceConfig {

    @Bean
    public String ilpServiceUrl(@Value("${ilp.service.url}") String url) {
        return url;
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
