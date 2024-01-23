package net.taskwolf.google.account;

import com.google.common.collect.Lists;
import net.taskwolf.core.database.*;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class GoogleUserAccountDatabaseTable extends DatabaseTable {
  private static final String TABLE_NAME = "google_user_account";

  public static GoogleUserAccountDatabaseTable create(
    DatabaseConnection connection, DatabaseKeyspace keyspace
  ) {
    var columns = Lists.<DatabaseColumn>newArrayList();
    columns.add(DatabaseColumn.create("user", DatabaseDataType.UUID,
      DatabaseColumn.Type.PRIMARY_KEY));
    columns.add(DatabaseListColumn.create("accounts", DatabaseDataType.TEXT));
    return new GoogleUserAccountDatabaseTable(connection, keyspace, TABLE_NAME, columns);
  }

  private GoogleUserAccountDatabaseTable(
    DatabaseConnection connection, DatabaseKeyspace keyspace, String name,
    List<DatabaseColumn> columns
  ) {
    super(connection, keyspace, name, columns);
  }

  public CompletableFuture<Void> addAccount(UUID userId, String accountId) {
    var futureResponse = new CompletableFuture<Void>();
    exists(DatabaseCell.create(userId)).thenAccept(exists ->
      addAccount(userId, accountId, exists).thenAccept(futureResponse::complete));
    return futureResponse;
  }

  private CompletableFuture<Void> addAccount(UUID userId, String accountId, boolean exists) {
    if (!exists) {
      return insertAccount(userId, accountId);
    }
    var futureResponse = new CompletableFuture<Void>();
    selectRow(DatabaseCell.create(userId)).thenAccept(row ->
      addAccount(userId, accountId, row).thenAccept(futureResponse::complete));
    return futureResponse;
  }

  private CompletableFuture<Void> addAccount(UUID userId, String accountId, DatabaseRow row) {
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
    selectRow(DatabaseCell.create(userId)).thenAccept(row ->
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
    return update(DatabaseCell.create(userId), DatabaseRow.of(userId, accountIds));
  }

  public void deleteAccounts(UUID userId) {
    delete(DatabaseCell.create(userId));
  }

  public CompletableFuture<Boolean> accountExists(UUID userId) {
    return exists(DatabaseCell.create(userId));
  }

  public CompletableFuture<Integer> accountOccurNumber(String account) {
    return selectRows("accounts CONTAINS '" + account + "'")
      .thenApply(List::size);
  }

  public CompletableFuture<List<String>> findAccounts(UUID userId) {
    return selectRow(DatabaseCell.create(userId))
      .thenApply(row -> row.findCell(1).listValue());
  }
}