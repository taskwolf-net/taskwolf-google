package net.taskwolf.google;

import com.google.inject.Injector;
import net.taskwolf.core.log.Log;
import net.taskwolf.core.module.Module;
import net.taskwolf.core.module.ModuleDescription;
import net.taskwolf.core.module.ModuleInformation;
import net.taskwolf.core.module.ModuleLoadPriority;
import org.springframework.boot.SpringApplication;

@ModuleDescription(name = "google", version = "1.0.0-SNAPSHOT",
  priority = ModuleLoadPriority.HIGH)
public final class GoogleModule extends Module {
  private Log log;

  public GoogleModule(Injector injector) {
    super(injector);
  }

  @Override
  public void enable() {
    log = injector().getInstance(Log.class).subLog("Google");
    injector().getInstance(SpringApplication.class).addInitializers(
      injector().getInstance(GoogleContextInitializer.class));
  }

  @Override
  public void disable() {

  }

  @Override
  public ModuleInformation moduleInformation() {
    return ModuleInformation.create("Google", "", "",
      ModuleInformation.Type.HIDDEN);
  }
}
