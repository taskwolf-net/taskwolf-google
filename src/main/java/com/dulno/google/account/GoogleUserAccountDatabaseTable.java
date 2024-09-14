package com.dulno.google.account;

import com.google.common.collect.Lists;
import com.dulno.core.database.*;
import com.dulno.core.database.condition.DatabaseComparison;
import com.dulno.core.database.condition.DatabaseCondition;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class GoogleUserAccountDatabaseTable extends DatabaseTable {
  public static GoogleUserAccountDatabaseTable create(
    DatabaseConnection connection, DatabaseKeyspace keyspace, String tableName
  ) {
    var columns = Lists.<DatabaseColumn>newArrayList();
    columns.add(DatabaseColumn.create("user", DatabaseDataType.UUID,
      DatabaseColumn.Type.PRIMARY_KEY));
    columns.add(DatabaseListColumn.create("accounts", DatabaseDataType.TEXT));
    return new GoogleUserAccountDatabaseTable(connection, keyspace, tableName, columns);
  }

  private GoogleUserAccountDatabaseTable(
    DatabaseConnection connection, DatabaseKeyspace keyspace, String name,
    List<DatabaseColumn> columns
  ) {
    super(connection, keyspace, name, columns);
  }

  public CompletableFuture<Void> addAccount(UUID userId, String accountId) {
    var futureResponse = new CompletableFuture<Void>();
    exists(userId).thenAccept(exists -> addAccount(userId, accountId, exists)
      .thenAccept(futureResponse::complete));
    return futureResponse;
  }

  private CompletableFuture<Void> addAccount(
    UUID userId, String accountId, boolean exists
  ) {
    if (!exists) {
      return insertAccount(userId, accountId);
    }
    var futureResponse = new CompletableFuture<Void>();
    selectRow(userId).thenAccept(row -> addAccount(userId, accountId, row)
      .thenAccept(futureResponse::complete));
    return futureResponse;
  }

  private CompletableFuture<Void> addAccount(
    UUID userId, String accountId, DatabaseRow row
  ) {
    var accountIds = row.findCell(1).<String>listValue();
    if (accountIds.contains(accountId)) {
      return CompletableFuture.completedFuture(null);
    }
    accountIds.add(accountId);
    return updateAccounts(userId, accountIds);
  }

  private CompletableFuture<Void> insertAccount(UUID userId, String accountId) {
    return insert(DatabaseRow.of(userId, Lists.newArrayList(accountId)));
  }

  public void removeAccount(UUID userId, String accountId) {
    selectRow(userId).thenAccept(row ->
      removeAccount(userId, accountId, row));
  }

  private void removeAccount(UUID userId, String accountId, DatabaseRow row) {
    var accountIds = row.findCell(1).<String>listValue();
    if (accountIds.size() == 1) {
      deleteAccounts(userId);
      return;
    }
    accountIds.remove(accountId);
    updateAccounts(userId, accountIds);
  }

  public CompletableFuture<Void> updateAccounts(UUID userId, List<String> accountIds) {
    return update(userId, DatabaseRow.of(userId, accountIds));
  }

  public void deleteAccounts(UUID userId) {
    delete(userId);
  }

  public CompletableFuture<Boolean> accountExists(UUID userId) {
    return exists(userId);
  }

  public CompletableFuture<Integer> accountOccurNumber(String account) {
    return selectRows(DatabaseCondition.of(DatabaseComparison.create("accounts",
      account, DatabaseComparison.Type.CONTAINS))).thenApply(List::size);
  }

  public CompletableFuture<List<String>> findAccountsIfExists(UUID userId) {
    var futureResponse = new CompletableFuture<List<String>>();
    accountExists(userId).thenAccept(exists -> findAccountsIfExists(userId, exists)
      .thenAccept(futureResponse::complete));
    return futureResponse;
  }

  private CompletableFuture<List<String>> findAccountsIfExists(
    UUID userId, boolean exists
  ) {
    if (!exists) {
      return CompletableFuture.completedFuture(Lists.newArrayList());
    }
    return findAccounts(userId);
  }

  public CompletableFuture<List<String>> findAccounts(UUID userId) {
    return selectRow(userId)
      .thenApply(row -> row.findCell(1).listValue());
  }
}