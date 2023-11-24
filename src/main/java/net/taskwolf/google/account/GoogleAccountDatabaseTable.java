package net.taskwolf.google.account;

import com.google.common.collect.Lists;
import net.taskwolf.core.database.*;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class GoogleAccountDatabaseTable extends DatabaseTable {
  private static final String TABLE_NAME = "google_account";

  public static GoogleAccountDatabaseTable create(
    DatabaseConnection connection, DatabaseKeyspace keyspace
  ) {
    var columns = Lists.<DatabaseColumn>newArrayList();
    columns.add(DatabaseColumn.create("user", DatabaseDataType.UUID,
      DatabaseColumn.Type.PRIMARY_KEY));
    columns.add(DatabaseListColumn.create("accounts", DatabaseDataType.TEXT));
    return new GoogleAccountDatabaseTable(connection, keyspace, TABLE_NAME, columns);
  }

  private GoogleAccountDatabaseTable(
    DatabaseConnection connection, DatabaseKeyspace keyspace, String name,
    List<DatabaseColumn> columns
  ) {
    super(connection, keyspace, name, columns);
  }

  public void addAccount(UUID userId, GoogleAccount account) {
    exists(DatabaseCell.create(userId)).thenAccept(exists ->
      addAccount(userId, account, exists));
  }

  private void addAccount(UUID userId, GoogleAccount account, boolean exists) {
    if (!exists) {
      insertAccount(userId, account);
      return;
    }
    selectRow(DatabaseCell.create(userId)).thenAccept(row ->
      addAccount(userId, account, row));
  }

  private void addAccount(UUID userId, GoogleAccount account, DatabaseRow row) {
    var accounts = row.findCell(1).<String>listValue();
    accounts.add(account.encode());
    updateAccounts(userId, accounts);
  }

  private void insertAccount(UUID userId, GoogleAccount account) {
    insert(DatabaseRow.of(userId, Lists.newArrayList(account.encode())));
  }

  public void removeAccount(UUID userId, GoogleAccount account) {
    selectRow(DatabaseCell.create(userId)).thenAccept(row ->
      removeAccount(userId, account, row));
  }

  private void removeAccount(UUID userId, GoogleAccount account, DatabaseRow row) {
    var accounts = row.findCell(1).<String>listValue();
    if (accounts.size() == 1) {
      deleteAccounts(userId);
      return;
    }
    accounts.remove(account.encode());
    updateAccounts(userId, accounts);
  }

  public void updateAccounts(UUID userId, List<String> accounts) {
    update(DatabaseCell.create(userId), DatabaseRow.of(userId, accounts));
  }

  public void deleteAccounts(UUID userId) {
    delete(DatabaseCell.create(userId));
  }

  public CompletableFuture<Boolean> accountExists(UUID userId) {
    return exists(DatabaseCell.create(userId));
  }

  public CompletableFuture<List<GoogleAccount>> findAccounts(UUID userId) {
    return selectRow(DatabaseCell.create(userId))
      .thenApply(row -> row.findCell(1).<String>listValue().stream()
        .map(GoogleAccount::of).collect(Collectors.toList()));
  }
}