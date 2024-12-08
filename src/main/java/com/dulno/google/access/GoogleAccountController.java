package com.dulno.google.access;

import com.dulno.access.trial.TrialController;
import com.dulno.access.verification.VerificationRegistrationController;
import com.dulno.core.error.ErrorRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.people.v1.PeopleService;
import com.google.api.services.people.v1.model.Person;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.dulno.access.verification.Verification;
import com.dulno.access.verification.VerificationLoginController;
import com.dulno.core.access.DulnoRestController;
import com.dulno.core.organization.team.TeamTargetDatabaseTable;
import com.dulno.core.user.*;
import com.dulno.google.GoogleAccountLink;
import com.dulno.google.GoogleAccountLinkRepository;
import com.dulno.google.account.GoogleAccount;
import com.dulno.google.account.GoogleAccountDatabaseTable;
import com.dulno.google.account.GoogleCredential;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.security.Key;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@RestController
public class GoogleAccountController extends DulnoRestController {
  private final Key homeKey;
  private final Key refreshKey;
  private final String clientId;
  private final String clientSecret;
  private final GoogleAccountLinkRepository googleAccountLinkRepository;
  private final VerificationLoginController verificationLoginController;
  private final VerificationRegistrationController verificationRegistrationController;
  private final TrialController trialController;
  private final TeamTargetDatabaseTable teamTargetDatabaseTable;
  private final ErrorRepository errorRepository;

  private GoogleAccountController(
    @Qualifier("homeKey") Key homeKey, @Qualifier("productKey") Key productKey,
    @Qualifier("refreshKey") Key refreshKey, UserDatabaseTable userDatabaseTable,
    @Qualifier("clientId") String clientId,
    @Qualifier("clientSecret") String clientSecret,
    GoogleAccountLinkRepository googleAccountLinkRepository,
    VerificationLoginController verificationLoginController,
    VerificationRegistrationController verificationRegistrationController,
    TrialController trialController,
    TeamTargetDatabaseTable teamTargetDatabaseTable,
    ErrorRepository errorRepository
  ) {
    super(productKey, userDatabaseTable);
    this.homeKey = homeKey;
    this.refreshKey = refreshKey;
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.googleAccountLinkRepository = googleAccountLinkRepository;
    this.verificationLoginController = verificationLoginController;
    this.verificationRegistrationController = verificationRegistrationController;
    this.trialController = trialController;
    this.teamTargetDatabaseTable = teamTargetDatabaseTable;
    this.errorRepository = errorRepository;
  }

  @RequestMapping(path = "/google/account/add/", method = RequestMethod.GET)
  public void addAccount(
    HttpServletRequest request, @RequestParam("state") String state,
    @RequestParam("code") String code, HttpServletResponse response
  ) throws Exception {
    response.sendRedirect("https://dulno.com/close/");
    var split = state.split("DULNO-STATE-SPLIT");
    var apiKey = split[0];
    if (!isValidApiKey(apiKey)) {
      return;
    }
    var id = UUID.fromString(split[1]);
    var module = split[2];
    userDatabaseTable().findUser(findUserId(apiKey)).thenAccept(user ->
      checkUserAuthorization(user, id).thenAccept(isAuthorized ->
        addAccount(id, module, code, isAuthorized)));
  }

  private void addAccount(UUID id, String module, String code, boolean isAuthorized) {
    if (!isAuthorized) {
      return;
    }
    new Thread(() -> googleAccountLinkRepository.findAccountLink(module)
      .ifPresent(link -> finishAccountAdding(id, link, code))).start();
  }

  private static final String ACCOUNT_ADD_REDIRECT_URI =
    "https://api.dulno.com/v1/google/account/add/";

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
      errorRepository.processError(exception);
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
    "https://api.dulno.com/v1/google/login/";

  @RequestMapping(path = "/google/login/", method = RequestMethod.GET)
  public CompletableFuture<Void> googleLogin(
    HttpServletRequest request, @RequestParam("state") String state,
    @RequestParam("code") String code, HttpServletResponse response
  ) {
    var futureResponse = new CompletableFuture<Void>();
    new Thread(() -> googleLogin(fetchGoogleAccount(code, GOOGLE_LOGIN_REDIRECT_URI),
      request, response, state).thenAccept(futureResponse::complete)).start();
    return futureResponse;
  }

  private CompletableFuture<Void> googleLogin(
    GoogleAccount account, HttpServletRequest request,
    HttpServletResponse response, String redirect
  ) {
    return userDatabaseTable().userExists(account.emailAddress()).thenCompose(
      exists -> googleLogin(account, request, response, redirect, exists));
  }

  private CompletableFuture<Void> googleLogin(
    GoogleAccount account, HttpServletRequest request,
    HttpServletResponse response, String redirect, boolean userExists
  ) {
    if (!userExists) {
      return verificationRegistrationController.createUser(account.displayName(),
          account.emailAddress(), "", "/dashboard/",
          request.getHeader("X-Real-IP"), true, true, false)
        .thenCompose(user -> trialController.useTrial(request, user))
        .thenCompose(value -> requestGoogleLogin(account, request, response,
          redirect));
    }
    return requestGoogleLogin(account, request, response, redirect);
  }

