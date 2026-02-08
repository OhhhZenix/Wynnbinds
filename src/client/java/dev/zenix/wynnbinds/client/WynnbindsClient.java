package dev.zenix.wynnbinds.client;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

public class WynnbindsClient implements ClientModInitializer {

    private static final String MOD_ID = "wynnbinds";
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
        ClientTickEvents.END_CLIENT_TICK.register(client -> onEndClientTick(client));
    }

    public Logger getLogger() {
        return logger;
    }

    private void loadLogger() {
        Configurator.setLevel(logger.getName(), Level.INFO);
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

        // Is it a valid character?
        if (currentCharacter.equals(WynnbindsUtils.DUMMY_CHARACTER_ID)) {
            return;
        }

        // Is it a new character?

    }

}
