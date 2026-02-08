package dev.zenix.wynnbinds.client;

import java.util.ArrayList;
import java.util.HashMap;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import it.unimi.dsi.fastutil.Hash;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

public class WynnbindsModMenu implements ModMenuApi {

        @Override
        public ConfigScreenFactory<?> getModConfigScreenFactory() {
                return parent -> {
                        ConfigBuilder builder = ConfigBuilder.create().setParentScreen(parent)
                                        .setTitle(Text.of("Wynnbinds"));
                        builder.setSavingRunnable(WynnbindsClient.getInstance()::saveConfig);

                        var config = WynnbindsClient.getInstance().getConfig();
                        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

                        // General
                        ConfigCategory generalCategory = builder.getOrCreateCategory(Text.of("General"));
                        generalCategory.addEntry(entryBuilder
                                        .startBooleanToggle(Text.of("Enable Mod"), config.isModEnabled())
                                        .setTooltip(Text.of("Enable or disable the Wynnbinds mod"))
                                        .setDefaultValue(true)
                                        .setSaveConsumer(value -> config.setEnableMod(value)).build());
                        generalCategory.addEntry(entryBuilder
                                        .startBooleanToggle(Text.of("Enable Bind Notifications"),
                                                        config.isBindNotificationsEnabled())
                                        .setTooltip(Text.of("Enable or disable bind notifications"))
                                        .setDefaultValue(true)
                                        .setSaveConsumer(value -> config.setEnableBindNotifications(value))
                                        .build());
                        generalCategory.addEntry(entryBuilder
                                        .startBooleanToggle(Text.of("Enable Update Notifications"),
                                                        config.isUpdateNotificationsEnabled())
                                        .setTooltip(Text.of("Enable or disable update notifications"))
                                        .setDefaultValue(true)
                                        .setSaveConsumer(value -> config.setEnableUpdateNotifications(value))
                                        .build());

                        builder.getOrCreateCategory(Text.of("Default Keybinds"));

                        // Capture
                        HashMap<String, ArrayList<String>> keysByCategories = new HashMap<>();
                        for (KeyBinding keyBinding : MinecraftClient.getInstance().options.allKeys) {
                                String category = keyBinding.getCategory();
                                if (!keysByCategories.containsKey(category)) {
                                        keysByCategories.put(category, new ArrayList<>());
                                }
                                keysByCategories.get(category).add(keyBinding.getTranslationKey());
                        }

                        ConfigCategory captureCategory = builder
                                        .getOrCreateCategory(Text.of("Capture"));
                        for (String category : keysByCategories.keySet()) {
                                SubCategoryBuilder keyCategory = entryBuilder
                                                .startSubCategory(Text.translatable(category));

                                for (String translationKey : keysByCategories.get(category)) {
                                        keyCategory.add(entryBuilder
                                                        .startBooleanToggle(Text.translatable(translationKey),
                                                                        config.isCaptureKeybind(translationKey))
                                                        .setDefaultValue(false)
                                                        .setSaveConsumer((value) -> {
                                                                if (value) {
                                                                        config.addCaptureKeybind(translationKey);
                                                                } else {
                                                                        config.removeCaptureKeybind(translationKey);
                                                                }
                                                        })
                                                        .build());
                                }

                                captureCategory.addEntry(keyCategory.build());
                        }

                        // Default Keybinds
                        ConfigCategory defaultKeyBindsCategory = builder
                                        .getOrCreateCategory(Text.of("Default Keybinds"));
                        for (Wynnbinds bind : Wynnbinds.values()) {
                                String translationKey = bind.getTranslationKey();
                                InputUtil.Key currentKey = InputUtil
                                                .fromTranslationKey(config.getDefaultKeyBind(bind.getTranslationKey()));
                                InputUtil.Key defaultKey = InputUtil.fromTranslationKey(bind.getDefaultBoundKey());
                                defaultKeyBindsCategory.addEntry(entryBuilder
                                                .startKeyCodeField(Text.of(bind.getDisplayName()), currentKey)
                                                .setTooltip(Text.of(String.format("Set default keybind for %s",
                                                                bind.getDisplayName())))
                                                .setDefaultValue(defaultKey)
                                                .setKeySaveConsumer(value -> {
                                                        String boundKey = value.getTranslationKey();
                                                        WynnbindsClient.LOGGER.debug(
                                                                        "Setting keybind for {} to {}",
                                                                        translationKey, boundKey);
                                                        config.setDefaultKeyBind(translationKey, boundKey);
                                                })
                                                .build());
                        }

                        return builder.build();
                };
        }

}
