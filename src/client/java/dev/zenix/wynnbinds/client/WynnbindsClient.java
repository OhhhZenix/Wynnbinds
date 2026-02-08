package dev.zenix.wynnbinds.client;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import com.google.gson.Gson;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;

public class WynnbindsClient implements ClientModInitializer {

    public static final String MOD_ID = "wynnbinds";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    private static final int CHECK_INTERVAL_TICKS = 20;
    private static final int UPDATE_CHECK_INTERVAL_MS = 3_600_000; // 1 hour
    private static WynnbindsClient instance = null;

    private int tickCounter = 0;
    private String oldCharacterId = WynnbindsUtils.DUMMY_CHARACTER_ID;
    private WynnbindsConfig config = null;
    private long lastUpdateCheckTime = 0;

    public static WynnbindsClient getInstance() {
        return instance;
    }

    @Override
    public void onInitializeClient() {
        Configurator.setLevel(LOGGER.getName(), Level.INFO);

        LOGGER.info("Initializing Wynnbinds client mod");
        instance = this;

        AutoConfig.register(WynnbindsConfig.class, GsonConfigSerializer::new);
        config = AutoConfig.getConfigHolder(WynnbindsConfig.class).getConfig();
        LOGGER.info("Config loaded successfully");

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Should we check yet?
            if (tickCounter < CHECK_INTERVAL_TICKS) {
                tickCounter++;
                return;
            }
            tickCounter = 0;

            // Is the player valid?
            if (client.player == null)
                return;

            // Check for updates
            if (config.isUpdateNotificationsEnabled() && shouldCheckForUpdates())
                checkForUpdates(client);

            // Is the feature enabled?
            if (!config.isModEnabled())
                return;

            String newCharacterId = WynnbindsUtils.getCharacterId();
            LOGGER.debug("Checking character ID: old='{}', new='{}'", oldCharacterId, newCharacterId);

            // Is it a valid character?
            if (newCharacterId.equals(WynnbindsUtils.DUMMY_CHARACTER_ID))
                return;

            applyKeys(client, newCharacterId);
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

    private boolean shouldCheckForUpdates() {
        return System.currentTimeMillis() - lastUpdateCheckTime > UPDATE_CHECK_INTERVAL_MS;
    }

    private void checkForUpdates(MinecraftClient client) {
        // update timer
        lastUpdateCheckTime = System.currentTimeMillis();

        // try to fetch latest version from GitHub API
        try {
            final String API_URL = "https://api.github.com/repos/OhhhZenix/Wynnbinds/releases/latest";
            HttpClient httpClient = HttpClient.newHttpClient();
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Accept", "application/json")
                    .build();
            HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (httpResponse.statusCode() != 200) {
                System.out.println("Failed to fetch. Status code: " + httpResponse.statusCode());
                return;
            }

            String body = httpResponse.body();
            Gson gson = new Gson();
            var json = gson.fromJson(body, HashMap.class);
            String latestVersion = (String) json.get("tag_name");
            String currentVersion = FabricLoader.getInstance().getModContainer(MOD_ID)
                    .map(modContainer -> modContainer.getMetadata().getVersion().getFriendlyString())
                    .orElse("0.0.0");
            String homepageUrl = FabricLoader.getInstance().getModContainer(MOD_ID)
                    .flatMap(modContainer -> modContainer.getMetadata().getContact().get("homepage"))
                    .orElse("https://github.com/OhhhZenix/Wynnbinds");

            if (WynnbindsUtils.compareSemver(latestVersion, currentVersion) > 0)
                client.player.sendMessage(
                        Text.of(String.format(
                                "Wynnbinds v%s is now available. You're running v%s. Visit %s to download.",
                                latestVersion, currentVersion, homepageUrl)),
                        false);
        } catch (Exception e) {
            LOGGER.warn("Failed to check for updates", e);
        }
    }

    private void loadKeys(MinecraftClient client, String newCharacterId) {
        // Is it a new character?
        if (!config.hasCharacter(newCharacterId)) {
            LOGGER.info("Setting up new character: {}", newCharacterId);

            config.setKeys(newCharacterId, config.getDefaultKeys());
            saveConfig();

            if (config.isBindNotificationsEnabled())
                SystemToast.add(
                        MinecraftClient.getInstance().getToastManager(),
                        SystemToast.Type.WORLD_BACKUP,
                        Text.of("New Character Detected"),
                        Text.of("Default keybinds created for character '" + newCharacterId + "'."));
        }

        LOGGER.info("Loading keybinds for character: {}", newCharacterId);

        HashMap<String, String> keyMappings = config.getKeys(newCharacterId);
        for (String translationKey : keyMappings.keySet()) {
            String boundKey = keyMappings.get(translationKey);

            for (KeyBinding keyBinding : client.options.allKeys) {
                if (!keyBinding.getTranslationKey().equals(translationKey))
                    continue;

                InputUtil.Key actualKey = InputUtil.fromTranslationKey(boundKey);
                keyBinding.setBoundKey(actualKey);
            }
        }

        if (config.isBindNotificationsEnabled())
            SystemToast.add(
                    MinecraftClient.getInstance().getToastManager(),
                    SystemToast.Type.WORLD_BACKUP,
                    Text.of("Keybinds Applied"),
                    Text.of("Keybinds for character '" + newCharacterId + "' have been loaded and applied."));

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
    }

    private void applyKeys(MinecraftClient client, String newCharacterId) {
        // Is it a different character?
        if (!newCharacterId.equals(oldCharacterId)) {
            LOGGER.info("Character changed from '{}' to '{}'", oldCharacterId, newCharacterId);
            loadKeys(client, newCharacterId);
        }

        boolean shouldSaveConfig = false;
        for (KeyBinding keyBinding : WynnbindsUtils.getKeybindingsFromCaptureKeys()) {
            HashMap<String, String> keyMappings = config.getKeys(newCharacterId);
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

            if (config.isBindNotificationsEnabled()) {
                Text categoryText = Text.translatable(keyBinding.getCategory());
                Text keyText = Text.translatable(keyBinding.getTranslationKey());
                SystemToast.add(
                        client.getToastManager(),
                        SystemToast.Type.WORLD_BACKUP,
                        Text.of("Keybinds Updated"),
                        Text.of(String.format("Updated '%s (%s)' from '%s' to '%s' and saved configurations.",
                                keyText.getString(), categoryText.getString(), WynnbindsUtils.getKeyName(oldBoundKey),
                                WynnbindsUtils.getKeyName(newBoundKey))));
            }
            LOGGER.info("Updated keybind for '{}' from '{}' to '{}'", translationKey, oldBoundKey, newBoundKey);
        }

        if (shouldSaveConfig)
            saveConfig();
    }

}
