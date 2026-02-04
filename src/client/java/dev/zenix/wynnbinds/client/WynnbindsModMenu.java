package dev.zenix.wynnbinds.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

public class WynnbindsModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            ConfigBuilder builder = ConfigBuilder.create().setParentScreen(parent).setTitle(Text.of("Wynnbinds"));
            builder.setSavingRunnable(WynnbindsClient.getInstance()::saveConfig);

            ConfigEntryBuilder entryBuilder = builder.entryBuilder();
            ConfigCategory configCategory = builder.getOrCreateCategory(Text.of("Config"));

            var config = WynnbindsClient.getInstance().getConfig();

            // General
            configCategory.addEntry(entryBuilder.startTextDescription(Text.of("General")).build());
            configCategory.addEntry(entryBuilder
                    .startBooleanToggle(Text.of("Enable Mod"), config.isModEnabled())
                    .setDefaultValue(true)
                    .setSaveConsumer(value -> config.setEnableMod(value)).build());
            configCategory.addEntry(entryBuilder
                    .startBooleanToggle(Text.of("Enable Notifications"), config.isNotificationsEnabled())
                    .setDefaultValue(true)
                    .setSaveConsumer(value -> config.setEnableNotifications(value))
                    .build());

            // Keybinds
            configCategory.addEntry(entryBuilder.startTextDescription(Text.of("Default Keybinds")).build());
            for (Wynnbinds bind : Wynnbinds.values()) {
                String translationKey = bind.getTranslationKey();
                InputUtil.Key currentKey = InputUtil
                        .fromTranslationKey(config.getDefaultBoundKey(bind.getTranslationKey()));
                InputUtil.Key defaultKey = InputUtil.fromTranslationKey(bind.getDefaultBoundKey());
                configCategory.addEntry(entryBuilder
                        .startKeyCodeField(Text.of(bind.getDisplayName()), currentKey)
                        .setDefaultValue(defaultKey)
                        .setKeySaveConsumer(value -> {
                            String boundKey = value.getTranslationKey();
                            WynnbindsClient.LOGGER.debug(
                                    "Setting keybind for {} to {}",
                                    translationKey, boundKey);
                            config.setDefaultBoundKey(translationKey, boundKey);
                        })
                        .build());
            }

            return builder.build();
        };
    }

}
