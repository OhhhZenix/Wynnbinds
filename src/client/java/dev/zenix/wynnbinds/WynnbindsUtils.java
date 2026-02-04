package dev.zenix.wynnbinds;

public class WynnbindsUtils {

    public static String getKeyName(String key) {
        if (key == null || key.isEmpty())
            return "Unknown";

        if (key.startsWith("key.keyboard."))
            return key.substring("key.keyboard.".length()).toUpperCase();

        if (key.startsWith("key.mouse."))
            return String.format("MOUSE BUTTON %s", key.substring("key.mouse.".length()).toUpperCase());

        return "Unbound";
    }

}
