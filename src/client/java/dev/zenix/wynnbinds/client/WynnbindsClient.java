package dev.zenix.wynnbinds.client;

import com.wynntils.core.components.Models;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;

public class WynnbindsClient implements ClientModInitializer {

    public static final String MOD_ID = "wynnbinds";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    private static final String DUMMY_CHARACTER_ID = "-";
    private static final int CHECK_INTERVAL_TICKS = 20;
    private static final HashMap<String, WynnbindsMetadata> SCAN_KEYS = new HashMap<>();

    private static WynnbindsClient instance;

    // the keys we want to track
    static {
        // Wynntils
        SCAN_KEYS.put("Cast 1st Spell", new WynnbindsMetadata("Cast 1st Spell", "key.keyboard.z"));
        SCAN_KEYS.put("Cast 2nd Spell", new WynnbindsMetadata("Cast 2nd Spell", "key.keyboard.x"));
        SCAN_KEYS.put("Cast 3rd Spell", new WynnbindsMetadata("Cast 3rd Spell", "key.keyboard.c"));
        SCAN_KEYS.put("Cast 4th Spell", new WynnbindsMetadata("Cast 4th Spell", "key.keyboard.v"));

        // Wynncraft Spell Caster
        SCAN_KEYS.put("key.wynncraft-spell-caster.spell.first",
                new WynnbindsMetadata("First Spell", "key.keyboard.unknown"));
        SCAN_KEYS.put("key.wynncraft-spell-caster.spell.second",
                new WynnbindsMetadata("Second Spell", "key.keyboard.unknown"));
        SCAN_KEYS.put("key.wynncraft-spell-caster.spell.third",
                new WynnbindsMetadata("Third Spell", "key.keyboard.unknown"));
        SCAN_KEYS.put("key.wynncraft-spell-caster.spell.fourth",
                new WynnbindsMetadata("Fourth Spell", "key.keyboard.unknown"));
        SCAN_KEYS.put("key.wynncraft-spell-caster.spell.melee",
                new WynnbindsMetadata("Melee Spell", "key.keyboard.unknown"));
        SCAN_KEYS.put("key.wynncraft-spell-caster.config",
                new WynnbindsMetadata("Spell Caster Config", "key.keyboard.unknown"));

        // BetterWynnMacros
        SCAN_KEYS.put("key.ktnwynnmacros.spell.1", new WynnbindsMetadata("BetterWynnMacros Spell 1", "key.keyboard.r"));
        SCAN_KEYS.put("key.ktnwynnmacros.spell.2", new WynnbindsMetadata("BetterWynnMacros Spell 2", "key.keyboard.f"));
        SCAN_KEYS.put("key.ktnwynnmacros.spell.3", new WynnbindsMetadata("BetterWynnMacros Spell 3", "key.keyboard.v"));
        SCAN_KEYS.put("key.ktnwynnmacros.spell.4", new WynnbindsMetadata("BetterWynnMacros Spell 4", "key.keyboard.q"));
    }

    private int tickCounter;
    private String oldCharacterId;
    private WynnbindsConfig config;

    public static WynnbindsClient getInstance() {
        return instance;
    }

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing Wynnbinds client mod");
        instance = this;
        tickCounter = 0;
        oldCharacterId = DUMMY_CHARACTER_ID;

        AutoConfig.register(WynnbindsConfig.class, GsonConfigSerializer::new);
        config = AutoConfig.getConfigHolder(WynnbindsConfig.class).getConfig();
        LOGGER.info("Config loaded successfully");

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Is the feature enabled?
            if (!config.isModEnabled())
                return;

            // Should we check yet?
            if (tickCounter < CHECK_INTERVAL_TICKS) {
                tickCounter++;
                return;
            }
            tickCounter = 0;

            // Is the player valid?
            if (client.player == null)
                return;

            String newCharacterId = Models.Character.getId();

            // Is it a valid character?
            if (newCharacterId.equals(DUMMY_CHARACTER_ID))
                return;

