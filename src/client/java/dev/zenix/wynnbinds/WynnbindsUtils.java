package dev.zenix.wynnbinds;

import java.util.Locale;
import java.util.regex.Pattern;

public class WynnbindsUtils {

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

}
