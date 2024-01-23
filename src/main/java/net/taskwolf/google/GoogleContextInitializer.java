package net.taskwolf.google;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.taskwolf.google.account.GoogleAccountDatabaseTable;
import net.taskwolf.google.account.GoogleUserAccountDatabaseTable;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class GoogleContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
  private final GoogleAccountDatabaseTable googleAccountDatabaseTable;
  private final GoogleUserAccountDatabaseTable googleUserAccountDatabaseTable;
  private final GoogleAccountLinkRepository googleAccountLinkRepository;

  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {
    var beanFactory = applicationContext.getBeanFactory();
    beanFactory.registerSingleton("googleAccountDatabaseTable",
      googleAccountDatabaseTable);
    beanFactory.registerSingleton("googleUserAccountDatabaseTable",
      googleUserAccountDatabaseTable);
    beanFactory.registerSingleton("googleAccountLinkRepository",
      googleAccountLinkRepository);
  }
}