  private CompletableFuture<Void> requestGoogleLogin(
    GoogleAccount account, HttpServletRequest request,
    HttpServletResponse response, String redirect
  ) {
    var verification = Verification.create(userDatabaseTable(), homeKey,
      secretKey(), refreshKey, account.emailAddress(), "");
    var futureResponse = new CompletableFuture<Map<String, Object>>();
    verificationLoginController.processAuthorizedLogin(request, verification,
      futureResponse);
    return futureResponse.thenAccept(result -> finishGoogleLogin(result,
      response, redirect));
  }

  private static final String PRODUCT_TOKEN_COOKIE_FORMAT =
    "token=%s; Domain=.dulno.com; Path=/; Expires=%s; Secure";
  private static final String REFRESH_TOKEN_COOKIE_FORMAT =
    "refresh-token=%s; Domain=.dulno.com; Path=/; Expires=%s; Secure";
  private static final String HOME_TOKEN_COOKIE_FORMAT =
    "home-token=%s; Domain=.dulno.com; Path=/; Expires=%s; Secure";

  private void finishGoogleLogin(
    Map<String, Object> loginResult, HttpServletResponse response,
    String redirect
  ) {
    try {
      if (!((boolean) loginResult.get("success"))) {
        processLoginFailure(loginResult, response);
        return;
      }
      var date = new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 30).toString();
      response.addHeader("Set-Cookie", String.format(PRODUCT_TOKEN_COOKIE_FORMAT,
        loginResult.get("productApiKey"), date));
      response.addHeader("Set-Cookie", String.format(REFRESH_TOKEN_COOKIE_FORMAT,
        loginResult.get("refreshToken"), date));
      response.addHeader("Set-Cookie", String.format(HOME_TOKEN_COOKIE_FORMAT,
        loginResult.get("homeApiKey"), date));
      response.sendRedirect("https://dulno.com" + redirect);
    } catch (Exception exception) {
      errorRepository.processError(exception);
    }
  }

  private void processLoginFailure(
    Map<String, Object> loginResult, HttpServletResponse response
  ) throws Exception {
    var errorCode = (int) loginResult.get("error");
    if (errorCode == 1001) {
      response.sendRedirect("https://dulno.com/login/");
    } else if (errorCode == 1003) {
      prepareExpirationCookies(loginResult, response);
      response.sendRedirect("https://dulno.com/expiration/");
    }
  }

  private static final String EXPIRATION_HAS_PERSONAL_PACKAGE_COOKIE_FORMAT =
    "expiration-has-personal-package=%s; Domain=.dulno.com; Path=/; Expires=%s; Secure";
  private static final String EXPIRATION_HAS_OWN_ORGANIZATION_COOKIE_FORMAT =
    "expiration-has-own-organization=%s; Domain=.dulno.com; Path=/; Expires=%s; Secure";
  private static final String EXPIRATION_IS_ORGANIZATION_MEMBER_COOKIE_FORMAT =
    "expiration-is-organization-member=%s; Domain=.dulno.com; Path=/; Expires=%s; Secure";
  private static final String EXPIRATION_USER_NAME_COOKIE_FORMAT =
    "expiration-user-name=%s; Domain=.dulno.com; Path=/; Expires=%s; Secure";

  private void prepareExpirationCookies(
    Map<String, Object> loginResult, HttpServletResponse response
  ) {
    var date = new Date(System.currentTimeMillis() + 1000L * 60 * 60).toString();
    response.addHeader("Set-Cookie", String.format(HOME_TOKEN_COOKIE_FORMAT,
      loginResult.get("homeApiKey"), date));
    response.addHeader("Set-Cookie",
      String.format(EXPIRATION_HAS_PERSONAL_PACKAGE_COOKIE_FORMAT,
        loginResult.get("hasPersonalBundle"), date));
    response.addHeader("Set-Cookie",
      String.format(EXPIRATION_HAS_OWN_ORGANIZATION_COOKIE_FORMAT,
        loginResult.get("hasOwnOrganization"), date));
    response.addHeader("Set-Cookie",
      String.format(EXPIRATION_IS_ORGANIZATION_MEMBER_COOKIE_FORMAT,
        loginResult.get("isOrganizationMember"), date));
    response.addHeader("Set-Cookie",
      String.format(EXPIRATION_USER_NAME_COOKIE_FORMAT,
        loginResult.get("userName"), date));
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
      errorRepository.processError(exception);
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
      var service = serviceBuilder.setApplicationName("Dulno").build();
      return service.people().get("people/me")
        .setPersonFields("names,emailAddresses")
        .execute();
    } catch (Exception exception) {
      errorRepository.processError(exception);
      return null;
    }
  }

  private CompletableFuture<Boolean> checkUserAuthorization(User user, UUID id) {
    if (id.equals(user.id()) || user.organizations().contains(id)) {
      return CompletableFuture.completedFuture(true);
    }
    return teamTargetDatabaseTable.findTargetSecured(user.id()).thenApply(
      teamTarget -> teamTarget.map(uuid -> uuid.equals(id)).orElse(false));
  }
}