            updateKeys(client, newCharacterId);
            oldCharacterId = newCharacterId;
        });
    }

    public WynnbindsConfig getConfig() {
        return config;
    }

    public void saveConfig() {
        LOGGER.debug("Saving configuration");
        AutoConfig.getConfigHolder(WynnbindsConfig.class).save();
    }

    private void loadKeys(MinecraftClient client, String newCharacterId) {
        if (!config.hasCharacter(newCharacterId)) {
            LOGGER.info("Setting up new character: {}", newCharacterId);
            config.setKeyBinds(newCharacterId, new HashMap<>());
            if (config.isNotificationsEnabled())
                SystemToast.add(
                        MinecraftClient.getInstance().getToastManager(),
                        SystemToast.Type.WORLD_BACKUP,
                        Text.of("New Character Detected"),
                        Text.of("Default keybinds created for character '" + newCharacterId + "'."));
            return;
        }

        LOGGER.info("Loading keybinds for character: {}", newCharacterId);
        HashMap<String, String> keyMappings = config.getKeyBinds(newCharacterId);
        for (String translationKey : keyMappings.keySet()) {
            String boundKey = keyMappings.get(translationKey);

            for (KeyBinding keyBinding : client.options.allKeys) {
                if (!keyBinding.getTranslationKey().equals(translationKey))
                    continue;

                InputUtil.Key actualKey = InputUtil.fromTranslationKey(boundKey);
                keyBinding.setBoundKey(actualKey);
            }
        }

        // Force a refresh of the keybinding system
        LOGGER.debug("Refreshing keybinding system");
        KeyBinding.updateKeysByCode();

        // Update the internal state of keybindings
        for (KeyBinding keyBinding : client.options.allKeys) {
            keyBinding.setPressed(false);
        }

        // Save changes to options.txt
        LOGGER.debug("Writing keybindings to options.txt");
        client.options.write();

        // Notify the player
        if (config.isNotificationsEnabled())
            SystemToast.add(
                    MinecraftClient.getInstance().getToastManager(),
                    SystemToast.Type.WORLD_BACKUP,
                    Text.of("Keybinds Applied"),
                    Text.of("Keybinds for character '" + newCharacterId + "' have been loaded and applied."));
    }

    private void updateKeys(MinecraftClient client, String newCharacterId) {
        // Is this a new character?
        if (!newCharacterId.equals(oldCharacterId)) {
            LOGGER.info("Character changed from '{}' to '{}'", oldCharacterId, newCharacterId);
            loadKeys(client, newCharacterId);
        }

        boolean shouldSaveConfig = false;

        for (KeyBinding keyBinding : client.options.allKeys) {
            // is this a keybind we care?
            if (!SCAN_KEYS.containsKey(keyBinding.getTranslationKey()))
                continue;

            HashMap<String, String> keyMappings = config.getKeyBinds(newCharacterId);
            String translationKey = keyBinding.getTranslationKey();
            String newBoundKey = keyBinding.getBoundKeyTranslationKey();

            // Is this a new mapping?
            if (!keyMappings.containsKey(translationKey)) {
                LOGGER.debug("Detected new keybind: {} = {}", translationKey, newBoundKey);
                keyMappings.put(translationKey, newBoundKey);
                shouldSaveConfig = true;
                continue;
            }

            String oldBoundKey = keyMappings.get(translationKey);

            // Has the mapping changed?
            if (newBoundKey.equals(oldBoundKey))
                continue;

            keyMappings.put(translationKey, newBoundKey);
            shouldSaveConfig = true;

            if (config.isNotificationsEnabled())
                SystemToast.add(
                        client.getToastManager(),
                        SystemToast.Type.WORLD_BACKUP,
                        Text.of("Keybinds Updated"),
                        Text.of(String.format("Updated '%s' from '%s' to '%s'",
                                SCAN_KEYS.get(translationKey).getDisplayName(), oldBoundKey, newBoundKey)));
            LOGGER.info("Updated keybind for '{}' from '{}' to '{}'", translationKey, oldBoundKey, newBoundKey);
        }

        if (shouldSaveConfig)
            saveConfig();
    }

}
