package net.taskwolf.google.account;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.taskwolf.core.database.DatabaseRow;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor(staticName = "create")
public final class GoogleAccount {
  public static GoogleAccount of(DatabaseRow row) {
    return create(row.findCell(0).stringValue(), row.findCell(1).stringValue(),
      row.findCell(2).stringValue(), row.findCell(3).stringValue(),
      row.findCell(4).stringValue(), row.findCell(5).longValue());
  }

  private final String id;
  private final String refreshToken;
  private final String displayName;
  private final String emailAddress;
  private String accessToken;
  private long expirationTime;

  public void updateVerification(
    String accessToken, long expirationTime
  ) {
    this.accessToken = accessToken;
    this.expirationTime = expirationTime;
  }
}
