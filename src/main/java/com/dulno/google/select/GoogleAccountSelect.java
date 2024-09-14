package com.dulno.google.select;

import lombok.RequiredArgsConstructor;
import com.dulno.core.iterator.AsyncIterator;
import com.dulno.core.user.User;
import com.dulno.core.workflow.component.input.InputComponentSelect;
import com.dulno.core.workflow.component.input.InputComponentSelectEntry;
import com.dulno.google.account.GoogleAccountDatabaseTable;
import com.dulno.google.account.GoogleUserAccountDatabaseTable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RequiredArgsConstructor(staticName = "create")
public class GoogleAccountSelect implements InputComponentSelect {
  private final GoogleAccountDatabaseTable googleAccountDatabaseTable;
  private final GoogleUserAccountDatabaseTable googleUserAccountDatabaseTable;

  @Override
  public CompletableFuture<List<InputComponentSelectEntry>> compile(
    User user, UUID target, Map<String, String> previousInputs
  ) {
    return googleUserAccountDatabaseTable.findAccountsIfExists(target)
      .thenCompose(accountIds -> AsyncIterator.execute(accountIds,
          googleAccountDatabaseTable::findAccount)
        .thenApply(accounts -> accounts.stream().map(
          account -> InputComponentSelectEntry.create(account.id(),
            account.displayName() + " | " + account.emailAddress()))
          .collect(Collectors.toList())));
  }
}
