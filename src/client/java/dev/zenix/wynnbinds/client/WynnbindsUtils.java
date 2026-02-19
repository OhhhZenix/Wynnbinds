package dev.zenix.wynnbinds.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
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

    public static String getCharacterId() {
        MinecraftClient client = MinecraftClient.getInstance();
        ServerInfo serverEntry = client.getCurrentServerEntry();

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

    public static HashMap<String, ArrayList<String>> getAllKeysByCategory() {
        HashMap<String, ArrayList<String>> result = new HashMap<>();

        for (KeyBinding keyBinding : MinecraftClient.getInstance().options.allKeys) {
            String category = keyBinding.getCategory().getLabel().getString();
            ArrayList<String> keys = result.get(category);

            // are the keys valid? if not, make empty list
            if (keys == null) {
                keys = new ArrayList<>();
                result.put(category, keys);
            }

            String translationKey = keyBinding.getId();
            keys.add(translationKey);
        }

        return result;
    }

    public static HashMap<String, ArrayList<String>> getCaptureKeysByCategory() {
        HashMap<String, ArrayList<String>> result = new HashMap<>();
        WynnbindsConfig config = WynnbindsClient.getInstance().getConfig();
        HashMap<String, ArrayList<String>> keysByCategory = getAllKeysByCategory();

        for (Entry<String, ArrayList<String>> entry : keysByCategory.entrySet()) {
            String category = entry.getKey();
            ArrayList<String> keys = entry.getValue();
            ArrayList<String> captureKeys = new ArrayList<>();

            for (String key : keys) {
                if (config.isCaptureKey(key)) {
                    captureKeys.add(key);
                }
            }

            if (!captureKeys.isEmpty()) {
                result.put(category, captureKeys);
            }
        }

        return result;
    }

    public static ArrayList<KeyBinding> getKeybindingsFromCaptureKeys() {
        ArrayList<KeyBinding> result = new ArrayList<>();
        for (KeyBinding keyBinding : MinecraftClient.getInstance().options.allKeys) {
            if (!WynnbindsClient.getInstance().getConfig().isCaptureKey(keyBinding.getId()))
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
                SystemToast.Type.WORLD_BACKUP, Text.of(WynnbindsClient.MOD_NAME), description);
    }
}
