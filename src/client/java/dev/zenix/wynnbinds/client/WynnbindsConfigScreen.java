package dev.zenix.wynnbinds.client;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class WynnbindsConfigScreen {

        public static Screen create(Screen parent) {
                ConfigBuilder builder = ConfigBuilder.create().setParentScreen(parent).setTitle(Component.nullToEmpty("Wynnbinds"));
                builder.setSavingRunnable(WynnbindsClient.getInstance()::saveConfig);

                WynnbindsConfig config = WynnbindsClient.getInstance().getConfig();
                ConfigEntryBuilder entryBuilder = builder.entryBuilder();

                // General
                ConfigCategory generalCategory = builder.getOrCreateCategory(Component.nullToEmpty("General"));
                generalCategory.addEntry(
                                entryBuilder.startBooleanToggle(Component.nullToEmpty("Wynnbinds"), config.isModEnabled())
                                                .setTooltip(Component.nullToEmpty("Enable or disable the mod")).setDefaultValue(true)
                                                .setSaveConsumer(value -> config.setEnableMod(value)).build());
                generalCategory.addEntry(entryBuilder
                                .startBooleanToggle(Component.nullToEmpty("Bind Notifications"),
                                                config.isBindNotificationsEnabled())
                                .setTooltip(Component.nullToEmpty("Enable or disable bind notifications")).setDefaultValue(true)
                                .setSaveConsumer(value -> config.setEnableBindNotifications(value)).build());
                generalCategory.addEntry(entryBuilder
                                .startBooleanToggle(Component.nullToEmpty("Update Notifications"),
                                                config.isUpdateNotificationsEnabled())
                                .setTooltip(Component.nullToEmpty("Enable or disable update notifications")).setDefaultValue(true)
                                .setSaveConsumer(value -> config.setEnableUpdateNotifications(value)).build());

                // Capture
                ConfigCategory captureKeysCategory = builder.getOrCreateCategory(Component.nullToEmpty("Capture"));
                HashMap<String, ArrayList<String>> allKeysByCategory = WynnbindsUtils.getAllKeysByCategory();

                for (Entry<String, ArrayList<String>> entry : allKeysByCategory.entrySet()) {
                        String category = entry.getKey();
                        ArrayList<String> translationKeys = entry.getValue();

                        Component categoryText = Component.translatable(category);
                        SubCategoryBuilder subCategory = entryBuilder.startSubCategory(categoryText);

                        subCategory.setTooltip(Component.nullToEmpty("Keys relating to " + categoryText.getString()));

                        for (String translationKey : translationKeys) {
                                Component keyText = Component.translatable(translationKey);

                                subCategory.add(entryBuilder
                                                .startBooleanToggle(keyText, config.isCaptureKey(translationKey))
                                                .setTooltip(Component.nullToEmpty(
                                                                "Enable or disable capture for " + keyText.getString()))
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
                ConfigCategory defaultKeysCategory = builder.getOrCreateCategory(Component.nullToEmpty("Default"));
                for (Entry<String, ArrayList<String>> entry : WynnbindsUtils.getCaptureKeysByCategory()
                                .entrySet()) {
                        String category = entry.getKey();
                        ArrayList<String> translationKeys = entry.getValue();

                        Component categoryText = Component.translatable(category);
                        SubCategoryBuilder subCategory = entryBuilder.startSubCategory(categoryText);

                        subCategory.setTooltip(Component.nullToEmpty("Keys relating to " + categoryText.getString()));

                        for (String translationKey : translationKeys) {
                                Component keyText = Component.translatable(translationKey);
                                InputConstants.Key currentKey = InputConstants
                                                .getKey(config.getDefaultKey(translationKey));
                                config.getDefaultKey(translationKey);
                                subCategory.add(entryBuilder.startKeyCodeField(keyText, currentKey)
                                                .setTooltip(Component.nullToEmpty(
                                                                String.format("Set default keybind for %s",
                                                                                keyText.getString())))
                                                .setDefaultValue(InputConstants.UNKNOWN).setKeySaveConsumer(value -> {
                                                        String boundKey = value.getName();
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
                        ConfigCategory currentKeysCategory = builder.getOrCreateCategory(Component.nullToEmpty("Current"));
                        for (Entry<String, ArrayList<String>> entry : WynnbindsUtils.getCaptureKeysByCategory()
                                        .entrySet()) {
                                String category = entry.getKey();
                                ArrayList<String> translationKeys = entry.getValue();

                                Component categoryText = Component.translatable(category);
                                SubCategoryBuilder subCategory = entryBuilder.startSubCategory(categoryText);

                                subCategory.setTooltip(Component.nullToEmpty("Keys relating to " + categoryText.getString()));

                                for (String translationKey : translationKeys) {
                                        InputConstants.Key currentKey = InputConstants
                                                        .getKey(config.getKey(currentCharacterId,
                                                                        translationKey));
                                        InputConstants.Key defaultKey = InputConstants
                                                        .getKey(config.getDefaultKey(translationKey));
                                        Component keyText = Component.translatable(translationKey);
                                        subCategory.add(entryBuilder.startKeyCodeField(keyText, currentKey)
                                                        .setTooltip(Component
                                                                        .nullToEmpty(String.format("Set keybind for %s",
                                                                                        keyText.getString())))
                                                        .setDefaultValue(defaultKey).setKeySaveConsumer(value -> {
                                                                // update our bind
                                                                String boundKey = value.getName();
                                                                config.setKey(currentCharacterId, translationKey,
                                                                                boundKey);

                                                                // update minecraft bind
                                                                KeyMapping keyBinding = KeyMapping.get(translationKey);
                                                                keyBinding.setKey(value);
                                                                WynnbindsUtils.refreshAndSaveKeyBindings();

                                                                // log
                                                                WynnbindsClient.LOGGER.debug(
                                                                                "character: {} translation: {} bound: {}",
                                                                                currentCharacterId, translationKey,
                                                                                boundKey);

                                                                // notify
                                                                WynnbindsUtils
                                                                                .sendNotification(
                                                                                                Component.nullToEmpty(String.format(
                                                                                                                "Updated keybind for %s",
                                                                                                                Component.translatable(
                                                                                                                                translationKey)
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