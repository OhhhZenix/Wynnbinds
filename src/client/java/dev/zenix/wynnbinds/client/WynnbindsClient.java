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
    private static final HashMap<String, String> SCAN_KEYS = new HashMap<>();

    // left is the key we are wanting to track, right is the display name for later.
    static {
        // Wynntils
        SCAN_KEYS.put("Cast 1st Spell", "Cast 1st Spell");
        SCAN_KEYS.put("Cast 2nd Spell", "Cast 2nd Spell");
        SCAN_KEYS.put("Cast 3rd Spell", "Cast 3rd Spell");
        SCAN_KEYS.put("Cast 4th Spell", "Cast 4th Spell");

        // Wynncraft Spell Caster
        SCAN_KEYS.put("key.wynncraft-spell-caster.spell.first", "First Spell");
        SCAN_KEYS.put("key.wynncraft-spell-caster.spell.second", "Second Spell");
        SCAN_KEYS.put("key.wynncraft-spell-caster.spell.third", "Third Spell");
        SCAN_KEYS.put("key.wynncraft-spell-caster.spell.fourth", "Fourth Spell");
        SCAN_KEYS.put("key.wynncraft-spell-caster.spell.melee", "Melee Attack");
        SCAN_KEYS.put("key.wynncraft-spell-caster.config", "Open Spell Caster Config");

        // BetterWynnMacros
        SCAN_KEYS.put("key.ktnwynnmacros.spell.1", "Spell 1");
        SCAN_KEYS.put("key.ktnwynnmacros.spell.2", "Spell 2");
        SCAN_KEYS.put("key.ktnwynnmacros.spell.3", "Spell 3");
        SCAN_KEYS.put("key.ktnwynnmacros.spell.4", "Spell 4");
    }

    private int tickCounter;
    private String oldCharacterId;
    private WynnbindsConfig config;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing Wynnbinds client mod");
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

    private void saveConfig() {
        LOGGER.debug("Saving configuration");
        AutoConfig.getConfigHolder(WynnbindsConfig.class).save();
    }

    private void loadKeys(MinecraftClient client, String newCharacterId) {
        if (!config.hasCharacter(newCharacterId)) {
            LOGGER.info("Setting up new character: {}", newCharacterId);
            config.setKeyBinds(newCharacterId, new HashMap<>());
            if (config.isNotificationsEnabled())
                MinecraftClient.getInstance().execute(() -> {
                    SystemToast.add(
                            MinecraftClient.getInstance().getToastManager(),
                            SystemToast.Type.WORLD_BACKUP,
                            Text.of("New Character"),
                            Text.of("Keybinds for character '" + newCharacterId + "' set up."));
                });
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
            MinecraftClient.getInstance().execute(() -> {
                SystemToast.add(
                        MinecraftClient.getInstance().getToastManager(),
                        SystemToast.Type.WORLD_BACKUP,
                        Text.of("Character Loaded"),
                        Text.of("Keybinds for character '" + newCharacterId + "' applied."));
            });
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
                LOGGER.debug("Detected new keybind: {} = {}", SCAN_KEYS.get(translationKey), newBoundKey);
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

            LOGGER.info("Updated keybind '{}' to '{}'", SCAN_KEYS.get(translationKey), newBoundKey);
            if (config.isNotificationsEnabled())
                MinecraftClient.getInstance().execute(() -> {
                    SystemToast.add(
                            MinecraftClient.getInstance().getToastManager(),
                            SystemToast.Type.WORLD_BACKUP,
                            Text.of("Character Updated"),
                            Text.of("Keybinds for character '" + newCharacterId + "' updated."));
                });
        }

        if (shouldSaveConfig)
            saveConfig();
    }

}
