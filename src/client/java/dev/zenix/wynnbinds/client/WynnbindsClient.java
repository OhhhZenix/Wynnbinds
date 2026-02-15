package dev.zenix.wynnbinds.client;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.KeyBinding.Category;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.lwjgl.glfw.GLFW;

public class WynnbindsClient implements ClientModInitializer {

    public static final String MOD_ID = "wynnbinds";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    private static final Category KEY_CATEGORY = Category.create(Identifier.of(MOD_ID, "all"));
    private static final KeyBinding OPEN_CONFIG_KEYBINDING =
            KeyBindingHelper.registerKeyBinding(new KeyBinding("key.wynnbinds.config",
                    InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, KEY_CATEGORY));
    private static WynnbindsClient instance = null;
    private WynnbindsConfig config = null;
    private String oldCharacterId = WynnbindsUtils.DUMMY_CHARACTER_ID;

    public static WynnbindsClient getInstance() {
        return instance;
    }

    @Override
    public void onInitializeClient() {
        instance = this;
        setupLogger();
        loadConfig();
        startUpdateChecker();
        ClientTickEvents.END_CLIENT_TICK.register(client -> onEndClientTick(client));
    }

    private void setupLogger() {
        Configurator.setLevel(LOGGER.getName(), Level.INFO);
    }

    private void startUpdateChecker() {
        Thread thread = new Thread(new WynnbindsUpdateChecker());
        thread.setDaemon(true);
        thread.start();
    }

    public WynnbindsConfig getConfig() {
        return config;
    }

    public void saveConfig() {
        LOGGER.debug("Saving configuration");
        AutoConfig.getConfigHolder(WynnbindsConfig.class).save();
    }

    private void loadConfig() {
        AutoConfig.register(WynnbindsConfig.class, GsonConfigSerializer::new);
        config = AutoConfig.getConfigHolder(WynnbindsConfig.class).getConfig();
        LOGGER.info("Config loaded successfully");
    }

    private void onEndClientTick(MinecraftClient client) {
        handleOpenConfig(client);
        handleKeybinds(client);
    }

    private void handleOpenConfig(MinecraftClient client) {
        if (OPEN_CONFIG_KEYBINDING.isPressed()) {
            OPEN_CONFIG_KEYBINDING.setPressed(false);
            client.setScreen(WynnbindsConfigScreen.create(client.currentScreen));
        }
    }

    private void handleKeybinds(MinecraftClient client) {
        var newCharacterId = WynnbindsUtils.getCharacterId();

        // Is it a valid character?
        if (newCharacterId.equals(WynnbindsUtils.DUMMY_CHARACTER_ID)) {
            return;
        }

        // Is it a new character?
        if (!oldCharacterId.equals(newCharacterId)) {
            LOGGER.debug("Character changed from '{}' to '{}'", oldCharacterId, newCharacterId);

            // Is it an existing character?
            if (!config.hasCharacter(newCharacterId)) {
                // log
                LOGGER.debug("Not an existing character. Using default keybinds.");

                // update & save
                config.setKeys(newCharacterId, config.getDefaultKeys());
                saveConfig();

                // notify
                WynnbindsUtils.sendNotification(
                        Text.of(String.format("Creating new profile for %s", newCharacterId)),
                        config.isBindNotificationsEnabled());
            }

            // load keybinds
            for (var keyBinding : WynnbindsUtils.getKeybindingsFromCaptureKeys()) {
                var translationKey = keyBinding.getId();
                var boundKey = config.getKey(newCharacterId, translationKey);
                var key = InputUtil.fromTranslationKey(boundKey);
                keyBinding.setBoundKey(key);
                LOGGER.debug("Loaded keybind for {}", translationKey);
            }

            // refresh & save binds
            WynnbindsUtils.refreshAndSaveKeyBindings();

            // notify
            WynnbindsUtils.sendNotification(
                    Text.of(String.format("Loaded keybinds for %s", newCharacterId)),
                    config.isBindNotificationsEnabled());
        }

        LOGGER.debug("Scanning for keybind changes.");
        var keys = config.getKeys(newCharacterId);
        var shouldSaveConfig = false;
        for (var keyBinding : WynnbindsUtils.getKeybindingsFromCaptureKeys()) {
            var translationKey = keyBinding.getId();

            // Is it an exisiting keybind?
            if (!keys.containsKey(translationKey)) {
                LOGGER.debug("Missing keybind for {}", translationKey);
                var boundKey = config.getDefaultKey(translationKey);
                keys.put(translationKey, boundKey);
                LOGGER.debug("Set {} keybind as {}", translationKey, boundKey);
                continue;
            }

            var newBoundKey = keyBinding.getBoundKeyTranslationKey();
            var oldBoundKey = keys.get(translationKey);

            // Is it a different key?
            if (oldBoundKey.equals(newBoundKey)) {
                LOGGER.debug("Keybind for {} has not changed yet.", translationKey);
                continue;
            }

            // update & save
            keys.put(translationKey, newBoundKey);
            shouldSaveConfig = true;

            // log
            LOGGER.debug("Updated keybind for {} from {} to {}", translationKey, oldBoundKey,
                    newBoundKey);

            // notify
            WynnbindsUtils.sendNotification(
                    Text.of(String.format("Updated keybind for %s",
                            Text.translatable(translationKey).getString())),
                    config.isBindNotificationsEnabled());
        }

        if (shouldSaveConfig) {
            saveConfig();
        }

        // update tracking
        oldCharacterId = newCharacterId;
    }
}
