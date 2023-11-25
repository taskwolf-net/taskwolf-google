package net.taskwolf.google.account;

import com.google.common.collect.Lists;
import net.taskwolf.core.database.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class GoogleAccountDatabaseTable extends DatabaseTable {
  private static final String TABLE_NAME = "google_account";

  public static GoogleAccountDatabaseTable create(
    DatabaseConnection connection, DatabaseKeyspace keyspace
  ) {
    var columns = Lists.<DatabaseColumn>newArrayList();
    columns.add(DatabaseColumn.create("id", DatabaseDataType.TEXT,
      DatabaseColumn.Type.PRIMARY_KEY));
    columns.add(DatabaseColumn.create("accessToken", DatabaseDataType.TEXT));
    columns.add(DatabaseColumn.create("refreshToken", DatabaseDataType.TEXT));
    columns.add(DatabaseColumn.create("expirationTime", DatabaseDataType.BIGINT));
    columns.add(DatabaseColumn.create("displayName", DatabaseDataType.TEXT));
    columns.add(DatabaseColumn.create("emailAddress", DatabaseDataType.TEXT));
    return new GoogleAccountDatabaseTable(connection, keyspace, TABLE_NAME, columns);
  }

  private GoogleAccountDatabaseTable(
    DatabaseConnection connection, DatabaseKeyspace keyspace, String name,
    List<DatabaseColumn> columns
  ) {
    super(connection, keyspace, name, columns);
  }

  public void insertAccount(GoogleAccount account) {
    insertAccount(account.id(), account.accessToken(), account.refreshToken(),
      account.expirationTime(), account.displayName(), account.emailAddress());
  }

  public void insertAccount(
    String id, String accessToken, String refreshToken, long expirationDuration,
    String displayName, String emailAddress
  ) {
    insert(DatabaseRow.of(id, accessToken, refreshToken, expirationDuration,
      displayName, emailAddress));
  }

  public void deleteAccount(String id) {
    delete(DatabaseCell.create(id));
  }

  public CompletableFuture<Boolean> accountExists(String id) {
    return exists(DatabaseCell.create(id));
  }

  public CompletableFuture<GoogleAccount> findAccount(String id) {
    return selectRow(DatabaseCell.create(id)).thenApply(GoogleAccount::of);
  }
}