package dev.zenix.wynnbinds.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import com.wynntils.utils.wynn.WynnUtils;

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
                                        .startBooleanToggle(Text.of("Wynnbinds"), config.isModEnabled())
                                        .setTooltip(Text.of("Enable or disable the mod"))
                                        .setDefaultValue(true)
                                        .setSaveConsumer(value -> config.setEnableMod(value)).build());
                        generalCategory.addEntry(entryBuilder
                                        .startBooleanToggle(Text.of("Bind Notifications"),
                                                        config.isBindNotificationsEnabled())
                                        .setTooltip(Text.of("Enable or disable bind notifications"))
                                        .setDefaultValue(true)
                                        .setSaveConsumer(value -> config.setEnableBindNotifications(value))
                                        .build());
                        generalCategory.addEntry(entryBuilder
                                        .startBooleanToggle(Text.of("Update Notifications"),
                                                        config.isUpdateNotificationsEnabled())
                                        .setTooltip(Text.of("Enable or disable update notifications"))
                                        .setDefaultValue(true)
                                        .setSaveConsumer(value -> config.setEnableUpdateNotifications(value))
                                        .build());

                        // Capture
                        ConfigCategory captureCategory = builder.getOrCreateCategory(Text.of("Capture"));
                        // TODO: add description
                        var keysByCategory = WynnbindsUtils.getAllKeysByCategory();

                        for (var entry : keysByCategory.entrySet()) {
                                String category = entry.getKey();
                                var translationKeys = entry.getValue();

                                Text categoryText = Text.translatable(category);
                                SubCategoryBuilder subCategory = entryBuilder.startSubCategory(categoryText);

                                subCategory.setTooltip(
                                                Text.of("Keys relating to " + categoryText.getString()));

                                for (String translationKey : translationKeys) {
                                        Text keyText = Text.translatable(translationKey);

                                        subCategory.add(
                                                        entryBuilder
                                                                        .startBooleanToggle(
                                                                                        keyText,
                                                                                        config.isCaptureKey(
                                                                                                        translationKey))
                                                                        .setTooltip(Text.of(
                                                                                        "Enable or disable capture for "
                                                                                                        + keyText.getString()))
                                                                        .setDefaultValue(false)
                                                                        .setSaveConsumer(value -> {
                                                                                if (value) {
                                                                                        config.addCaptureKey(
                                                                                                        translationKey);
                                                                                } else {
                                                                                        config.removeCaptureKey(
                                                                                                        translationKey);
                                                                                }
                                                                        })
                                                                        .build());
                                }

                                captureCategory.addEntry(subCategory.build());
                        }

                        // Default
                        ConfigCategory defaultKeyBindsCategory = builder
                                        .getOrCreateCategory(Text.of("Default"));

                        for (Wynnbinds bind : Wynnbinds.values()) {
                                String translationKey = bind.getTranslationKey();
                                InputUtil.Key currentKey = InputUtil
                                                .fromTranslationKey(config.getDefaultKey(bind.getTranslationKey()));
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
                                                        config.setDefaultKey(translationKey, boundKey);
                                                })
                                                .build());
                        }

                        return builder.build();
                };
        }

}
