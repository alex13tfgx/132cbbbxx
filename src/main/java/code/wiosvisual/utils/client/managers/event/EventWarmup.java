/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.logging.log4j.LogManager
 *  org.apache.logging.log4j.Logger
 */
package code.wiosvisual.utils.client.managers.event;

import code.wiosvisual.events.block.BlockBreakingEvent;
import code.wiosvisual.events.block.BlockCollisionEvent;
import code.wiosvisual.events.block.BlockEntityProgressEvent;
import code.wiosvisual.events.block.BlockUpdateEvent;
import code.wiosvisual.events.block.BreakBlockEvent;
import code.wiosvisual.events.block.PushEvent;
import code.wiosvisual.events.chat.ChatEvent;
import code.wiosvisual.events.chat.TabCompleteEvent;
import code.wiosvisual.events.container.CloseScreenEvent;
import code.wiosvisual.events.container.HandledScreenEvent;
import code.wiosvisual.events.container.SetScreenEvent;
import code.wiosvisual.events.item.ClickSlotEvent;
import code.wiosvisual.events.item.HandAnimationEvent;
import code.wiosvisual.events.item.HandOffsetEvent;
import code.wiosvisual.events.item.SwingDurationEvent;
import code.wiosvisual.events.item.UsingItemEvent;
import code.wiosvisual.events.keyboard.HotBarScrollEvent;
import code.wiosvisual.events.keyboard.KeyEvent;
import code.wiosvisual.events.keyboard.MouseRotationEvent;
import code.wiosvisual.events.packet.PacketEvent;
import code.wiosvisual.events.player.AttackEvent;
import code.wiosvisual.events.player.BoundingBoxControlEvent;
import code.wiosvisual.events.player.DeathScreenEvent;
import code.wiosvisual.events.player.EntityDeathEvent;
import code.wiosvisual.events.player.EntitySpawnEvent;
import code.wiosvisual.events.player.EventNoSlow;
import code.wiosvisual.events.player.FireworkEvent;
import code.wiosvisual.events.player.GameUpdateEvent;
import code.wiosvisual.events.player.HeldItemRendererEvent;
import code.wiosvisual.events.player.HotBarUpdateEvent;
import code.wiosvisual.events.player.InputEvent;
import code.wiosvisual.events.player.JumpEvent;
import code.wiosvisual.events.player.MotionEvent;
import code.wiosvisual.events.player.MoveEvent;
import code.wiosvisual.events.player.PlayerCollisionEvent;
import code.wiosvisual.events.player.PlayerTravelEvent;
import code.wiosvisual.events.player.PlayerVelocityStrafeEvent;
import code.wiosvisual.events.player.PostMotionEvent;
import code.wiosvisual.events.player.PostTickEvent;
import code.wiosvisual.events.player.RotatedMovementInputEvent;
import code.wiosvisual.events.player.RotationUpdateEvent;
import code.wiosvisual.events.player.SprintEvent;
import code.wiosvisual.events.player.SwimmingEvent;
import code.wiosvisual.events.player.TickEvent;
import code.wiosvisual.events.render.AspectRatioEvent;
import code.wiosvisual.events.render.CameraEvent;
import code.wiosvisual.events.render.CameraPositionEvent;
import code.wiosvisual.events.render.DrawEvent;
import code.wiosvisual.events.render.EntityColorEvent;
import code.wiosvisual.events.render.FogEvent;
import code.wiosvisual.events.render.FovEvent;
import code.wiosvisual.events.render.ItemRendererEvent;
import code.wiosvisual.events.render.TextFactoryEvent;
import code.wiosvisual.events.render.WorldLoadEvent;
import code.wiosvisual.events.render.WorldRenderEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class EventWarmup {
    private static final Logger LOGGER = LogManager.getLogger((String)"Wiosvisual");
    private static final Class<?>[] EVENT_TYPES = new Class[]{AttackEvent.class, AspectRatioEvent.class, BlockBreakingEvent.class, BlockCollisionEvent.class, BlockEntityProgressEvent.class, BlockUpdateEvent.class, BreakBlockEvent.class, BoundingBoxControlEvent.class, CameraEvent.class, CameraPositionEvent.class, ChatEvent.class, ClickSlotEvent.class, CloseScreenEvent.class, DeathScreenEvent.class, DrawEvent.class, EntityColorEvent.class, EntityDeathEvent.class, EntitySpawnEvent.class, EventNoSlow.class, FireworkEvent.class, FogEvent.class, FovEvent.class, GameUpdateEvent.class, HandAnimationEvent.class, HandOffsetEvent.class, HandledScreenEvent.class, HeldItemRendererEvent.class, HotBarScrollEvent.class, HotBarUpdateEvent.class, InputEvent.class, ItemRendererEvent.class, JumpEvent.class, KeyEvent.class, MotionEvent.class, MouseRotationEvent.class, MoveEvent.class, PacketEvent.class, PlayerCollisionEvent.class, PlayerTravelEvent.class, PlayerVelocityStrafeEvent.class, PostMotionEvent.class, PostTickEvent.class, PushEvent.class, RotatedMovementInputEvent.class, RotationUpdateEvent.class, SetScreenEvent.class, SprintEvent.class, SwimmingEvent.class, SwingDurationEvent.class, TabCompleteEvent.class, TextFactoryEvent.class, TickEvent.class, UsingItemEvent.class, WorldLoadEvent.class, WorldRenderEvent.class};

    private EventWarmup() {
    }

    public static void warmup() {
        for (Class<?> type : EVENT_TYPES) {
            EventWarmup.touch(type);
        }
        LOGGER.debug("Preloaded {} event types", (Object)EVENT_TYPES.length);
    }

    private static void touch(Class<?> type) {
        try {
            Class.forName(type.getName(), true, type.getClassLoader());
        }
        catch (ClassNotFoundException error) {
            LOGGER.error("Missing event class: {}", (Object)type.getName(), (Object)error);
        }
        catch (LinkageError error) {
            LOGGER.error("Failed to initialize event class: {}", (Object)type.getName(), (Object)error);
        }
    }
}
