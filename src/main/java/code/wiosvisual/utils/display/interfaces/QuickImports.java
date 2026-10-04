/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  net.minecraft.class_310
 *  net.minecraft.class_9779
 */
package code.wiosvisual.utils.display.interfaces;

import code.wiosvisual.utils.display.interfaces.LazyWindow;
import code.wiosvisual.utils.display.interfaces.QuickLogger;
import code.wiosvisual.utils.display.render.draw.DrawEngine;
import code.wiosvisual.utils.display.render.draw.DrawEngineImpl;
import code.wiosvisual.utils.display.render.shape.implement.Arc;
import code.wiosvisual.utils.display.render.shape.implement.Blur;
import code.wiosvisual.utils.display.render.shape.implement.Image;
import code.wiosvisual.utils.display.render.shape.implement.Rectangle;
import com.google.gson.Gson;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderTickCounter;

public interface QuickImports
extends QuickLogger {
    public static final MinecraftClient mc = MinecraftClient.getInstance();
    public static final RenderTickCounter tickCounter = mc.getRenderTickCounter();
    public static final LazyWindow window = new LazyWindow();
    public static final DrawEngine drawEngine = new DrawEngineImpl();
    public static final Rectangle rectangle = new Rectangle();
    public static final Blur blur = new Blur();
    public static final Arc arc = new Arc();
    public static final Image image = new Image();
    public static final Gson gson = new Gson();
}
