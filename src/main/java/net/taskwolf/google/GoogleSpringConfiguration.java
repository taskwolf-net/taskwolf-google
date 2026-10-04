package net.taskwolf.google;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GoogleSpringConfiguration {
  private String clientId;
  private String clientSecret;

  @Bean("clientId")
  String provideClientId() {
    return clientId;
  }

  @Bean("clientSecret")
  String provideClientSecret() {
    return clientSecret;
  }

  @PostConstruct
  private void initialize() throws Exception {
    var configuration = GoogleConfiguration.createAndLoad();
    clientId = configuration.clientId();
    clientSecret = configuration.clientSecret();
  }
}
