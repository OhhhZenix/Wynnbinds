package dev.zenix.wynnbinds.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screen.Screen;

public class WynnbindsModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return WynnbindsModMenu::createScreen;
    }

    private static Screen createScreen(Screen screen) {

        return screen;
    }
}
