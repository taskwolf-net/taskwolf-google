package net.taskwolf.google.select;

import lombok.RequiredArgsConstructor;
import net.taskwolf.core.workflow.component.ComponentSelect;
import net.taskwolf.google.account.GoogleAccountDatabaseTable;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RequiredArgsConstructor(staticName = "create")
public class GoogleAccountSelect implements ComponentSelect {
  private final GoogleAccountDatabaseTable googleAccountDatabaseTable;

  @Override
  public CompletableFuture<List<String>> compile(
    UUID user, Map<String, String> previousInputs
  ) {
    return googleAccountDatabaseTable.findAccounts(user).thenApply(accounts ->
      accounts.stream().map(account ->
        new JSONObject(Map.of("identifier", account.resourceName(), "name",
          account.displayName() + " | " + account.emailAddress())).toString())
        .collect(Collectors.toList()));
  }
}
