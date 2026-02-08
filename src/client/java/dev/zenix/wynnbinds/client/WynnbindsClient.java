package dev.zenix.wynnbinds.client;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

public class WynnbindsClient implements ClientModInitializer {

    public static final String MOD_ID = "wynnbinds";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
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
        Configurator.setLevel(LOGGER.getName(), Level.DEBUG);
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
                LOGGER.debug("Not an existing character. Using default keybinds.");
                config.setKeys(newCharacterId, config.getDefaultKeys());
                saveConfig();
            }

            // load keybinds
            for (KeyBinding keyBinding : WynnbindsUtils.getKeybindingsFromCaptureKeys()) {
                var translationKey = keyBinding.getTranslationKey();
                var boundKey = config.getDefaultKey(translationKey);
                var key = InputUtil.fromTranslationKey(boundKey);
                keyBinding.setBoundKey(key);
                LOGGER.debug("Loaded keybind for {}", translationKey);
            }

            // update
            KeyBinding.updateKeysByCode();
            LOGGER.debug("Refreshed keybinds.");

            // save
            client.options.write();
            LOGGER.debug("Saved keybinds.");

            // update tracking
            oldCharacterId = newCharacterId;
        }

        // from minecraft to wynnbinds
        // var keys = config.getKeys(newCharacterId);
        // for (KeyBinding keyBinding : WynnbindsUtils.getKeybindingsFromCaptureKeys())
        // {
        // String translationKey = keyBinding.getTranslationKey();

        // // Is it a new keybind?
        // if (keys.containsKey(translationKey)) {
        // var defaultKey = config.getDefaultKey(translationKey);
        // keys.put(translationKey, defaultKey);
        // shouldSaveConfig = true;
        // logger.debug("Detected new keybind: {} = {}", translationKey, defaultKey);
        // continue;
        // }

        // String newBoundKey = keyBinding.getBoundKeyTranslationKey();
        // String oldBoundKey = keys.get(translationKey);

        // // Has the mapping changed?
        // if (newBoundKey.equals(oldBoundKey)) {
        // continue;
        // }

        // keys.put(translationKey, newBoundKey);
        // shouldSaveConfig = true;

        // logger.debug("Updated keybind for '{}' from '{}' to '{}'", translationKey,
        // oldBoundKey, newBoundKey);
        // }

        // if (shouldSaveConfig) {
        // saveConfig();
        // }
    }

}
