/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.class_2338
 *  net.minecraft.class_243
 *  net.minecraft.class_4587
 */
package code.wiosvisual.common.repository.way;

import code.wiosvisual.utils.display.render.GuiCompat;

import code.wiosvisual.common.repository.way.Way;
import code.wiosvisual.events.render.DrawEvent;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import code.wiosvisual.utils.client.managers.event.EventManager;
import code.wiosvisual.utils.display.color.ColorAssist;
import code.wiosvisual.utils.display.interfaces.QuickImports;
import code.wiosvisual.utils.display.interfaces.QuickLogger;
import code.wiosvisual.utils.display.render.font.FontRenderer;
import code.wiosvisual.utils.display.render.font.Fonts;
import code.wiosvisual.utils.display.render.shape.ShapeProperties;
import code.wiosvisual.utils.math.projection.Projection;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class WayRepository
implements QuickImports,
QuickLogger {
    public List<Way> wayList = new ArrayList<Way>();

    public WayRepository(EventManager eventManager) {
        EventManager.register(this);
    }

    public boolean isEmpty() {
        return this.wayList.isEmpty();
    }

    public void addWay(String name, BlockPos pos, String server) {
        this.wayList.add(new Way(name, pos, server));
    }

    public void upsertWay(String name, BlockPos pos, String server) {
        if (name == null) {
            return;
        }
        for (int i = 0; i < this.wayList.size(); ++i) {
            Way w = this.wayList.get(i);
            if (w == null || !w.name().equalsIgnoreCase(name)) continue;
            this.wayList.set(i, new Way(name, pos, server));
            return;
        }
        this.addWay(name, pos, server);
    }

    public boolean hasWay(String text) {
        return this.wayList.stream().anyMatch(s -> s.name().equalsIgnoreCase(text));
    }

    public void deleteWay(String name) {
        this.wayList.removeIf(macro -> macro.name().equalsIgnoreCase(name));
    }

    public void clearList() {
        if (!this.isEmpty()) {
            this.wayList.clear();
        }
    }

    /** Ключ сервера, к которому привязываются точки: адрес сервера или "vanilla" в одиночной игре. */
    public static String currentServer() {
        return mc.getNetworkHandler() == null || mc.getNetworkHandler().getServerInfo() == null ? "vanilla" : mc.getNetworkHandler().getServerInfo().address;
    }

    @EventHandler
    public void onDraw(DrawEvent e) {
        if (this.isEmpty() || WayRepository.mc.player == null) {
            return;
        }
        String currentServer = WayRepository.currentServer();
        MatrixStack matrix = GuiCompat.stack(e.getDrawContext());
        double[] screen = new double[3];
        this.wayList.forEach(way -> {
            if (!way.server().equalsIgnoreCase(currentServer)) {
                return;
            }
            Vec3d wayVec = way.pos().toCenterPos();
            if (!Projection.isInFrontOfCamera(wayVec)) {
                return;
            }
            if (!Projection.worldSpaceToScreenSpaceInto(wayVec.x, wayVec.y, wayVec.z, screen)) {
                return;
            }
            double distance = WayRepository.mc.getEntityRenderDispatcher().camera.pos.distanceTo(wayVec);
            String text = way.name() + " - " + Math.round(distance) + "m";
            FontRenderer font = Fonts.getSize(10, Fonts.Type.SuisseIntlMedium);
            float textWidth = font.getStringWidth(text);
            float textHeight = font.getStringHeight(text);
            float paddingX = 5.0f;
            float paddingY = 3.0f;
            float boxWidth = textWidth + paddingX * 2.0f;
            float boxHeight = textHeight + paddingY * 2.0f;
            float x = (float)screen[0] - boxWidth / 2.0f;
            float y = (float)screen[1] - boxHeight / 2.0f;
            rectangle.render(ShapeProperties.create(matrix, x, y, boxWidth, boxHeight).round(4.0f).color(new Color(0, 0, 0, 255).getRGB()).build());
            rectangle.render(ShapeProperties.create(matrix, x, y, boxWidth, boxHeight).round(4.0f).thickness(3.0f).outlineColor(new Color(220, 225, 235, 235).getRGB()).color(0).build());
            font.drawCenteredBoth(matrix, text, x + boxWidth / 2.0f, y, boxHeight, ColorAssist.getText());
        });
    }
}
