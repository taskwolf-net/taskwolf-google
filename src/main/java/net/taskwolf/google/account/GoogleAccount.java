package net.taskwolf.google.account;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.json.JSONObject;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor(staticName = "create")
public final class GoogleAccount {
  public static GoogleAccount of(String content) {
    var json = new JSONObject(content);
    return create(json.getString("accessToken"), json.getString("refreshToken"),
      json.getLong("expirationTime"), json.getString("resourceName"),
      json.getString("displayName"), json.getString("emailAddress"));
  }

  private final String accessToken;
  private final String refreshToken;
  private final long expirationTime;
  private final String resourceName;
  private final String displayName;
  private final String emailAddress;

  public String encode() {
    var json = new JSONObject();
    json.put("accessToken", accessToken);
    json.put("refreshToken", refreshToken);
    json.put("expirationTime", expirationTime);
    json.put("resourceName", resourceName);
    json.put("displayName", displayName);
    json.put("emailAddress", emailAddress);
    return json.toString();
  }
}
