package dev.zenix.wynnbinds.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

public class WynnbindsModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            ConfigBuilder builder =
                    ConfigBuilder.create().setParentScreen(parent).setTitle(Text.of("Wynnbinds"));
            builder.setSavingRunnable(WynnbindsClient.getInstance()::saveConfig);

            var config = WynnbindsClient.getInstance().getConfig();
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
                    .setTooltip(Text.of("Enable or disable bind notifications"))
                    .setDefaultValue(true)
                    .setSaveConsumer(value -> config.setEnableBindNotifications(value)).build());
            generalCategory.addEntry(entryBuilder
                    .startBooleanToggle(Text.of("Update Notifications"),
                            config.isUpdateNotificationsEnabled())
                    .setTooltip(Text.of("Enable or disable update notifications"))
                    .setDefaultValue(true)
                    .setSaveConsumer(value -> config.setEnableUpdateNotifications(value)).build());

            // Capture
            ConfigCategory captureKeysCategory = builder.getOrCreateCategory(Text.of("Capture"));
            var allKeysByCategory = WynnbindsUtils.getAllKeysByCategory();

            for (var entry : allKeysByCategory.entrySet()) {
                String category = entry.getKey();
                var translationKeys = entry.getValue();

                Text categoryText = Text.translatable(category);
                SubCategoryBuilder subCategory = entryBuilder.startSubCategory(categoryText);

                subCategory.setTooltip(Text.of("Keys relating to " + categoryText.getString()));

                for (String translationKey : translationKeys) {
                    Text keyText = Text.translatable(translationKey);

                    subCategory.add(entryBuilder
                            .startBooleanToggle(keyText, config.isCaptureKey(translationKey))
                            .setTooltip(
                                    Text.of("Enable or disable capture for " + keyText.getString()))
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
            for (var entry : WynnbindsUtils.getCaptureKeysByCategory().entrySet()) {
                String category = entry.getKey();
                var translationKeys = entry.getValue();

                Text categoryText = Text.translatable(category);
                SubCategoryBuilder subCategory = entryBuilder.startSubCategory(categoryText);

                subCategory.setTooltip(Text.of("Keys relating to " + categoryText.getString()));

                for (String translationKey : translationKeys) {
                    Text keyText = Text.translatable(translationKey);
                    InputUtil.Key currentKey =
                            InputUtil.fromTranslationKey(config.getDefaultKey(translationKey));
                    config.getDefaultKey(translationKey);
                    subCategory.add(entryBuilder.startKeyCodeField(keyText, currentKey)
                            .setTooltip(Text.of(String.format("Set default keybind for %s",
                                    keyText.getString())))
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
            var currentCharacterId = WynnbindsUtils.getCharacterId();
            if (!currentCharacterId.equals(WynnbindsUtils.DUMMY_CHARACTER_ID)) {
                ConfigCategory currentKeysCategory =
                        builder.getOrCreateCategory(Text.of("Current"));
                for (var entry : WynnbindsUtils.getCaptureKeysByCategory().entrySet()) {
                    var category = entry.getKey();
                    var translationKeys = entry.getValue();

                    Text categoryText = Text.translatable(category);
                    SubCategoryBuilder subCategory = entryBuilder.startSubCategory(categoryText);

                    subCategory.setTooltip(Text.of("Keys relating to " + categoryText.getString()));

                    for (String translationKey : translationKeys) {
                        var currentKey = InputUtil.fromTranslationKey(
                                config.getKey(currentCharacterId, translationKey));
                        var defaultKey =
                                InputUtil.fromTranslationKey(config.getDefaultKey(translationKey));
                        var keyText = Text.translatable(translationKey);
                        subCategory.add(entryBuilder.startKeyCodeField(keyText, currentKey)
                                .setTooltip(Text.of(
                                        String.format("Set keybind for %s", keyText.getString())))
                                .setDefaultValue(defaultKey).setKeySaveConsumer(value -> {
                                    // update our bind
                                    var boundKey = value.getTranslationKey();
                                    config.setKey(currentCharacterId, translationKey, boundKey);

                                    // update minecraft bind
                                    var keyBinding = KeyBinding.byId(translationKey);
                                    keyBinding.setBoundKey(value);
                                    WynnbindsUtils.refreshKeyBindings();
                                    WynnbindsUtils.saveKeyBindings();

                                    WynnbindsClient.LOGGER.debug(
                                            "character: {} translation: {} bound: {}",
                                            currentCharacterId, translationKey, boundKey);
                                }).build());
                    }

                    currentKeysCategory.addEntry(subCategory.build());
                }
            }

            return builder.build();
        };
    }

}
