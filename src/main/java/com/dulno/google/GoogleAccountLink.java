package com.dulno.google;

import com.dulno.core.account.AccountLinkEntry;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.Getter;
import lombok.experimental.Accessors;
import com.dulno.core.account.AccountLink;
import com.dulno.core.iterator.AsyncIterator;
import com.dulno.google.account.GoogleAccount;
import com.dulno.google.account.GoogleAccountDatabaseTable;
import com.dulno.google.account.GoogleUserAccountDatabaseTable;
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
  public CompletableFuture<List<AccountLinkEntry>> findAccounts(UUID id) {
    var futureResponse = new CompletableFuture<List<AccountLinkEntry>>();
    googleUserAccountDatabaseTable.findAccounts(id).thenApply(accountIds ->
      AsyncIterator.execute(accountIds, googleAccountDatabaseTable::findAccount)
        .thenAccept(accounts -> futureResponse.complete(completeAccountFinding(accounts))));
    return futureResponse;
  }

  private List<AccountLinkEntry> completeAccountFinding(
    List<GoogleAccount> accounts
  ) {
    checkAccountsTokenRefresh(accounts);
    return accounts.stream().map(account -> AccountLinkEntry.create(account.id(),
      account.displayName() + " | " + account.emailAddress())).toList();
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
    } catch (Exception ignored) {
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

  private static final String GOOGLE_REGISTRATION_URL = "https://accounts.google.com/o/oauth2/auth?access_type=offline&prompt=consent&client_id=GOOGLE_CLIENT_ID&redirect_uri=https://api.dulno.com/v1/google/account/add/&state=DULNO-STATE&response_type=code&scope=";

  @Override
  public String registrationUrl(UUID id, String apiKey) {
    return GOOGLE_REGISTRATION_URL.replace("DULNO-STATE", apiKey +
        "DULNO-STATE-SPLIT" + id.toString() + "DULNO-STATE-SPLIT" + module)
      .replace("GOOGLE_CLIENT_ID", googleConfiguration.clientId()) +
      String.join(" ", scopes);
  }

  @Override
  public String description() {
    return "google.account.link.description";
  }
}
