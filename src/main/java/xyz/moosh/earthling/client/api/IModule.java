package xyz.moosh.earthling.client.api;

import xyz.moosh.earthling.client.config.ConfigGroup;
import xyz.moosh.earthling.client.module.Category;

public interface IModule {
    String getId();
    String getName();
    Category getCategory();
    boolean isEnabled();
    void setEnabled(boolean enabled);
    ConfigGroup getConfig();
    void onEnable();
    void onDisable();
}
