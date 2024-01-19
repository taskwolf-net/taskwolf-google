package net.taskwolf.google;

import net.taskwolf.core.CoreModule;
import net.taskwolf.core.module.Module;
import net.taskwolf.core.module.ModuleDescription;
import net.taskwolf.core.module.ModuleInformation;
import net.taskwolf.core.module.ModuleLoadPriority;

@ModuleDescription(name = "google", version = "1.0.0-SNAPSHOT",
  priority = ModuleLoadPriority.NEUTRAL)
public final class GoogleModule extends Module {
  public GoogleModule(CoreModule coreModule) {
    super(coreModule);
  }

  @Override
  public void enable() throws Exception {

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
