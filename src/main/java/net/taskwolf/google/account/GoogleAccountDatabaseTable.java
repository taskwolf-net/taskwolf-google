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
    columns.add(DatabaseColumn.create("refreshToken", DatabaseDataType.TEXT));
    columns.add(DatabaseColumn.create("displayName", DatabaseDataType.TEXT));
    columns.add(DatabaseColumn.create("emailAddress", DatabaseDataType.TEXT));
    columns.add(DatabaseColumn.create("accessToken", DatabaseDataType.TEXT));
    columns.add(DatabaseColumn.create("expirationTime", DatabaseDataType.BIGINT));
    return new GoogleAccountDatabaseTable(connection, keyspace, TABLE_NAME, columns);
  }

  private GoogleAccountDatabaseTable(
    DatabaseConnection connection, DatabaseKeyspace keyspace, String name,
    List<DatabaseColumn> columns
  ) {
    super(connection, keyspace, name, columns);
  }

  public void insertAccount(GoogleAccount account) {
    insertAccount(account.id(), account.refreshToken(), account.displayName(),
      account.emailAddress(), account.accessToken(), account.expirationTime());
  }

  public void insertAccount(
    String id, String refreshToken, String displayName, String emailAddress,
    String accessToken, long expirationTime
  ) {
    insert(DatabaseRow.of(id, refreshToken, displayName, emailAddress,
      accessToken, expirationTime));
  }

  public void updateAccount(GoogleAccount account) {
    updateAccount(account.id(), account.refreshToken(), account.displayName(),
      account.emailAddress(), account.accessToken(), account.expirationTime());
  }

  private void updateAccount(
    String id, String refreshToken, String displayName, String emailAddress,
    String accessToken, long expirationTime
  ) {
    update(DatabaseCell.create(id), DatabaseRow.of(id, refreshToken, displayName,
      emailAddress, accessToken, expirationTime));
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