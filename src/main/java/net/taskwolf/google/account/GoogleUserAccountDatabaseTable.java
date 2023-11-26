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

  public void addAccount(UUID userId, String accountId) {
    exists(DatabaseCell.create(userId)).thenAccept(exists ->
      addAccount(userId, accountId, exists));
  }

  private void addAccount(UUID userId, String accountId, boolean exists) {
    if (!exists) {
      insertAccount(userId, accountId);
      return;
    }
    selectRow(DatabaseCell.create(userId)).thenAccept(row ->
      addAccount(userId, accountId, row));
  }

  private void addAccount(UUID userId, String accountId, DatabaseRow row) {
    var accountIds = row.findCell(1).<String>listValue();
    if (accountIds.contains(accountId)) {
      return;
    }
    accountIds.add(accountId);
    updateAccounts(userId, accountIds);
  }

  private void insertAccount(UUID userId, String accountId) {
    insert(DatabaseRow.of(userId, Lists.newArrayList(accountId)));
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

  public void updateAccounts(UUID userId, List<String> accountIds) {
    update(DatabaseCell.create(userId), DatabaseRow.of(userId, accountIds));
  }

  public void deleteAccounts(UUID userId) {
    delete(DatabaseCell.create(userId));
  }

  public CompletableFuture<Boolean> accountExists(UUID userId) {
    return exists(DatabaseCell.create(userId));
  }

  public CompletableFuture<List<String>> findAccounts(UUID userId) {
    return selectRow(DatabaseCell.create(userId))
      .thenApply(row -> row.findCell(1).listValue());
  }
}