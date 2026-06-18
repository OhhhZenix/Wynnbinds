package dev.zenix.wynnbinds.client;

import java.util.HashMap;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import dev.zenix.wynnbinds.Wynnbinds;
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

    private static final Category KEY_CATEGORY = Category
            .register(Identifier.fromNamespaceAndPath(Wynnbinds.MOD_ID, "all"));

    private static final KeyMapping OPEN_CONFIG_KEYBINDING = KeyBindingHelper
            .registerKeyBinding(new KeyMapping("key.wynnbinds.config",
                    InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, KEY_CATEGORY));

    private static WynnbindsClient instance = null;

    private ClothConfig config = null;
    private UpdateChecker updateChecker = null;
    private String oldCharacterId = Utils.DUMMY_CHARACTER_ID;

    public static WynnbindsClient getInstance() {
        return instance;
    }

    @Override
    public void onInitializeClient() {
        instance = this;
        loadConfig();
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> onClientStart(client));
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> onClientStop(client));
        ClientTickEvents.END_CLIENT_TICK.register(client -> onEndClientTick(client));
    }

    public ClothConfig getConfig() {
        return config;
    }

    public void saveConfig() {
        Wynnbinds.LOGGER.debug("Saving configuration");
        AutoConfig.getConfigHolder(ClothConfig.class).save();
    }

    private void loadConfig() {
        AutoConfig.register(ClothConfig.class, GsonConfigSerializer::new);
        config = AutoConfig.getConfigHolder(ClothConfig.class).getConfig();
        Wynnbinds.LOGGER.info("Config loaded successfully");
    }

    private void onClientStart(Minecraft client) {
        updateChecker = new UpdateChecker();
        updateChecker.start();
    }

    private void onClientStop(Minecraft client) {
        updateChecker.stop();
    }

    private void onEndClientTick(Minecraft client) {
        handleOpenConfig(client);
        handleKeybinds(client);
    }

    private void handleOpenConfig(Minecraft client) {
        if (OPEN_CONFIG_KEYBINDING.isDown()) {
            OPEN_CONFIG_KEYBINDING.setDown(false);
            client.setScreen(ConfigScreen.create(client.screen));
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
            Wynnbinds.LOGGER.debug("Updated keybind for {} from {} to {}", translationKey, oldBoundKey,
                    newBoundKey);

            // notify
            Utils.sendNotification(
                    Component.nullToEmpty(String.format("Updated keybind for %s",
                            Component.translatable(translationKey).getString())),
                    config.isBindNotificationsEnabled());
        }

        if (shouldSaveConfig) {
            saveConfig();
        }

        // update tracking
        oldCharacterId = newCharacterId;
    }
}
