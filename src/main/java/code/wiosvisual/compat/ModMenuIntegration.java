package code.wiosvisual.compat;

import code.wiosvisual.display.screens.clickgui.WiosGuiScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Кнопка настроек Wios в списке модов ModMenu открывает ClickGUI клиента. */
public class ModMenuIntegration
implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            WiosGuiScreen gui = WiosGuiScreen.getInstance();
            gui.openGui(parent);
            return gui;
        };
    }
}
