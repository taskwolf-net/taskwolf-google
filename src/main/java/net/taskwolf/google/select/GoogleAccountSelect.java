package net.taskwolf.google.select;

import lombok.RequiredArgsConstructor;
import net.taskwolf.core.iterator.AsyncIterator;
import net.taskwolf.core.user.User;
import net.taskwolf.core.workflow.component.input.InputComponentSelect;
import net.taskwolf.google.account.GoogleAccountDatabaseTable;
import net.taskwolf.google.account.GoogleUserAccountDatabaseTable;
import org.json.JSONObject;

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
  public CompletableFuture<List<String>> compile(
    User user, UUID target, Map<String, String> previousInputs
  ) {
    var futureResponse = new CompletableFuture<List<String>>();
    googleUserAccountDatabaseTable.findAccountsIfExists(target).thenApply(accountIds ->
      AsyncIterator.execute(accountIds, googleAccountDatabaseTable::findAccount)
        .thenAccept(accounts -> futureResponse.complete(accounts.stream().map(
          account -> new JSONObject(Map.of("identifier", account.id(), "name",
            account.displayName() + " | " + account.emailAddress())).toString())
          .collect(Collectors.toList()))));
    return futureResponse;
  }
}
