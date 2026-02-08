package dev.zenix.wynnbinds.client;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.wynntils.core.components.Models;
import com.wynntils.models.character.CharacterModel;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;

public class WynnbindsUtils {

    public static final String DUMMY_CHARACTER_ID = "-";

    public static String capitalizeStartOfEachWord(String input) {
        if (input == null || input.isEmpty())
            return input;

        String output = Pattern
                .compile("\\b\\p{L}")
                .matcher(input.toLowerCase())
                .replaceAll(match -> match.group().toUpperCase(Locale.ROOT));

        return output;
    }

    public static String getKeyName(String key) {
        if (key.equals("key.keyboard.unknown"))
            return "Unbound";

        if (key.startsWith("key.keyboard."))
            return capitalizeStartOfEachWord(key.substring("key.keyboard.".length()));

        if (key.startsWith("key.mouse."))
            return capitalizeStartOfEachWord(String.format("MOUSE BUTTON %s", key.substring("key.mouse.".length())));

        return "Unknown";
    }

    // A bit slower of method but more reliable
    public static String getCharacterId() {
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
            result
                    .computeIfAbsent(keyBinding.getCategory(), category -> new ArrayList<>())
                    .add(keyBinding.getTranslationKey());
        }

        return result;
    }

    public static HashMap<String, ArrayList<String>> getCaptureKeysByCategory() {
        var result = new HashMap<String, ArrayList<String>>();
        var config = WynnbindsClient.getInstance().getConfig();
        var keysByCategory = getAllKeysByCategory();

        for (var entry : keysByCategory.entrySet()) {
            var captureKeys = entry.getValue().stream()
                    .filter(config::isCaptureKey)
                    .collect(Collectors.toCollection(ArrayList::new));
            result.put(entry.getKey(), captureKeys);
        }

        return result;
    }
}
