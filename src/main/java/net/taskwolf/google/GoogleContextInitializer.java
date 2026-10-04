package net.taskwolf.google;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class GoogleContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
  private final GoogleAccountLinkRepository googleAccountLinkRepository;

  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {
    var beanFactory = applicationContext.getBeanFactory();
    beanFactory.registerSingleton("googleAccountLinkRepository",
      googleAccountLinkRepository);
  }
}
