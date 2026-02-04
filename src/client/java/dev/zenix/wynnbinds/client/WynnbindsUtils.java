package dev.zenix.wynnbinds;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.regex.Pattern;

import com.wynntils.core.components.Models;
import com.wynntils.models.character.CharacterModel;

import dev.zenix.wynnbinds.client.WynnbindsClient;

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

}
