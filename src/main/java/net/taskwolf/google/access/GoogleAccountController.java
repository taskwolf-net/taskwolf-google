package net.taskwolf.google.access;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.people.v1.PeopleService;
import com.google.api.services.people.v1.model.Person;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.taskwolf.access.verification.Verification;
import net.taskwolf.access.verification.VerificationLoginController;
import net.taskwolf.core.access.TaskwolfRestController;
import net.taskwolf.core.user.*;
import net.taskwolf.google.GoogleAccountLink;
import net.taskwolf.google.GoogleAccountLinkRepository;
import net.taskwolf.google.account.GoogleAccount;
import net.taskwolf.google.account.GoogleAccountDatabaseTable;
import net.taskwolf.google.account.GoogleCredential;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.security.Key;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@RestController
public class GoogleAccountController extends TaskwolfRestController {
  private final String clientId;
  private final String clientSecret;
  private final GoogleAccountLinkRepository googleAccountLinkRepository;
  private final VerificationLoginController verificationLoginController;

  private GoogleAccountController(
    Key secretKey, UserDatabaseTable userDatabaseTable,
    @Qualifier("clientId") String clientId,
    @Qualifier("clientSecret") String clientSecret,
    GoogleAccountLinkRepository googleAccountLinkRepository,
    VerificationLoginController verificationLoginController
  ) {
    super(secretKey, userDatabaseTable);
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.googleAccountLinkRepository = googleAccountLinkRepository;
    this.verificationLoginController = verificationLoginController;
  }

  @RequestMapping(path = "/google/account/add/", method = RequestMethod.GET)
  public void addAccount(
    HttpServletRequest request, @RequestParam("state") String state,
    @RequestParam("code") String code, HttpServletResponse response
  ) throws Exception {
    response.sendRedirect("https://taskwolf.net/close/");
    var splitted = state.split("TASKWOLF-STATE-SPLIT");
    var apiKey = splitted[0];
    if (!isValidApiKey(apiKey)) {
      return;
    }
    var id = UUID.fromString(splitted[1]);
    var module = splitted[2];
    userDatabaseTable().findUser(findUserId(apiKey)).thenAccept(user ->
      addAccount(user, id, module, code));
  }

  private void addAccount(User user, UUID id, String module, String code) {
    if (!user.id().equals(id) && !user.organizations().contains(id)) {
      return;
    }
    new Thread(() -> googleAccountLinkRepository.findAccountLink(module)
      .ifPresent(link -> finishAccountAdding(id, link, code))).start();
  }

  private static final String ACCOUNT_ADD_REDIRECT_URI =
    "https://api.taskwolf.net/v1/google/account/add/";

  private void finishAccountAdding(UUID userId, GoogleAccountLink link, String code) {
    var account = fetchGoogleAccount(code, ACCOUNT_ADD_REDIRECT_URI);
    var accountId = account.id();
    var accountDatabaseTable = link.googleAccountDatabaseTable();
    var userAccountDatabaseTable = link.googleUserAccountDatabaseTable();
    accountDatabaseTable.accountExists(accountId).thenAccept(exists ->
      storeGoogleAccount(accountDatabaseTable, account, exists)
        .thenAccept(firstValue -> userAccountDatabaseTable.addAccount(userId, accountId))
        .thenAccept(secondValue -> broadcastAccountRegistration(link, userId, accountId)));
  }

  private void broadcastAccountRegistration(
    GoogleAccountLink link, UUID userId, String accountId
  ) {
    try {
      link.registerAccount(userId, accountId);
    } catch (Exception exception) {
      exception.printStackTrace();
    }
  }

  private CompletableFuture<Void> storeGoogleAccount(
    GoogleAccountDatabaseTable googleAccountDatabaseTable,
    GoogleAccount account, boolean exists
  ) {
    if (exists) {
      return CompletableFuture.completedFuture(null);
    }
    return googleAccountDatabaseTable.insertAccount(account);
  }

  private static final String GOOGLE_LOGIN_REDIRECT_URI =
    "https://api.taskwolf.net/v1/google/login/";

