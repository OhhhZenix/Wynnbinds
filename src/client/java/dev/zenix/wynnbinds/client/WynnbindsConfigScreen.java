package dev.zenix.wynnbinds.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

public class WynnbindsConfigScreen {

    public static Screen create(Screen parent) {
        ConfigBuilder builder =
                ConfigBuilder.create().setParentScreen(parent).setTitle(Text.of("Wynnbinds"));
        builder.setSavingRunnable(WynnbindsClient.getInstance()::saveConfig);

        WynnbindsConfig config = WynnbindsClient.getInstance().getConfig();
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        // General
        ConfigCategory generalCategory = builder.getOrCreateCategory(Text.of("General"));
        generalCategory.addEntry(
                entryBuilder.startBooleanToggle(Text.of("Wynnbinds"), config.isModEnabled())
                        .setTooltip(Text.of("Enable or disable the mod")).setDefaultValue(true)
                        .setSaveConsumer(value -> config.setEnableMod(value)).build());
        generalCategory.addEntry(entryBuilder
                .startBooleanToggle(Text.of("Bind Notifications"),
                        config.isBindNotificationsEnabled())
                .setTooltip(Text.of("Enable or disable bind notifications")).setDefaultValue(true)
                .setSaveConsumer(value -> config.setEnableBindNotifications(value)).build());
        generalCategory.addEntry(entryBuilder
                .startBooleanToggle(Text.of("Update Notifications"),
                        config.isUpdateNotificationsEnabled())
                .setTooltip(Text.of("Enable or disable update notifications")).setDefaultValue(true)
                .setSaveConsumer(value -> config.setEnableUpdateNotifications(value)).build());

        // Capture
        ConfigCategory captureKeysCategory = builder.getOrCreateCategory(Text.of("Capture"));
        HashMap<String, ArrayList<String>> allKeysByCategory =
                WynnbindsUtils.getAllKeysByCategory();

        for (Entry<String, ArrayList<String>> entry : allKeysByCategory.entrySet()) {
            String category = entry.getKey();
            ArrayList<String> translationKeys = entry.getValue();

            Text categoryText = Text.translatable(category);
            SubCategoryBuilder subCategory = entryBuilder.startSubCategory(categoryText);

            subCategory.setTooltip(Text.of("Keys relating to " + categoryText.getString()));

            for (String translationKey : translationKeys) {
                Text keyText = Text.translatable(translationKey);

                subCategory.add(entryBuilder
                        .startBooleanToggle(keyText, config.isCaptureKey(translationKey))
                        .setTooltip(Text.of("Enable or disable capture for " + keyText.getString()))
                        .setDefaultValue(false).setSaveConsumer(value -> {
                            if (value) {
                                config.addCaptureKey(translationKey);
                            } else {
                                config.removeCaptureKey(translationKey);
                            }
                        }).build());
            }

            captureKeysCategory.addEntry(subCategory.build());
        }

        // Default
        ConfigCategory defaultKeysCategory = builder.getOrCreateCategory(Text.of("Default"));
        for (Entry<String, ArrayList<String>> entry : WynnbindsUtils.getCaptureKeysByCategory()
                .entrySet()) {
            String category = entry.getKey();
            ArrayList<String> translationKeys = entry.getValue();

            Text categoryText = Text.translatable(category);
            SubCategoryBuilder subCategory = entryBuilder.startSubCategory(categoryText);

            subCategory.setTooltip(Text.of("Keys relating to " + categoryText.getString()));

            for (String translationKey : translationKeys) {
                Text keyText = Text.translatable(translationKey);
                InputUtil.Key currentKey =
                        InputUtil.fromTranslationKey(config.getDefaultKey(translationKey));
                config.getDefaultKey(translationKey);
                subCategory.add(entryBuilder.startKeyCodeField(keyText, currentKey)
                        .setTooltip(Text.of(
                                String.format("Set default keybind for %s", keyText.getString())))
                        .setDefaultValue(InputUtil.UNKNOWN_KEY).setKeySaveConsumer(value -> {
                            String boundKey = value.getTranslationKey();
                            WynnbindsClient.LOGGER.debug("Setting keybind for {} to {}",
                                    translationKey, boundKey);
                            config.setDefaultKey(translationKey, boundKey);
                        }).build());
            }

            defaultKeysCategory.addEntry(subCategory.build());
        }

        // Current
        String currentCharacterId = WynnbindsUtils.getCharacterId();
        if (!currentCharacterId.equals(WynnbindsUtils.DUMMY_CHARACTER_ID)) {
            ConfigCategory currentKeysCategory = builder.getOrCreateCategory(Text.of("Current"));
            for (Entry<String, ArrayList<String>> entry : WynnbindsUtils.getCaptureKeysByCategory()
                    .entrySet()) {
                String category = entry.getKey();
                ArrayList<String> translationKeys = entry.getValue();

                Text categoryText = Text.translatable(category);
                SubCategoryBuilder subCategory = entryBuilder.startSubCategory(categoryText);

                subCategory.setTooltip(Text.of("Keys relating to " + categoryText.getString()));

                for (String translationKey : translationKeys) {
                    InputUtil.Key currentKey = InputUtil
                            .fromTranslationKey(config.getKey(currentCharacterId, translationKey));
                    InputUtil.Key defaultKey =
                            InputUtil.fromTranslationKey(config.getDefaultKey(translationKey));
                    Text keyText = Text.translatable(translationKey);
                    subCategory.add(entryBuilder.startKeyCodeField(keyText, currentKey)
                            .setTooltip(Text
                                    .of(String.format("Set keybind for %s", keyText.getString())))
                            .setDefaultValue(defaultKey).setKeySaveConsumer(value -> {
                                // update our bind
                                String boundKey = value.getTranslationKey();
                                config.setKey(currentCharacterId, translationKey, boundKey);

                                // update minecraft bind
                                KeyBinding keyBinding = KeyBinding.byId(translationKey);
                                keyBinding.setBoundKey(value);
                                WynnbindsUtils.refreshAndSaveKeyBindings();

                                // log
                                WynnbindsClient.LOGGER.debug(
                                        "character: {} translation: {} bound: {}",
                                        currentCharacterId, translationKey, boundKey);

                                // notify
                                WynnbindsUtils
                                        .sendNotification(
                                                Text.of(String.format("Updated keybind for %s",
                                                        Text.translatable(translationKey)
                                                                .getString())),
                                                config.isBindNotificationsEnabled());
                            }).build());
                }

                currentKeysCategory.addEntry(subCategory.build());
            }
        }

        return builder.build();
    }
}
