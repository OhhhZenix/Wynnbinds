package dev.zenix.wynnbinds.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.text.Text;

public class WynnbindsModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            ConfigBuilder builder = ConfigBuilder.create().setParentScreen(parent).setTitle(Text.of("Wynnbinds"));
            builder.setSavingRunnable(WynnbindsClient.getInstance()::saveConfig);

            ConfigEntryBuilder entryBuilder = builder.entryBuilder();
            ConfigCategory configCategory = builder.getOrCreateCategory(Text.of("Config"));

            // General
            configCategory.addEntry(entryBuilder.startTextDescription(Text.of("General")).build());
            configCategory.addEntry(entryBuilder
                    .startBooleanToggle(Text.of("Enable Mod"), WynnbindsClient.getInstance().getConfig().isModEnabled())
                    .setDefaultValue(true)
                    .setSaveConsumer(value -> WynnbindsClient.getInstance().getConfig().setEnableMod(value)).build());
            configCategory.addEntry(entryBuilder
                    .startBooleanToggle(Text.of("Enable Notifications"),
                            WynnbindsClient.getInstance().getConfig().isNotificationsEnabled())
                    .setDefaultValue(true)
                    .setSaveConsumer(value -> WynnbindsClient.getInstance().getConfig().setEnableNotifications(value))
                    .build());

            return builder.build();
        };
    }

}
