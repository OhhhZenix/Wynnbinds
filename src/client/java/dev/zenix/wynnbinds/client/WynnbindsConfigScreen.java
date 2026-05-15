package dev.zenix.wynnbinds.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.swing.text.JTextComponent.KeyBinding;

import com.mojang.blaze3d.platform.InputConstants;

import dev.zenix.wynnbinds.Wynnbinds;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class WynnbindsConfigScreen {

        public static Screen create(Screen parent) {
                ConfigBuilder builder = ConfigBuilder.create().setParentScreen(parent)
                                .setTitle(Component.literal("Wynnbinds"));
                builder.setSavingRunnable(WynnbindsClient.getInstance()::saveConfig);

                WynnbindsConfig config = WynnbindsClient.getInstance().getConfig();
                ConfigEntryBuilder entryBuilder = builder.entryBuilder();

                // General
                ConfigCategory generalCategory = builder.getOrCreateCategory(Component.literal("General"));
                generalCategory.addEntry(
                                entryBuilder.startBooleanToggle(Component.literal("Wynnbinds"), config.isModEnabled())
                                                .setTooltip(Component.literal("Enable or disable the mod"))
                                                .setDefaultValue(true)
                                                .setSaveConsumer(value -> config.setEnableMod(value)).build());
                generalCategory.addEntry(entryBuilder
                                .startBooleanToggle(Component.literal("Bind Notifications"),
                                                config.isBindNotificationsEnabled())
                                .setTooltip(Component.literal("Enable or disable bind notifications"))
                                .setDefaultValue(true)
                                .setSaveConsumer(value -> config.setEnableBindNotifications(value)).build());
                generalCategory.addEntry(entryBuilder
                                .startBooleanToggle(Component.literal("Update Notifications"),
                                                config.isUpdateNotificationsEnabled())
                                .setTooltip(Component.literal("Enable or disable update notifications"))
                                .setDefaultValue(true)
                                .setSaveConsumer(value -> config.setEnableUpdateNotifications(value)).build());

                // Capture
                ConfigCategory captureKeysCategory = builder.getOrCreateCategory(Component.literal("Capture"));
                HashMap<String, ArrayList<String>> allKeysByCategory = WynnbindsUtils.getAllKeysByCategory();

                for (Entry<String, ArrayList<String>> entry : allKeysByCategory.entrySet()) {
                        String category = entry.getKey();
                        ArrayList<String> translationKeys = entry.getValue();

                        Component categoryText = Component.translatable(category);
                        SubCategoryBuilder subCategory = entryBuilder.startSubCategory(categoryText);

                        subCategory.setTooltip(Component.literal("Keys relating to " + categoryText.getString()));

                        for (String translationKey : translationKeys) {
                                Component keyText = Component.translatable(translationKey);

                                subCategory.add(entryBuilder
                                                .startBooleanToggle(keyText, config.isCaptureKey(translationKey))
                                                .setTooltip(Component.literal(
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
                ConfigCategory defaultKeysCategory = builder.getOrCreateCategory(Component.literal("Default"));
                for (Entry<String, ArrayList<String>> entry : WynnbindsUtils.getCaptureKeysByCategory()
                                .entrySet()) {
                        String category = entry.getKey();
                        ArrayList<String> translationKeys = entry.getValue();

                        Component categoryText = Component.translatable(category);
                        SubCategoryBuilder subCategory = entryBuilder.startSubCategory(categoryText);

                        subCategory.setTooltip(Component.literal("Keys relating to " + categoryText.getString()));

                        for (String translationKey : translationKeys) {
                                Component keyText = Component.translatable(translationKey);
                                InputConstants.Key currentKey = KeyBindingHelper
                                                .getBoundKeyOf(KeyMapping.get(config.getDefaultKey(translationKey)));
                                config.getDefaultKey(translationKey);
                                subCategory.add(entryBuilder.startKeyCodeField(keyText, currentKey)
                                                .setTooltip(Component.literal(
                                                                String.format("Set default keybind for %s",
                                                                                keyText.getString())))
                                                .setDefaultValue(InputConstants.UNKNOWN).setKeySaveConsumer(value -> {
                                                        String boundKey = value.getName();
                                                        Wynnbinds.LOGGER.debug("Setting keybind for {} to {}",
                                                                        translationKey, boundKey);
                                                        config.setDefaultKey(translationKey, boundKey);
                                                }).build());
                        }

                        defaultKeysCategory.addEntry(subCategory.build());
                }

                // Current
                String currentCharacterId = WynnbindsUtils.getCharacterId();
                if (!currentCharacterId.equals(WynnbindsUtils.DUMMY_CHARACTER_ID)) {
                        ConfigCategory currentKeysCategory = builder.getOrCreateCategory(Component.literal("Current"));
                        for (Entry<String, ArrayList<String>> entry : WynnbindsUtils.getCaptureKeysByCategory()
                                        .entrySet()) {
                                String category = entry.getKey();
                                ArrayList<String> translationKeys = entry.getValue();

                                Component categoryText = Component.translatable(category);
                                SubCategoryBuilder subCategory = entryBuilder.startSubCategory(categoryText);

                                subCategory.setTooltip(
                                                Component.literal("Keys relating to " + categoryText.getString()));

                                for (String translationKey : translationKeys) {
                                        InputConstants.Key currentKey = InputUtil
                                                        .fromTranslationKey(config.getKey(currentCharacterId,
                                                                        translationKey));
                                        InputConstants.Key defaultKey = InputUtil
                                                        .fromTranslationKey(config.getDefaultKey(translationKey));
                                        Component keyText = Component.translatable(translationKey);
                                        subCategory.add(entryBuilder.startKeyCodeField(keyText, currentKey)
                                                        .setTooltip(Component
                                                                        .literal(String.format("Set keybind for %s",
                                                                                        keyText.getString())))
                                                        .setDefaultValue(defaultKey).setKeySaveConsumer(value -> {
                                                                // update our bind
                                                                String boundKey = value.getName();
                                                                config.setKey(currentCharacterId, translationKey,
                                                                                boundKey);

                                                                // update minecraft bind
                                                                KeyBinding keyBinding = KeyBinding.byId(translationKey);
                                                                keyBinding.setBoundKey(value);
                                                                WynnbindsUtils.refreshAndSaveKeyBindings();

                                                                // log
                                                                Wynnbinds.LOGGER.debug(
                                                                                "character: {} translation: {} bound: {}",
                                                                                currentCharacterId, translationKey,
                                                                                boundKey);

                                                                // notify
                                                                WynnbindsUtils
                                                                                .sendNotification(
                                                                                                Component.literal(String
                                                                                                                .format(
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
