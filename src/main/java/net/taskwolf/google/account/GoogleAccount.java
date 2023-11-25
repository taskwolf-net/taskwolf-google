package net.taskwolf.google.account;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import net.taskwolf.core.database.DatabaseRow;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor(staticName = "create")
public final class GoogleAccount {
  public static GoogleAccount of(DatabaseRow row) {
    return create(row.findCell(0).stringValue(), row.findCell(1).stringValue(),
      row.findCell(2).stringValue(), row.findCell(3).longValue(),
      row.findCell(4).stringValue(), row.findCell(5).stringValue());
  }

  private final String id;
  private final String accessToken;
  private final String refreshToken;
  private final long expirationTime;
  private final String displayName;
  private final String emailAddress;
}
