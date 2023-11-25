package net.taskwolf.google;

import jakarta.annotation.PostConstruct;
import net.taskwolf.core.database.DatabaseConnection;
import net.taskwolf.core.database.DatabaseKeyspace;
import net.taskwolf.google.account.GoogleAccountDatabaseTable;
import net.taskwolf.google.account.GoogleUserAccountDatabaseTable;
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
  private GoogleUserAccountDatabaseTable googleUserAccountDatabaseTable;

  @Bean("clientId")
  String provideClientId() {
    return clientId;
  }

  @Bean("clientSecret")
  String provideClientSecret() {
    return clientSecret;
  }

  @Bean
  GoogleAccountDatabaseTable provideGoogleAccountDatabaseTable() {
    return googleAccountDatabaseTable;
  }

  @Bean
  GoogleUserAccountDatabaseTable provideGoogleUserAccountDatabaseTable() {
    return googleUserAccountDatabaseTable;
  }

  @PostConstruct
  private void initialize() throws Exception {
    var configuration = GoogleConfiguration.createAndLoad();
    clientId = configuration.clientId();
    clientSecret = configuration.clientSecret();
    googleAccountDatabaseTable = GoogleAccountDatabaseTable.create(databaseConnection,
      databaseKeyspace);
    googleAccountDatabaseTable.createIfNotExists();
    googleUserAccountDatabaseTable = GoogleUserAccountDatabaseTable.create(databaseConnection,
      databaseKeyspace);
    googleUserAccountDatabaseTable.createIfNotExists();
  }
}
