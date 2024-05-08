package net.taskwolf.google;

import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.taskwolf.core.account.AccountLink;
import net.taskwolf.core.iterator.AsyncIterator;
import net.taskwolf.google.account.GoogleAccount;
import net.taskwolf.google.account.GoogleAccountDatabaseTable;
import net.taskwolf.google.account.GoogleUserAccountDatabaseTable;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Accessors(fluent = true)
public class GoogleAccountLink implements AccountLink {
  @Getter
  protected final GoogleConfiguration googleConfiguration;
  @Getter
  protected final GoogleAccountDatabaseTable googleAccountDatabaseTable;
  @Getter
  protected final GoogleUserAccountDatabaseTable googleUserAccountDatabaseTable;
  @Getter
  private final String module;
  private final List<String> scopes;

  protected GoogleAccountLink(
    GoogleConfiguration googleConfiguration,
    GoogleAccountDatabaseTable googleAccountDatabaseTable,
    GoogleUserAccountDatabaseTable googleUserAccountDatabaseTable,
    String module, List<String> scopes
  ) {
    this.googleConfiguration = googleConfiguration;
    this.googleAccountDatabaseTable = googleAccountDatabaseTable;
    this.googleUserAccountDatabaseTable = googleUserAccountDatabaseTable;
    this.module = module;
    this.scopes = scopes;
    this.scopes.add("https://www.googleapis.com/auth/userinfo.email");
    this.scopes.add("https://www.googleapis.com/auth/userinfo.profile");
  }

  public void registerAccount(UUID id, String identifier) throws Exception {

  }

  @Override
  public CompletableFuture<Boolean> accountExists(UUID id) {
    checkAccountsTokenRefresh(id);
    return googleUserAccountDatabaseTable.accountExists(id);
  }

  @Override
  public CompletableFuture<List<String>> findAccounts(UUID id) {
    var futureResponse = new CompletableFuture<List<String>>();
    googleUserAccountDatabaseTable.findAccounts(id).thenApply(accountIds ->
      AsyncIterator.execute(accountIds, googleAccountDatabaseTable::findAccount)
        .thenAccept(accounts -> futureResponse.complete(completeAccountFinding(accounts))));
    return futureResponse;
  }

  private List<String> completeAccountFinding(
    List<GoogleAccount> accounts
  ) {
    checkAccountsTokenRefresh(accounts);
    return accounts.stream().map(account -> new JSONObject(Map.of("identifier",
      account.id(), "name", account.displayName() + " | " +
        account.emailAddress())).toString()).toList();
  }

  private void checkAccountsTokenRefresh(UUID id) {
    googleUserAccountDatabaseTable.findAccounts(id).thenApply(accountIds ->
      AsyncIterator.execute(accountIds, googleAccountDatabaseTable::findAccount)
        .thenAccept(this::checkAccountsTokenRefresh));
  }

  private void checkAccountsTokenRefresh(List<GoogleAccount> accounts) {
    for (var account : accounts) {
      if (System.currentTimeMillis() > account.expirationTime()) {
        refreshToken(account);
      }
    }
  }

  private void refreshToken(GoogleAccount account) {
    try {
      var response = new GoogleRefreshTokenRequest(GoogleNetHttpTransport.newTrustedTransport(),
        GsonFactory.getDefaultInstance(), account.refreshToken(), googleConfiguration.clientId(),
        googleConfiguration.clientSecret()).execute();
      account.updateVerification(response.getAccessToken(), System.currentTimeMillis() +
        (response.getExpiresInSeconds() * 1000));
      googleAccountDatabaseTable.updateAccount(account);
    } catch (Exception exception) {
      exception.printStackTrace();
    }
  }

  @Override
  public void removeAccount(UUID id, String identifier) {
    googleUserAccountDatabaseTable.accountOccurNumber(identifier).thenAccept(
      occur -> removeAccount(id, identifier, occur));
  }

  public void removeAccount(UUID id, String identifier, int accountOccurNumber) {
    if (accountOccurNumber == 1) {
      googleAccountDatabaseTable.deleteAccount(identifier);
    }
    googleUserAccountDatabaseTable.removeAccount(id, identifier);
  }

  private static final String GOOGLE_REGISTRATION_URL = "https://accounts.google.com/o/oauth2/auth?access_type=offline&prompt=consent&client_id=GOOGLE_CLIENT_ID&redirect_uri=https://api.taskwolf.net/v1/google/account/add/&state=TASKWOLF-STATE&response_type=code&scope=";

  @Override
  public String registrationUrl(UUID id, String apiKey) {
    return GOOGLE_REGISTRATION_URL.replace("TASKWOLF-STATE", apiKey +
        "TASKWOLF-STATE-SPLIT" + id.toString() + "TASKWOLF-STATE-SPLIT" + module)
      .replace("GOOGLE_CLIENT_ID", googleConfiguration.clientId()) +
      String.join(" ", scopes);
  }

  @Override
  public String description() {
    return "google.account.link.description";
  }
}
