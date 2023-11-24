package net.taskwolf.google;

import com.google.inject.name.Named;
import jakarta.annotation.PostConstruct;
import net.taskwolf.core.database.DatabaseConnection;
import net.taskwolf.core.database.DatabaseKeyspace;
import net.taskwolf.google.account.GoogleAccountDatabaseTable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GoogleSpringConfiguration {
  @Autowired
  private DatabaseConnection databaseConnection;
  @Autowired
  private DatabaseKeyspace databaseKeyspace;
  private String clientId;
  private String clientSecret;
  private GoogleAccountDatabaseTable googleAccountDatabaseTable;

  @Bean
  @Named("clientId")
  String provideClientId() {
    return clientId;
  }

  @Bean
  @Named("clientSecret")
  String provideClientSecret() {
    return clientSecret;
  }

  @Bean
  GoogleAccountDatabaseTable provideGoogleAccountDatabaseTable() {
    return googleAccountDatabaseTable;
  }

  @PostConstruct
  private void initialize() throws Exception {
    var configuration = GoogleConfiguration.createAndLoad();
    clientId = configuration.clientId();
    clientSecret = configuration.clientSecret();
    googleAccountDatabaseTable = GoogleAccountDatabaseTable.create(databaseConnection,
      databaseKeyspace);
    googleAccountDatabaseTable.createIfNotExists();
  }
}
