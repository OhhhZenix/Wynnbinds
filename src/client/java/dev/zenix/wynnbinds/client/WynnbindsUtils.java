package dev.zenix.wynnbinds.client;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.wynntils.core.components.Models;
import com.wynntils.models.character.CharacterModel;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.Text;

public class WynnbindsUtils {

    public static final String DUMMY_CHARACTER_ID = "-";

    public static String capitalizeStartOfEachWord(String input) {
        if (input == null || input.isEmpty())
            return input;

        String output = Pattern.compile("\\b\\p{L}").matcher(input.toLowerCase())
                .replaceAll(match -> match.group().toUpperCase(Locale.ROOT));

        return output;
    }

    // A bit slower of method but more reliable
    public static String getCharacterId() {
        var serverEntry = MinecraftClient.getInstance().getCurrentServerEntry();

        if (serverEntry == null) {
            return DUMMY_CHARACTER_ID;
        }

        if (!serverEntry.address.toLowerCase().contains("wynncraft")) {
            return DUMMY_CHARACTER_ID;
        }

        try {
            // Get the field
            Field idField = CharacterModel.class.getDeclaredField("id");

            // Make it accessible (bypass private modifier)
            idField.setAccessible(true);

            // Get the value from an instance
            String id = (String) idField.get(Models.Character);

            return id;
        } catch (Exception e) {
            WynnbindsClient.LOGGER.error("Failed to retrieve character ID: {}", e.getMessage());
            return DUMMY_CHARACTER_ID;
        }
    }

    public static int compareSemver(String v1, String v2) {
        String[] a = v1.split("\\.");
        String[] b = v2.split("\\.");

        int len = Math.max(a.length, b.length);

        for (int i = 0; i < len; i++) {
            int n1 = i < a.length ? Integer.parseInt(a[i]) : 0;
            int n2 = i < b.length ? Integer.parseInt(b[i]) : 0;

            if (n1 != n2) {
                return Integer.compare(n1, n2);
            }
        }
        return 0;
    }

    public static HashMap<String, ArrayList<String>> getAllKeysByCategory() {
        var result = new HashMap<String, ArrayList<String>>();

        for (KeyBinding keyBinding : MinecraftClient.getInstance().options.allKeys) {
            result.computeIfAbsent(keyBinding.getCategory(), category -> new ArrayList<>())
                    .add(keyBinding.getTranslationKey());
        }

        return result;
    }

    public static HashMap<String, ArrayList<String>> getCaptureKeysByCategory() {
        var result = new HashMap<String, ArrayList<String>>();
        var config = WynnbindsClient.getInstance().getConfig();
        var keysByCategory = getAllKeysByCategory();

        for (var entry : keysByCategory.entrySet()) {
            var captureKeys = entry.getValue().stream().filter(config::isCaptureKey)
                    .collect(Collectors.toCollection(ArrayList::new));

            if (captureKeys.isEmpty())
                continue;

            result.put(entry.getKey(), captureKeys);
        }

        return result;
    }

    public static ArrayList<KeyBinding> getKeybindingsFromCaptureKeys() {
        ArrayList<KeyBinding> result = new ArrayList<>();
        for (KeyBinding keyBinding : MinecraftClient.getInstance().options.allKeys) {
            if (!WynnbindsClient.getInstance().getConfig()
                    .isCaptureKey(keyBinding.getTranslationKey()))
                continue;
            result.add(keyBinding);
        }
        return result;
    }

    public static void refreshKeyBindings() {
        KeyBinding.updateKeysByCode();
        WynnbindsClient.LOGGER.debug("Refreshed keybinds.");
    }

    public static void saveKeyBindings() {
        MinecraftClient.getInstance().options.write();
        WynnbindsClient.LOGGER.debug("Saved keybinds.");
    }

    public static void refreshAndSaveKeyBindings() {
        refreshKeyBindings();
        saveKeyBindings();
    }

    public static void sendNotification(Text description, Boolean shouldSend) {
        if (!shouldSend) {
            return;
        }

        SystemToast.add(MinecraftClient.getInstance().getToastManager(),
                SystemToast.Type.WORLD_BACKUP, Text.of("Wynnbinds"), description);
    }
}
