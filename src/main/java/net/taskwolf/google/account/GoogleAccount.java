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
      json.getLong("expirationTime"));
  }

  private final String accessToken;
  private final String refreshToken;
  private final long expirationTime;

  public String encode() {
    var json = new JSONObject();
    json.put("accessToken", accessToken);
    json.put("refreshToken", refreshToken);
    json.put("expirationTime", expirationTime);
    return json.toString();
  }
}
