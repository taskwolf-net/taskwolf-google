package net.taskwolf.google.account;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import net.taskwolf.core.database.DatabaseConnection;
import net.taskwolf.core.database.DatabaseKeyspace;

@RequiredArgsConstructor(staticName = "create")
public final class GoogleAccountInjectionModule extends AbstractModule {
  @Provides
  @Singleton
  GoogleAccountDatabaseTable provideGoogleAccountDatabaseTable(
    DatabaseConnection databaseConnection, DatabaseKeyspace databaseKeyspace
  ) {
    var googleAccountDatabaseTable = GoogleAccountDatabaseTable.create(
      databaseConnection, databaseKeyspace);
    googleAccountDatabaseTable.createIfNotExists();
    return googleAccountDatabaseTable;
  }

  @Provides
  @Singleton
  GoogleUserAccountDatabaseTable provideGoogleUserAccountDatabaseTable(
    DatabaseConnection databaseConnection, DatabaseKeyspace databaseKeyspace
  ) {
    var googleUserAccountDatabaseTable = GoogleUserAccountDatabaseTable.create(
      databaseConnection, databaseKeyspace);
    googleUserAccountDatabaseTable.createIfNotExists();
    return googleUserAccountDatabaseTable;
  }
}
