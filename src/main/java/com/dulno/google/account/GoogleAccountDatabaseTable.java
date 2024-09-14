package com.dulno.google.account;

import com.google.common.collect.Lists;
import com.dulno.core.database.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class GoogleAccountDatabaseTable extends DatabaseTable {
  public static GoogleAccountDatabaseTable create(
    DatabaseConnection connection, DatabaseKeyspace keyspace, String tableName
  ) {
    var columns = Lists.<DatabaseColumn>newArrayList();
    columns.add(DatabaseColumn.create("id", DatabaseDataType.TEXT,
      DatabaseColumn.Type.PRIMARY_KEY));
    columns.add(DatabaseColumn.create("refreshToken", DatabaseDataType.TEXT));
    columns.add(DatabaseColumn.create("displayName", DatabaseDataType.TEXT));
    columns.add(DatabaseColumn.create("emailAddress", DatabaseDataType.TEXT));
    columns.add(DatabaseColumn.create("accessToken", DatabaseDataType.TEXT));
    columns.add(DatabaseColumn.create("expirationTime", DatabaseDataType.BIGINT));
    return new GoogleAccountDatabaseTable(connection, keyspace, tableName, columns);
  }

  private GoogleAccountDatabaseTable(
    DatabaseConnection connection, DatabaseKeyspace keyspace, String name,
    List<DatabaseColumn> columns
  ) {
    super(connection, keyspace, name, columns);
  }

  public CompletableFuture<Void> insertAccount(GoogleAccount account) {
    return insertAccount(account.id(), account.refreshToken(), account.displayName(),
      account.emailAddress(), account.accessToken(), account.expirationTime());
  }

  public CompletableFuture<Void> insertAccount(
    String id, String refreshToken, String displayName, String emailAddress,
    String accessToken, long expirationTime
  ) {
    return insert(DatabaseRow.of(id, refreshToken, displayName, emailAddress,
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
    update(id, DatabaseRow.of(id, refreshToken, displayName,
      emailAddress, accessToken, expirationTime));
  }

  public void deleteAccount(String id) {
    delete(id);
  }

  public CompletableFuture<Boolean> accountExists(String id) {
    return exists(id);
  }

  public CompletableFuture<GoogleAccount> findAccount(String id) {
    return selectRow(id).thenApply(GoogleAccount::of);
  }
}