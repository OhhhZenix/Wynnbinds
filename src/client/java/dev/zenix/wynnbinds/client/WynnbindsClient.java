package dev.zenix.wynnbinds.client;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

public class WynnbindsClient implements ClientModInitializer {

    public static final String MOD_ID = "wynnbinds";
    private static WynnbindsClient instance = null;
    private Logger logger;
    private WynnbindsConfig config;

    public static WynnbindsClient getInstance() {
        return instance;
    }

    @Override
    public void onInitializeClient() {
        instance = this;
        loadLogger();
        loadConfig();
        startUpdateChecker();
        ClientTickEvents.END_CLIENT_TICK.register(client -> onEndClientTick(client));
    }

    private void startUpdateChecker() {
        Thread thread = new Thread(new WynnbindsUpdateChecker());
        thread.setDaemon(true);
        thread.start();
    }

    public Logger getLogger() {
        return logger;
    }

    private void loadLogger() {
        Configurator.setLevel(logger.getName(), Level.DEBUG);
        logger = LogManager.getLogger(MOD_ID);
    }

    public WynnbindsConfig getConfig() {
        return config;
    }

    public void saveConfig() {
        logger.debug("Saving configuration");
        AutoConfig.getConfigHolder(WynnbindsConfig.class).save();
    }

    private void loadConfig() {
        AutoConfig.register(WynnbindsConfig.class, GsonConfigSerializer::new);
        config = AutoConfig.getConfigHolder(WynnbindsConfig.class).getConfig();
        logger.info("Config loaded successfully");
    }

    private void onEndClientTick(MinecraftClient client) {
        String currentCharacter = WynnbindsUtils.getCharacterId();
        var shouldSaveConfig = false;

        // Is it a valid character?
        if (currentCharacter.equals(WynnbindsUtils.DUMMY_CHARACTER_ID)) {
            return;
        }

        // Is it a new character?
        if (!config.hasCharacter(currentCharacter)) {
            config.setKeys(currentCharacter, config.getDefaultKeys());
            shouldSaveConfig = true;
        }

        var keys = config.getKeys(currentCharacter);
        for (KeyBinding keyBinding : WynnbindsUtils.getKeybindingsFromCaptureKeys()) {
            String translationKey = keyBinding.getTranslationKey();

            // Is it a new keybind?
            if (keys.containsKey(translationKey)) {
                var defaultKey = config.getDefaultKey(translationKey);
                keys.put(translationKey, defaultKey);
                shouldSaveConfig = true;
                logger.debug("Detected new keybind: {} = {}", translationKey, defaultKey);
                continue;
            }

            String newBoundKey = keyBinding.getBoundKeyTranslationKey();
            String oldBoundKey = keys.get(translationKey);

            // Has the mapping changed?
            if (newBoundKey.equals(oldBoundKey)) {
                continue;
            }

            keys.put(translationKey, newBoundKey);
            shouldSaveConfig = true;

            logger.debug("Updated keybind for '{}' from '{}' to '{}'", translationKey, oldBoundKey, newBoundKey);
        }

        if (shouldSaveConfig) {
            saveConfig();

            // Force a refresh of the keybinding system
            logger.debug("Refreshing keybinding system");
            KeyBinding.updateKeysByCode();

            // Update the internal state of keybindings
            for (KeyBinding keyBinding : client.options.allKeys) {
                keyBinding.setPressed(false);
            }

            // Save changes to options.txt
            logger.debug("Writing keybindings to options.txt");
            client.options.write();
        }
    }

}
