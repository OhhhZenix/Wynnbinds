package dev.zenix.wynnbinds.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.zenix.wynnbinds.Wynnbinds;
import dev.zenix.wynnbinds.client.config.ConfigScreen;
import dev.zenix.wynnbinds.client.config.ModConfig;
import dev.zenix.wynnbinds.client.core.UpdateChecker;
import dev.zenix.wynnbinds.client.core.Utils;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.KeyMapping.Category;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class WynnbindsClient implements ClientModInitializer {

  private static final Category KEY_CATEGORY =
      Category.register(Identifier.fromNamespaceAndPath(Wynnbinds.MOD_ID, "all"));

  private static final KeyMapping OPEN_CONFIG_KEYBINDING =
      KeyBindingHelper.registerKeyBinding(
          new KeyMapping(
              "key.wynnbinds.config",
              InputConstants.Type.KEYSYM,
              InputConstants.UNKNOWN.getValue(),
              KEY_CATEGORY));

  private static final AtomicBoolean running = new AtomicBoolean(true);
  private static WynnbindsClient instance = null;

  private ModConfig config = null;
  private String oldCharacterId = Utils.DUMMY_CHARACTER_ID;

  public static boolean isRunning() {
    return running.get();
  }

  public static WynnbindsClient getInstance() {
    return instance;
  }

  @Override
  public void onInitializeClient() {
    instance = this;
    ClientLifecycleEvents.CLIENT_STARTED.register(this::onClientStart);
    ClientLifecycleEvents.CLIENT_STOPPING.register(this::onClientStop);
    ClientTickEvents.END_CLIENT_TICK.register(this::handleKeybinds);
    ClientTickEvents.END_CLIENT_TICK.register(this::handleOpenConfig);
  }

  public ModConfig getConfig() {
    return config;
  }

  public void saveConfig() {
    Wynnbinds.LOGGER.debug("Saving configuration");
    AutoConfig.getConfigHolder(ModConfig.class).save();
  }

  private void loadConfig() {
    AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
    config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
    Wynnbinds.LOGGER.info("Config loaded successfully");
  }

  private void onClientStart(Minecraft client) {
    loadConfig();

    UpdateChecker updateChecker = new UpdateChecker();
    updateChecker.start();
  }

  private void onClientStop(Minecraft client) {
    running.set(false);
  }

  private void handleOpenConfig(Minecraft client) {
    if (OPEN_CONFIG_KEYBINDING.isDown()) {
      OPEN_CONFIG_KEYBINDING.setDown(false);
      client.setScreenAndShow(ConfigScreen.create(client.screen));
    }
  }

  private void handleKeybinds(Minecraft client) {
    String newCharacterId = Utils.getCharacterId();

    // Is it a valid character?
    if (newCharacterId.equals(Utils.DUMMY_CHARACTER_ID)) {
      return;
    }

    // Is it a new character?
    if (!oldCharacterId.equals(newCharacterId)) {
      Wynnbinds.LOGGER.debug("Character changed from '{}' to '{}'", oldCharacterId, newCharacterId);

      // Is it an existing character?
      if (!config.hasCharacter(newCharacterId)) {
        // log
        Wynnbinds.LOGGER.debug("Not an existing character. Using default keybinds.");

        // update & save
        config.setKeys(newCharacterId, config.getDefaultKeys());
        saveConfig();

        // notify
        Utils.sendNotification(
            Component.nullToEmpty(String.format("Creating new profile for %s", newCharacterId)),
            config.isBindNotificationsEnabled());
      }

      // load keybinds
      for (KeyMapping keyBinding : Utils.getKeybindingsFromCaptureKeys()) {
        String translationKey = keyBinding.getName();
        String boundKey = config.getKey(newCharacterId, translationKey);
        InputConstants.Key key = InputConstants.getKey(boundKey);
        keyBinding.setKey(key);
        Wynnbinds.LOGGER.debug("Loaded keybind for {}", translationKey);
      }

      // refresh & save binds
      Utils.refreshAndSaveKeyBindings();

      // notify
      Utils.sendNotification(
          Component.nullToEmpty(String.format("Loaded keybinds for %s", newCharacterId)),
          config.isBindNotificationsEnabled());
    }

    Wynnbinds.LOGGER.debug("Scanning for keybind changes.");
    HashMap<String, String> keys = config.getKeys(newCharacterId);
    boolean shouldSaveConfig = false;
    for (KeyMapping keyBinding : Utils.getKeybindingsFromCaptureKeys()) {
      String translationKey = keyBinding.getName();

      // Is it an exisiting keybind?
      if (!keys.containsKey(translationKey)) {
        Wynnbinds.LOGGER.debug("Missing keybind for {}", translationKey);
        String boundKey = config.getDefaultKey(translationKey);
        keys.put(translationKey, boundKey);
        Wynnbinds.LOGGER.debug("Set {} keybind as {}", translationKey, boundKey);
        continue;
      }

      String newBoundKey = keyBinding.saveString();
      String oldBoundKey = keys.get(translationKey);

      // Is it a different key?
      if (oldBoundKey.equals(newBoundKey)) {
        Wynnbinds.LOGGER.debug("Keybind for {} has not changed yet.", translationKey);
        continue;
      }

      // update & save
      keys.put(translationKey, newBoundKey);
      shouldSaveConfig = true;

      // log
      Wynnbinds.LOGGER.debug(
          "Updated keybind for {} from {} to {}", translationKey, oldBoundKey, newBoundKey);

      // notify
      Utils.sendNotification(
          Component.nullToEmpty(
              String.format(
                  "Updated keybind for %s", Component.translatable(translationKey).getString())),
          config.isBindNotificationsEnabled());
    }

    if (shouldSaveConfig) {
      saveConfig();
    }

    // update tracking
    oldCharacterId = newCharacterId;
  }
}
