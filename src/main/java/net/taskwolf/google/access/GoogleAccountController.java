package net.taskwolf.google.access;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.people.v1.PeopleService;
import com.google.api.services.people.v1.model.Person;
import com.google.common.collect.Lists;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.taskwolf.core.access.TaskwolfRestController;
import net.taskwolf.core.distribution.Distribution;
import net.taskwolf.core.notification.NotificationDatabaseTable;
import net.taskwolf.core.user.ProfilePictureDatabaseTable;
import net.taskwolf.core.user.User;
import net.taskwolf.core.user.UserDatabaseTable;
import net.taskwolf.google.account.GoogleAccount;
import net.taskwolf.google.account.GoogleAccountDatabaseTable;
import net.taskwolf.google.account.GoogleUserAccountDatabaseTable;
import net.taskwolf.google.account.GoogleCredential;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.security.Key;
import java.util.AbstractMap;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
public class GoogleAccountController extends TaskwolfRestController {
  private final String clientId;
  private final String clientSecret;
  private final GoogleAccountDatabaseTable googleAccountDatabaseTable;
  private final GoogleUserAccountDatabaseTable googleUserAccountDatabaseTable;
  private final ProfilePictureDatabaseTable profilePictureDatabaseTable;
  private final String defaultProfilePicture;
  private final NotificationDatabaseTable notificationDatabaseTable;
  private final Distribution distribution;

  private GoogleAccountController(
    Key secretKey, UserDatabaseTable userDatabaseTable,
    @Qualifier("clientId") String clientId,
    @Qualifier("clientSecret") String clientSecret,
    GoogleAccountDatabaseTable googleAccountDatabaseTable,
    GoogleUserAccountDatabaseTable googleUserAccountDatabaseTable,
    ProfilePictureDatabaseTable profilePictureDatabaseTable,
    @Qualifier("defaultProfilePicture") String defaultProfilePicture,
    NotificationDatabaseTable notificationDatabaseTable, Distribution distribution
  ) {
    super(secretKey, userDatabaseTable);
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.googleAccountDatabaseTable = googleAccountDatabaseTable;
    this.googleUserAccountDatabaseTable = googleUserAccountDatabaseTable;
    this.profilePictureDatabaseTable = profilePictureDatabaseTable;
    this.defaultProfilePicture = defaultProfilePicture;
    this.notificationDatabaseTable = notificationDatabaseTable;
    this.distribution = distribution;
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
    userDatabaseTable().findUser(findUserId(apiKey)).thenAccept(user ->
      addAccount(user, id, code));
  }

  private void addAccount(User user, UUID id, String code) {
    if (!user.id().equals(id) && !user.organizations().contains(id)) {
      return;
    }
    new Thread(() -> saveGoogleAccount(id, code)).start();
  }

  private static final String ACCOUNT_ADD_REDIRECT_URI =
    "https://api.taskwolf.net/v1/google/account/add/";

  private void saveGoogleAccount(UUID userId, String code) {
    var account = fetchGoogleAccount(code, ACCOUNT_ADD_REDIRECT_URI);
    googleAccountDatabaseTable.insertAccount(account);
    googleUserAccountDatabaseTable.addAccount(userId, account.id());
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

  private CompletableFuture<Void> googleLogin(GoogleAccount account, HttpServletResponse response) {
    var futureResponse = new CompletableFuture<Void>();
    var email = account.emailAddress();
    userDatabaseTable().userExists(email).thenApply(exists -> (exists ?
        userDatabaseTable().findUser(email).thenApply(User::id) :
        userDatabaseTable().generateAvailableUserId())
        .thenApply(id -> new AbstractMap.SimpleEntry<>(exists, id)))
      .thenAccept(future -> future.thenAccept(entry ->
          finishGoogleLogin(account, entry.getKey(), entry.getValue(), response))
        .thenAccept(futureResponse::complete));
    return futureResponse;
  }

  private static final String TOKEN_COOKIE_FORMAT =
    "token=%s; Domain=.taskwolf.net; Path=/; Expires=%s; Secure";

  private void finishGoogleLogin(
    GoogleAccount account, boolean userExists, UUID userId,
    HttpServletResponse response
  ) {
    if (!userExists) {
      insertNewUser(userId, account.displayName(), account.emailAddress(), "");
    }
    var token = generateApiKey(userId);
    var date = new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24).toString();
    var cookieContent = String.format(TOKEN_COOKIE_FORMAT, token, date);
    response.addHeader("Set-Cookie", cookieContent);
    try {
      response.sendRedirect("https://taskwolf.net/");
    } catch (Exception exception) {
      exception.printStackTrace();
    }
  }

  private void insertNewUser(
    UUID userId, String name, String email, String passwordHash
  ) {
    userDatabaseTable().insertUser(userId, name, email, passwordHash, "en",
      Lists.newArrayList());
    profilePictureDatabaseTable.insertProfilePicture(userId, defaultProfilePicture);
    notificationDatabaseTable.insertNotificationSettings(userId, true, true);
    distribution.addNewUser(userId);
  }

  private static final int EXPIRATION_TIME = 1000 * 60 * 60;

  private String generateApiKey(UUID userId) {
    var expiration = new Date(System.currentTimeMillis() + EXPIRATION_TIME);
    return Jwts.builder()
      .setExpiration(expiration)
      .claim("id", userId.toString())
      .signWith(secretKey())
      .compact();
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
