package com.dulno.google;

import lombok.Getter;
import lombok.experimental.Accessors;
import com.dulno.core.configuration.Configuration;
import org.json.JSONObject;

@Getter
@Accessors(fluent = true)
public final class GoogleConfiguration extends Configuration {
  private static final String CONFIGURATION_PATH = "/configurations/google/google.json";

  public static GoogleConfiguration createAndLoad() throws Exception {
    var configuration = new GoogleConfiguration(CONFIGURATION_PATH);
    configuration.load();
    return configuration;
  }

  private String clientId;
  private String clientSecret;

  private GoogleConfiguration(String path) {
    super(path);
  }

  @Override
  protected void deserialize(JSONObject json) {
    clientId = json.getString("clientId");
    clientSecret = json.getString("clientSecret");
  }
}

