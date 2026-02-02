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
            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent).setTitle(Text.of("Wynnbinds"));

            ConfigEntryBuilder entryBuilder = builder.entryBuilder();

            ConfigCategory configCategory = builder.getOrCreateCategory(Text.of("Config"));

            configCategory.addEntry(entryBuilder
                    .startBooleanToggle(Text.of("Enable Mod"),
                            WynnbindsClient.getInstance().getConfig().isModEnabled())
                    .setDefaultValue(false)
                    .setSaveConsumer(value -> WynnbindsClient.getInstance().getConfig().setEnableMod(value))
                    .build());

            configCategory.addEntry(entryBuilder
                    .startBooleanToggle(Text.of("Enable Notifications"),
                            WynnbindsClient.getInstance().getConfig().isNotificationsEnabled())
                    .setDefaultValue(false)
                    .setSaveConsumer(value -> WynnbindsClient.getInstance().getConfig().setEnableNotifications(value))
                    .build());

            builder.setSavingRunnable(WynnbindsClient.getInstance()::saveConfig);

            return builder.build();
        };
    }

}
