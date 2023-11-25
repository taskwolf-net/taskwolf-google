package net.taskwolf.google;

import lombok.RequiredArgsConstructor;
import net.taskwolf.core.account.AccountLink;
import net.taskwolf.google.account.GoogleUserAccountDatabaseTable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor(staticName = "create")
public final class GoogleAccountLink implements AccountLink {
  private final GoogleUserAccountDatabaseTable googleUserAccountDatabaseTable;

  @Override
  public CompletableFuture<Boolean> accountExists(UUID userId) {
    return googleUserAccountDatabaseTable.accountExists(userId);
  }

  private static final String[] GOOGLE_SCOPES = {"https://www.googleapis.com/auth/userinfo.email", "https://www.googleapis.com/auth/userinfo.profile", "https://mail.google.com/"};
  private static final String GOOGLE_REGISTRATION_URL = "https://accounts.google.com/o/oauth2/auth?access_type=offline&client_id=449589853116-jfrqb583smjop8m0rsvk7gspqge2sta9.apps.googleusercontent.com&redirect_uri=https://api.taskwolf.net/google/account/add/&state=API-KEY&response_type=code&scope=" + String.join("%20", GOOGLE_SCOPES);

  @Override
  public String registrationUrl(String apiKey) {
    return GOOGLE_REGISTRATION_URL.replace("API-KEY", apiKey);
  }

  @Override
  public String description() {
    return "Would you like to add another Google account to integrate it into your automation with the help of Taskwolf? Just click on the logo.";
  }
}
