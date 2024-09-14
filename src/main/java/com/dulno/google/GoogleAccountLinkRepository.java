package com.dulno.google;

import com.google.api.client.util.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class GoogleAccountLinkRepository {
  private final List<GoogleAccountLink> accountLinks = Lists.newArrayList();

  public void registerGoogleAccountLink(GoogleAccountLink accountLink) {
    accountLinks.add(accountLink);
  }

  public void unregisterGoogleAccountLink(GoogleAccountLink accountLink) {
    accountLinks.remove(accountLink);
  }

  public Optional<GoogleAccountLink> findAccountLink(String module) {
    return accountLinks.stream().filter(link -> link.module().equals(module))
      .findFirst();
  }
}