  @RequestMapping(path = "/google/login/", method = RequestMethod.GET)
  public CompletableFuture<Void> googleLogin(
    @RequestParam("code") String code, HttpServletResponse response
  ) {
    var futureResponse = new CompletableFuture<Void>();
    new Thread(() -> googleLogin(fetchGoogleAccount(code, GOOGLE_LOGIN_REDIRECT_URI),
      response).thenAccept(futureResponse::complete)).start();
    return futureResponse;
  }

  private CompletableFuture<Void> googleLogin(
    GoogleAccount account, HttpServletResponse response
  ) {
    return userDatabaseTable().userExists(account.emailAddress())
      .thenCompose(exists -> googleLogin(account, response, exists));
  }

  private CompletableFuture<Void> googleLogin(
    GoogleAccount account, HttpServletResponse response, boolean userExists
  ) {
    try {
      if (!userExists) {
        response.sendRedirect("https://taskwolf.net/register/");
        return CompletableFuture.completedFuture(null);
      }
      var verification = Verification.create(userDatabaseTable(), secretKey(),
        account.emailAddress(), "");
      var futureResponse = new CompletableFuture<Map<String, Object>>();
      verificationLoginController.processAuthorizedLogin(verification, futureResponse);
      return futureResponse.thenAccept(result -> finishGoogleLogin(result, response));
    } catch (Exception exception) {
      exception.printStackTrace();
      return CompletableFuture.completedFuture(null);
    }
  }

  private static final String TOKEN_COOKIE_FORMAT =
    "token=%s; Domain=.taskwolf.net; Path=/; Expires=%s; Secure";

  private void finishGoogleLogin(
    Map<String, Object> loginResult, HttpServletResponse response
  ) {
    try {
      if (!((boolean) loginResult.get("success"))) {
        processLoginFailure(loginResult, response);
        return;
      }
      var date = new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 30).toString();
      var cookieContent = String.format(TOKEN_COOKIE_FORMAT,
        loginResult.get("apiKey"), date);
      response.addHeader("Set-Cookie", cookieContent);
      response.sendRedirect("https://taskwolf.net/dashboard/");
    } catch (Exception exception) {
      exception.printStackTrace();
    }
  }

  private void processLoginFailure(
    Map<String, Object> loginResult, HttpServletResponse response
  ) throws Exception {
    var errorCode = (int) loginResult.get("error");
    if (errorCode == 1001 || errorCode == 1003) {
      response.sendRedirect("https://taskwolf.net/login/");
    } else if (errorCode == 1002) {
      response.sendRedirect("https://taskwolf.net/pricing/");
    }
  }

  private GoogleAccount fetchGoogleAccount(String code, String redirectUri) {
    var response = sendTokenRequest(code, redirectUri);
    var accessToken = response.getAccessToken();
    var refreshToken = response.getRefreshToken();
    var expirationTime = response.getExpiresInSeconds();
    var person = catchAccountInformation(accessToken, refreshToken, expirationTime);
    var id = person.getResourceName().replace("people/", "");
    var displayName = person.getNames().get(0).getDisplayName();
    var emailAddress = person.getEmailAddresses().get(0).getValue();
    return GoogleAccount.create(id, refreshToken, displayName, emailAddress,
      accessToken, System.currentTimeMillis() + (expirationTime * 1000));
  }

  private GoogleTokenResponse sendTokenRequest(String code, String redirectUri) {
    try {
      return new GoogleAuthorizationCodeTokenRequest(
        GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance(),
        clientId, clientSecret, URLDecoder.decode(code, "UTF-8"), redirectUri).execute();
    } catch (Exception exception) {
      exception.printStackTrace();
      return null;
    }
  }

  private Person catchAccountInformation(
    String accessToken, String refreshToken, long expirationTime
  ) {
    try {
      var credential = GoogleCredential.create(clientId, clientSecret,
        accessToken, refreshToken, expirationTime).buildCredential();
      var serviceBuilder = new PeopleService.Builder(
        GoogleNetHttpTransport.newTrustedTransport(),
        GsonFactory.getDefaultInstance(), credential);
      var service = serviceBuilder.setApplicationName("Taskwolf").build();
      return service.people().get("people/me")
        .setPersonFields("names,emailAddresses")
        .execute();
    } catch (Exception exception) {
      exception.printStackTrace();
      return null;
    }
  }
}
