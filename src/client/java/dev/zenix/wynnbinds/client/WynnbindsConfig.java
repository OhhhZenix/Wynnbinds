package dev.zenix.wynnbinds.client;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

import java.util.HashMap;

@Config(name = "wynnbinds")
public class WynnbindsConfig implements ConfigData {

    private boolean enableMod = true;
    private boolean enableBindNotifications = true;
    private boolean enableUpdateNotifications = true;
    private HashMap<String, String> defaultKeyBinds = Wynnbinds.getDefaultKeyBinds();

    @ConfigEntry.Gui.Excluded
    private HashMap<String, HashMap<String, String>> characterMappings = new HashMap<>();

    public boolean isModEnabled() {
        return enableMod;
    }

    public void setEnableMod(boolean enabled) {
        enableMod = enabled;
    }

    public boolean isBindNotificationsEnabled() {
        return enableBindNotifications;
    }

    public void setEnableBindNotifications(boolean enabled) {
        enableBindNotifications = enabled;
    }

    public boolean isUpdateNotificationsEnabled() {
        return enableUpdateNotifications;
    }

    public void setEnableUpdateNotifications(boolean enabled) {
        enableUpdateNotifications = enabled;
    }

    public boolean hasCharacter(String characterId) {
        return characterMappings.containsKey(characterId);
    }

    public HashMap<String, String> getKeyBinds(String characterId) {
        return characterMappings.get(characterId);
    }

    public void setKeyBinds(String characterId, HashMap<String, String> keybinds) {
        characterMappings.put(characterId, keybinds);
    }

    public String getDefaultKeyBind(String translationKey) {
        return defaultKeyBinds.get(translationKey);
    }

    public HashMap<String, String> getDefaultKeyBinds() {
        return defaultKeyBinds;
    }

    public void setDefaultKeyBind(String translationKey, String boundKey) {
        defaultKeyBinds.put(translationKey, boundKey);
    }
}