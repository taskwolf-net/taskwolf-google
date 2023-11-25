package net.taskwolf.google.account;

import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.Clock;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(staticName = "create")
public class GoogleCredential {
  public static GoogleCredential of(
    String clientId, String clientSecret, GoogleAccount account
  ) {
    return create(clientId, clientSecret, account.accessToken(),
      account.refreshToken(), account.expirationTime());
  }

  private final String clientId;
  private final String clientSecret;
  private final String accessToken;
  private final String refreshToken;
  private final long expirationTime;

  private static final String TOKEN_SERVER_URL = "https://oauth2.googleapis.com/token";

  public Credential buildCredential() throws Exception {
    var credential = new Credential.Builder(BearerToken.authorizationHeaderAccessMethod())
      .setTransport(GoogleNetHttpTransport.newTrustedTransport())
      .setJsonFactory(new GsonFactory())
      .setTokenServerEncodedUrl(TOKEN_SERVER_URL)
      .setClientAuthentication(new ClientParametersAuthentication(clientId, clientSecret))
      .setClock(Clock.SYSTEM)
      .build();
    credential.setAccessToken(accessToken);
    credential.setRefreshToken(refreshToken);
    credential.setExpiresInSeconds(expirationTime);
    return credential;
  }
}
