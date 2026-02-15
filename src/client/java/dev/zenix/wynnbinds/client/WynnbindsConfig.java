package dev.zenix.wynnbinds.client;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import java.util.HashMap;
import java.util.HashSet;

@Config(name = "wynnbinds")
public class WynnbindsConfig implements ConfigData {

    private boolean enableMod = true;
    private boolean enableBindNotifications = true;
    private boolean enableUpdateNotifications = true;
    private HashSet<String> captureKeys = new HashSet<>();
    private HashMap<String, String> defaultKeys = new HashMap<>();
    private HashMap<String, HashMap<String, String>> characters = new HashMap<>();

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
        return characters.containsKey(characterId);
    }

    public String getKey(String characterId, String translationKey) {
        HashMap<String, String> keys = characters.get(characterId);
        return keys.get(translationKey);
    }

    public void setKey(String characterId, String translationKey, String boundKey) {
        HashMap<String, String> keys = characters.get(characterId);
        keys.put(translationKey, boundKey);
    }

    public HashMap<String, String> getKeys(String characterId) {
        return characters.get(characterId);
    }

    public void setKeys(String characterId, HashMap<String, String> keybinds) {
        characters.put(characterId, keybinds);
    }

    public boolean isCaptureKey(String translationKey) {
        return captureKeys.contains(translationKey);
    }

    public void addCaptureKey(String translationKey) {
        captureKeys.add(translationKey);
    }

    public void removeCaptureKey(String translationKey) {
        captureKeys.remove(translationKey);
    }

    public String getDefaultKey(String translationKey) {
        // if key does not exists, take current keybind
        if (!defaultKeys.containsKey(translationKey)) {
            KeyBinding keyBinding = KeyBinding.byId(translationKey);
            String boundKey = KeyBindingHelper.getBoundKeyOf(keyBinding).getTranslationKey();
            defaultKeys.put(translationKey, boundKey);
        }
        return defaultKeys.get(translationKey);
    }

    public HashMap<String, String> getDefaultKeys() {
        // return only the keys that are being captured
        HashMap<String, String> result = new HashMap<>();
        for (String translationKey : captureKeys) {
            String boundKey = getDefaultKey(translationKey);
            result.put(translationKey, boundKey);
        }
        return result;
    }

    public void setDefaultKey(String translationKey, String boundKey) {
        defaultKeys.put(translationKey, boundKey);
    }
}
