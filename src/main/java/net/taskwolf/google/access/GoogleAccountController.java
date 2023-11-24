package net.taskwolf.google.access;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.inject.name.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.taskwolf.core.access.TaskwolfRestController;
import net.taskwolf.core.user.UserDatabaseTable;
import net.taskwolf.google.account.GoogleAccount;
import net.taskwolf.google.account.GoogleAccountDatabaseTable;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.security.Key;
import java.util.UUID;

@RestController
public class GoogleAccountController extends TaskwolfRestController {
  private final String clientId;
  private final String clientSecret;
  private final GoogleAccountDatabaseTable googleAccountDatabaseTable;
  private final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
  private final GsonFactory gsonFactory = new GsonFactory();

  private GoogleAccountController(
    Key secretKey, UserDatabaseTable userDatabaseTable,
    @Named("clientId") String clientId, @Named("clientSecret") String clientSecret,
    GoogleAccountDatabaseTable googleAccountDatabaseTable
  ) throws Exception {
    super(secretKey, userDatabaseTable);
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.googleAccountDatabaseTable = googleAccountDatabaseTable;
  }

  @RequestMapping(path = "/google/account/add/", method = RequestMethod.GET)
  public void addAccount(
    HttpServletRequest request, HttpServletResponse response,
    @RequestParam("state") String apiKey, @RequestParam("code") String code
  ) throws Exception {
    if (!isValidApiKey(apiKey)) {
      return;
    }
    var userId = findUserId(apiKey);
    new Thread(() -> sendTokenRequest(userId, code)).start();
    response.setHeader("Location", "https://google.com");
    response.setStatus(302);
  }

  private static final String REDIRECT_URI = "https://api.taskwolf.net/google/account/add/";

  private void sendTokenRequest(UUID userId, String code) {
    try {
      var response = new GoogleAuthorizationCodeTokenRequest(httpTransport,
        gsonFactory, clientId, clientSecret, URLDecoder.decode(code, "UTF-8"),
        REDIRECT_URI).execute();
      var account = GoogleAccount.create(response.getAccessToken(),
        response.getRefreshToken(), response.getExpiresInSeconds());
      googleAccountDatabaseTable.addAccount(userId, account);
    } catch (Exception exception) {
      exception.printStackTrace();
    }
  }
}
