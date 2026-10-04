package ru.mentions.module.impl.display.interfaceimpl;

import ru.mentions.module.api.settings.impl.DragSetting;
import ru.mentions.module.impl.display.InterFace;
import ru.mentions.utils.annotation.IMinecraft;

public interface element extends IMinecraft {

    void onRender(DragSetting dragSetting, InterFace interFace);

}
