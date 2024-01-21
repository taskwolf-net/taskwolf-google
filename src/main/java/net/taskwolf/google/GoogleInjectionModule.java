package net.taskwolf.google;

import com.google.inject.AbstractModule;
import lombok.RequiredArgsConstructor;
import net.taskwolf.google.account.GoogleAccountInjectionModule;

@RequiredArgsConstructor(staticName = "create")
public final class GoogleInjectionModule extends AbstractModule {
  @Override
  protected void configure() {
    install(GoogleAccountInjectionModule.create());
  }
}
