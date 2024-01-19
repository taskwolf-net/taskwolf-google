package net.taskwolf.google.access;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.people.v1.PeopleService;
import com.google.api.services.people.v1.model.Person;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.taskwolf.core.access.TaskwolfRestController;
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
import java.util.UUID;

@RestController
public class GoogleAccountController extends TaskwolfRestController {
  private final String clientId;
  private final String clientSecret;
  private final GoogleAccountDatabaseTable googleAccountDatabaseTable;
  private final GoogleUserAccountDatabaseTable googleUserAccountDatabaseTable;

  private GoogleAccountController(
    Key secretKey, UserDatabaseTable userDatabaseTable,
    @Qualifier("clientId") String clientId, @Qualifier("clientSecret") String clientSecret,
    GoogleAccountDatabaseTable googleAccountDatabaseTable,
    GoogleUserAccountDatabaseTable googleUserAccountDatabaseTable
  ) {
    super(secretKey, userDatabaseTable);
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.googleAccountDatabaseTable = googleAccountDatabaseTable;
    this.googleUserAccountDatabaseTable = googleUserAccountDatabaseTable;
  }

  @RequestMapping(path = "/google/account/add/", method = RequestMethod.GET)
  public void addAccount(
    HttpServletRequest request, HttpServletResponse response,
    @RequestParam("state") String state, @RequestParam("code") String code
  ) throws Exception {
    response.setStatus(310);
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
    new Thread(() -> sendTokenRequest(id, code)).start();
  }

  private static final String REDIRECT_URI = "https://api.taskwolf.net/v1/google/account/add/";

  private void sendTokenRequest(UUID userId, String code) {
    try {
      var response = new GoogleAuthorizationCodeTokenRequest(GoogleNetHttpTransport.newTrustedTransport(),
        GsonFactory.getDefaultInstance(), clientId, clientSecret, URLDecoder.decode(code, "UTF-8"),
        REDIRECT_URI).execute();
      var accessToken = response.getAccessToken();
      var refreshToken = response.getRefreshToken();
      var expirationTime = response.getExpiresInSeconds();
      var person = catchAccountInformation(accessToken, refreshToken, expirationTime);
      var id = person.getResourceName().replace("people/", "");
      var displayName = person.getNames().get(0).getDisplayName();
      var emailAddress = person.getEmailAddresses().get(0).getValue();
      var account = GoogleAccount.create(id, refreshToken, displayName, emailAddress,
        accessToken, System.currentTimeMillis() + (expirationTime * 1000));
      googleAccountDatabaseTable.insertAccount(account);
      googleUserAccountDatabaseTable.addAccount(userId, id);
    } catch (Exception exception) {
      exception.printStackTrace();
    }
  }

  private Person catchAccountInformation(
    String accessToken, String refreshToken, long expirationTime
  ) throws Exception {
    var credential = GoogleCredential.create(clientId, clientSecret, accessToken,
      refreshToken, expirationTime).buildCredential();
    var service = new PeopleService.Builder(GoogleNetHttpTransport.newTrustedTransport(),
      GsonFactory.getDefaultInstance(), credential).setApplicationName("Taskwolf").build();
    return service.people().get("people/me")
      .setPersonFields("names,emailAddresses")
      .execute();
  }
}
