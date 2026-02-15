package dev.zenix.wynnbinds.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

public class WynnbindsUtils {

    public static final String DUMMY_CHARACTER_ID = "-";
    private static final Pattern CHARACTER_ID_PATTERN = Pattern.compile("^[a-z0-9]{8}$");
    private static final int CHARACTER_INFO_SLOT = 7;
    private static final int CHARACTER_COLOR_CODE_LENGTH = 2;

    public static String capitalizeStartOfEachWord(String input) {
        if (input == null || input.isEmpty())
            return input;

        String output = Pattern.compile("\\b\\p{L}").matcher(input.toLowerCase())
                .replaceAll(match -> match.group().toUpperCase(Locale.ROOT));

        return output;
    }

    public static String getCharacterId() {
        var client = MinecraftClient.getInstance();
        var serverEntry = client.getCurrentServerEntry();

        if (serverEntry == null) {
            return DUMMY_CHARACTER_ID;
        }

        if (!serverEntry.address.toLowerCase().contains("wynncraft")) {
            return DUMMY_CHARACTER_ID;
        }

        if (client.player == null) {
            return DUMMY_CHARACTER_ID;
        }

        ItemStack compassItem = client.player.getInventory().getStack(CHARACTER_INFO_SLOT);
        if (compassItem == null) {
            return DUMMY_CHARACTER_ID;
        }

        List<Text> compassLore =
                compassItem.getOrDefault(DataComponentTypes.LORE, LoreComponent.DEFAULT).lines();
        if (compassLore.isEmpty()) {
            return DUMMY_CHARACTER_ID;
        }

        String idLine = compassLore.getFirst().getString().substring(CHARACTER_COLOR_CODE_LENGTH);
        if (idLine == null || !CHARACTER_ID_PATTERN.matcher(idLine).matches()) {
            WynnbindsClient.LOGGER.warn("Compass item had unexpected character ID line: " + idLine);
            return DUMMY_CHARACTER_ID;
        }

        return idLine;
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
            result.computeIfAbsent(keyBinding.getCategory().getLabel().getString(),
                    category -> new ArrayList<>()).add(keyBinding.getBoundKeyTranslationKey());
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
