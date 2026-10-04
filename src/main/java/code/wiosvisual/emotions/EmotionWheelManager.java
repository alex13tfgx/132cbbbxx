/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 *  net.minecraft.class_10055
 *  net.minecraft.class_1132
 *  net.minecraft.class_1297
 *  net.minecraft.class_1657
 *  net.minecraft.class_3532
 *  net.minecraft.class_3675$class_307
 *  net.minecraft.class_437
 *  net.minecraft.class_5498
 *  net.minecraft.class_630
 */
package code.wiosvisual.emotions;

import code.wiosvisual.utils.client.net.BackendConfig;

import code.wiosvisual.display.screens.EmotionWheelScreen;
import code.wiosvisual.emotions.EmotionSyncWebSocketClient;
import code.wiosvisual.emotions.EmotionType;
import code.wiosvisual.events.keyboard.KeyEvent;
import code.wiosvisual.events.player.TickEvent;
import code.wiosvisual.features.impl.misc.Globals;
import code.wiosvisual.main.listener.Listener;
import code.wiosvisual.utils.client.Instance;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import code.wiosvisual.utils.display.interfaces.QuickImports;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.math.MathHelper;

public class EmotionWheelManager
implements Listener,
QuickImports {
    private static final String PRESENCE_EMOTION = "__presence__";
    private static final long PRESENCE_INTERVAL_MS = 20000L;
    private static final long PRESENCE_TIMEOUT_MS = 35000L;
    private static EmotionWheelManager instance;
    private final Map<UUID, ActiveEmotion> activeEmotions = new ConcurrentHashMap<UUID, ActiveEmotion>();
    private final Map<UUID, PendingEmotion> pendingEmotions = new ConcurrentHashMap<UUID, PendingEmotion>();
    private final Map<UUID, Long> knownClientPlayers = new ConcurrentHashMap<UUID, Long>();
    private final CopyOnWriteArrayList<Consumer<JsonObject>> relayListeners = new CopyOnWriteArrayList();
    private EmotionWheelScreen wheelScreen;
    private EmotionSyncWebSocketClient syncClient;
    private long nextReconnectAttemptMs;
    private long nextPresenceBroadcastMs;
    private EmotionType previewEmotion = EmotionType.HELLO;
    private long previewStartedAtMs;

    public static EmotionWheelManager create() {
        instance = new EmotionWheelManager();
        return instance;
    }

    public static EmotionWheelManager getInstance() {
        return instance;
    }

    @EventHandler
    public void onKey(KeyEvent event) {
        Screen screen2;
        Globals globalsModule = Instance.get(Globals.class);
        if (globalsModule == null || !globalsModule.isState()) {
            return;
        }
        int openKey = globalsModule.getEmotionWheelBind().getKey();
        if (openKey == -1 || event.key() != openKey || event.type() != InputUtil.Type.KEYSYM) {
            return;
        }
        if (event.action() == 1) {
            if (EmotionWheelManager.mc.player != null && EmotionWheelManager.mc.world != null && EmotionWheelManager.mc.currentScreen == null) {
                this.openWheel();
            }
        } else if (event.action() == 0 && (screen2 = EmotionWheelManager.mc.currentScreen) instanceof EmotionWheelScreen) {
            EmotionWheelScreen screen = (EmotionWheelScreen)screen2;
            EmotionType emotion = screen.getHoveredEmotion();
            this.closeWheel(false);
            if (emotion != null) {
                this.playEmotion(emotion);
            }
        }
    }

    @EventHandler
    public void onTick(TickEvent event) {
        Globals globalsModule = Instance.get(Globals.class);
        if (globalsModule == null || !globalsModule.isState()) {
            this.closeWheel(false);
            this.stopEmotion();
            this.disconnectSync();
            this.knownClientPlayers.clear();
            this.pendingEmotions.clear();
            return;
        }
        this.ensureSyncConnection();
        if (EmotionWheelManager.mc.player == null || EmotionWheelManager.mc.world == null) {
            this.closeWheel(false);
            this.stopEmotion();
            this.knownClientPlayers.clear();
            this.pendingEmotions.clear();
            return;
        }
        this.sendPresenceIfNeeded();
        this.pruneKnownClients();
        this.applyPendingEmotions();
        Iterator<Map.Entry<UUID, ActiveEmotion>> iterator = this.activeEmotions.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, ActiveEmotion> entry = iterator.next();
            if (!entry.getValue().isExpired()) continue;
            if (entry.getValue().local && EmotionWheelManager.mc.player != null && EmotionWheelManager.mc.player.getUuid().equals(entry.getKey())) {
                EmotionWheelManager.mc.options.setPerspective(entry.getValue().previousPerspective);
            }
            iterator.remove();
        }
    }

    public void closeWheel(boolean forceGame) {
        if (EmotionWheelManager.mc.currentScreen instanceof EmotionWheelScreen) {
            this.wheelScreen = null;
            mc.setScreen(null);
        }
    }

    public void applyToModel(PlayerEntityRenderState state, ModelPart head, ModelPart hat, ModelPart body, ModelPart rightArm, ModelPart leftArm, ModelPart rightLeg, ModelPart leftLeg) {
        float alpha;
        if (EmotionWheelManager.mc.world == null) {
            return;
        }
        Entity entity2 = EmotionWheelManager.mc.world.getEntityById(state.id);
        if (!(entity2 instanceof PlayerEntity)) {
            return;
        }
        PlayerEntity player = (PlayerEntity)entity2;
        ActiveEmotion activeEmotion = this.activeEmotions.get(player.getUuid());
        EmotionType renderEmotion = activeEmotion != null ? activeEmotion.type : null;
        long startedAt = activeEmotion != null ? activeEmotion.startedAt : 0L;
        float f = alpha = activeEmotion != null ? activeEmotion.getBlend() : 0.0f;
        if (renderEmotion == null && EmotionWheelManager.mc.currentScreen instanceof EmotionWheelScreen && EmotionWheelManager.mc.player != null && player == EmotionWheelManager.mc.player && this.previewEmotion != null) {
            renderEmotion = this.previewEmotion;
            startedAt = this.previewStartedAtMs;
            alpha = 1.0f;
        }
        if (renderEmotion == null) {
            return;
        }
        this.resetModelPose(head, hat, body, rightArm, leftArm, rightLeg, leftLeg);
        long elapsed = Math.max(0L, System.currentTimeMillis() - startedAt);
        boolean animateHands = this.canAnimateHands(player);
        this.applyEmotionPose(renderEmotion, alpha, elapsed, animateHands, head, hat, body, rightArm, leftArm, rightLeg, leftLeg);
    }

    private void applyEmotionPose(EmotionType renderEmotion, float alpha, long elapsed, boolean animateHands, ModelPart head, ModelPart hat, ModelPart body, ModelPart rightArm, ModelPart leftArm, ModelPart rightLeg, ModelPart leftLeg) {
        if (renderEmotion.getAnimationFile() != null) {
            JsonEmotes.apply(renderEmotion.getAnimationFile(), elapsed, alpha, head, hat, body, rightArm, leftArm, rightLeg, leftLeg);
            return;
        }
        if (renderEmotion == EmotionType.DEB) {
            this.applyDeb(alpha, animateHands, head, hat, rightArm, leftArm);
        } else if (renderEmotion == EmotionType.FLOSS) {
            this.applyFloss(alpha, elapsed, animateHands, body, rightArm, leftArm, rightLeg, leftLeg);
        } else if (renderEmotion == EmotionType.GET_GRIDDY) {
            this.applyGetGriddy(alpha, elapsed, animateHands, head, hat, body, rightArm, leftArm, rightLeg, leftLeg);
        } else if (renderEmotion == EmotionType.HELLO) {
            this.applyHello(alpha, elapsed, animateHands, rightArm);
        } else if (renderEmotion == EmotionType.HAPPY) {
            this.applyHappy(alpha, animateHands, rightArm, leftArm);
        } else if (renderEmotion == EmotionType.CLAP) {
            this.applyClap(alpha, elapsed, animateHands, rightArm, leftArm);
        } else if (renderEmotion == EmotionType.CRY) {
            this.applyCry(alpha, elapsed, animateHands, head, hat, rightArm, leftArm);
        } else if (renderEmotion == EmotionType.POINT) {
            this.applyPoint(alpha, animateHands, head, hat, body, rightArm, leftArm);
        } else if (renderEmotion == EmotionType.SALUTE) {
            this.applySalute(alpha, animateHands, head, hat, rightArm, leftArm);
        } else if (renderEmotion == EmotionType.THINK) {
            this.applyThink(alpha, animateHands, head, hat, rightArm, leftArm);
        } else if (renderEmotion == EmotionType.STRETCH) {
            this.applyStretch(alpha, animateHands, body, rightArm, leftArm);
        } else if (renderEmotion == EmotionType.DANCE) {
            this.applyDance(alpha, elapsed, head, hat, body, rightArm, leftArm, rightLeg, leftLeg);
        } else if (renderEmotion == EmotionType.FACEPALM) {
            this.applyFacepalm(alpha, animateHands, head, hat, rightArm, leftArm);
        } else if (renderEmotion == EmotionType.SHRUG) {
            this.applyShrug(alpha, animateHands, head, hat, rightArm, leftArm);
        }
    }

    private void openWheel() {
        this.wheelScreen = new EmotionWheelScreen(this);
        mc.setScreen((Screen)this.wheelScreen);
    }

    public void openFromGlobals() {
        Globals globalsModule = Instance.get(Globals.class);
        if (globalsModule == null || !globalsModule.isState()) {
            return;
        }
        if (EmotionWheelManager.mc.player == null || EmotionWheelManager.mc.world == null || EmotionWheelManager.mc.currentScreen != null) {
            return;
        }
        this.openWheel();
    }

    public void setPreviewEmotion(EmotionType emotionType) {
        if (emotionType == null) {
            return;
        }
        if (this.previewEmotion != emotionType) {
            this.previewEmotion = emotionType;
            this.previewStartedAtMs = System.currentTimeMillis();
        }
    }

    public EmotionType getPreviewEmotion() {
        return this.previewEmotion;
    }

    private void playEmotion(EmotionType emotionType) {
        if (EmotionWheelManager.mc.player == null) {
            return;
        }
        long startedAt = System.currentTimeMillis();
        this.startEmotion((PlayerEntity)EmotionWheelManager.mc.player, emotionType, true, startedAt);
        this.sendEmotionPacket(emotionType, startedAt);
    }

    private void stopEmotion() {
        if (EmotionWheelManager.mc.player == null) {
            this.activeEmotions.clear();
            return;
        }
        ActiveEmotion activeEmotion = this.activeEmotions.remove(EmotionWheelManager.mc.player.getUuid());
        if (activeEmotion != null && activeEmotion.local) {
            EmotionWheelManager.mc.options.setPerspective(activeEmotion.previousPerspective);
        }
    }

    private void applyDeb(float alpha, boolean animateHands, ModelPart head, ModelPart hat, ModelPart rightArm, ModelPart leftArm) {
        if (animateHands) {
            rightArm.pitch = this.move(rightArm.pitch, -0.7f, alpha);
            rightArm.roll = this.move(rightArm.roll, -2.0f, alpha);
            leftArm.pitch = this.move(leftArm.pitch, -0.7f, alpha);
            leftArm.roll = this.move(leftArm.roll, -2.0f, alpha);
        }
        head.pitch = this.move(head.pitch, 0.7f, alpha);
        head.yaw = this.move(head.yaw, 0.5f, alpha);
        hat.pitch = this.move(hat.pitch, 0.7f, alpha);
        hat.yaw = this.move(hat.yaw, 0.5f, alpha);
    }

    private void applyHello(float alpha, long elapsed, boolean animateHands, ModelPart rightArm) {
        if (!animateHands) {
            return;
        }
        float wave = this.pingPong(elapsed, 250L);
        rightArm.pitch = this.move(rightArm.pitch, -0.2f, alpha);
        rightArm.roll = this.move(rightArm.roll, 2.75f - 0.25f * wave, alpha);
    }

    private void applyHappy(float alpha, boolean animateHands, ModelPart rightArm, ModelPart leftArm) {
        if (!animateHands) {
            return;
        }
        float pulse = this.pingPong(System.currentTimeMillis(), 260L);
        rightArm.pitch = this.move(rightArm.pitch, -0.35f - 0.2f * pulse, alpha);
        rightArm.roll = this.move(rightArm.roll, 2.1f + 0.3f * pulse, alpha);
        leftArm.pitch = this.move(leftArm.pitch, -0.35f - 0.2f * pulse, alpha);
        leftArm.roll = this.move(leftArm.roll, -2.1f - 0.3f * pulse, alpha);
    }

    private void applyClap(float alpha, long elapsed, boolean animateHands, ModelPart rightArm, ModelPart leftArm) {
        if (!animateHands) {
            return;
        }
        float t = this.pingPong(elapsed, 220L);
        float xBend = -1.1f * t;
        float yInward = 0.95f * t;
        float zFlat = 0.55f * t;
        rightArm.pitch = this.move(rightArm.pitch, xBend, alpha);
        rightArm.yaw = this.move(rightArm.yaw, -yInward, alpha);
        rightArm.roll = this.move(rightArm.roll, zFlat, alpha);
        leftArm.pitch = this.move(leftArm.pitch, xBend, alpha);
        leftArm.yaw = this.move(leftArm.yaw, yInward, alpha);
        leftArm.roll = this.move(leftArm.roll, -zFlat, alpha);
    }

    private void applyCry(float alpha, long elapsed, boolean animateHands, ModelPart head, ModelPart hat, ModelPart rightArm, ModelPart leftArm) {
        if (animateHands) {
            float hand = this.pingPong(elapsed, 320L);
            float xBend = -1.9f * hand - 0.25f;
            float yInward = 0.62f * hand;
            float zAcross = 1.05f * hand;
            rightArm.pitch = this.move(rightArm.pitch, xBend, alpha);
            rightArm.yaw = this.move(rightArm.yaw, -yInward, alpha);
            rightArm.roll = this.move(rightArm.roll, zAcross, alpha);
            leftArm.pitch = this.move(leftArm.pitch, xBend, alpha);
            leftArm.yaw = this.move(leftArm.yaw, yInward, alpha);
            leftArm.roll = this.move(leftArm.roll, -zAcross, alpha);
        }
        float headBob = this.pingPong(elapsed, 220L);
        head.pitch = this.move(head.pitch, 0.62f + 0.25f * headBob, alpha);
        head.roll = this.move(head.roll, -0.1f + 0.2f * headBob, alpha);
        hat.pitch = this.move(hat.pitch, head.pitch, alpha);
        hat.roll = this.move(hat.roll, head.roll, alpha);
    }

    private void applyFloss(float alpha, long elapsed, boolean animateHands, ModelPart body, ModelPart rightArm, ModelPart leftArm, ModelPart rightLeg, ModelPart leftLeg) {
        float action = this.pingPong(elapsed, 250L);
        float rightSide = this.pingPong(elapsed, 500L);
        float leftSide = this.pingPong(elapsed + 250L, 500L);
        if (animateHands) {
            rightArm.pitch = this.move(rightArm.pitch, 0.5f - rightSide, alpha);
            rightArm.roll = this.move(rightArm.roll, 1.0f - 2.0f * action, alpha);
            leftArm.pitch = this.move(leftArm.pitch, 0.5f - leftSide, alpha);
            leftArm.roll = this.move(leftArm.roll, 1.5f - 2.0f * action, alpha);
        }
        body.roll = this.move(body.roll, 0.3f * action - 0.15f, alpha);
        rightLeg.originX = this.move(rightLeg.originX, -0.5f - 3.0f * action, alpha);
        leftLeg.originX = this.move(leftLeg.originX, 3.0f - 2.5f * action, alpha);
    }

    private void applyGetGriddy(float alpha, long elapsed, boolean animateHands, ModelPart head, ModelPart hat, ModelPart body, ModelPart rightArm, ModelPart leftArm, ModelPart rightLeg, ModelPart leftLeg) {
        float action = this.pingPong(elapsed, 350L);
        float leg = this.pingPong(elapsed, 300L);
        float eye = this.griddyEye(elapsed);
        if (animateHands) {
            rightArm.pitch = this.move(rightArm.pitch, 1.0f - 1.5f * action - 3.5f * eye, alpha);
            rightArm.roll = this.move(rightArm.roll, 0.5f - 0.75f * action, alpha);
            leftArm.pitch = this.move(leftArm.pitch, 1.0f - 1.5f * action - 3.5f * eye, alpha);
            leftArm.roll = this.move(leftArm.roll, -0.5f + 0.75f * action, alpha);
        }
        body.pitch = this.move(body.pitch, 0.2f - 0.05f * action, alpha);
        rightLeg.originZ = this.move(rightLeg.originZ, 2.0f - 0.5f * action, alpha);
        rightLeg.pitch = this.move(rightLeg.pitch, 0.3f * leg - 0.3f, alpha);
        leftLeg.originZ = this.move(leftLeg.originZ, 2.0f - 0.5f * action, alpha);
        leftLeg.pitch = this.move(leftLeg.pitch, -0.3f * leg - 0.1f, alpha);
        head.yaw = this.move(head.yaw, -0.4f + 0.4f * eye, alpha);
        head.pitch = this.move(head.pitch, -0.3f * eye, alpha);
        head.roll = this.move(head.roll, 0.0f, alpha);
        hat.yaw = this.move(hat.yaw, -0.4f + 0.4f * eye, alpha);
        hat.pitch = this.move(hat.pitch, -0.3f * eye, alpha);
        hat.roll = this.move(hat.roll, 0.0f, alpha);
    }

    private void applyPoint(float alpha, boolean animateHands, ModelPart head, ModelPart hat, ModelPart body, ModelPart rightArm, ModelPart leftArm) {
        body.yaw = this.move(body.yaw, -0.18f, alpha);
        body.pitch = this.move(body.pitch, -0.04f, alpha);
        head.yaw = this.move(head.yaw, -0.3f, alpha);
        head.pitch = this.move(head.pitch, -0.1f, alpha);
        hat.yaw = this.move(hat.yaw, head.yaw, alpha);
        hat.pitch = this.move(hat.pitch, head.pitch, alpha);
        if (!animateHands) {
            return;
        }
        rightArm.pitch = this.move(rightArm.pitch, -1.52f, alpha);
        rightArm.yaw = this.move(rightArm.yaw, -0.22f, alpha);
        rightArm.roll = this.move(rightArm.roll, 0.09f, alpha);
        leftArm.pitch = this.move(leftArm.pitch, 0.1f, alpha);
        leftArm.roll = this.move(leftArm.roll, -0.2f, alpha);
    }

    private void applySalute(float alpha, boolean animateHands, ModelPart head, ModelPart hat, ModelPart rightArm, ModelPart leftArm) {
        head.yaw = this.move(head.yaw, -0.12f, alpha);
        head.pitch = this.move(head.pitch, -0.08f, alpha);
        hat.yaw = this.move(hat.yaw, head.yaw, alpha);
        hat.pitch = this.move(hat.pitch, head.pitch, alpha);
        if (!animateHands) {
            return;
        }
        rightArm.pitch = this.move(rightArm.pitch, -1.26f, alpha);
        rightArm.yaw = this.move(rightArm.yaw, -0.38f, alpha);
        rightArm.roll = this.move(rightArm.roll, 0.7f, alpha);
        leftArm.pitch = this.move(leftArm.pitch, 0.04f, alpha);
        leftArm.roll = this.move(leftArm.roll, -0.1f, alpha);
    }

    private void applyThink(float alpha, boolean animateHands, ModelPart head, ModelPart hat, ModelPart rightArm, ModelPart leftArm) {
        head.yaw = this.move(head.yaw, 0.36f, alpha);
        head.pitch = this.move(head.pitch, 0.22f, alpha);
        head.roll = this.move(head.roll, 0.05f, alpha);
        hat.yaw = this.move(hat.yaw, head.yaw, alpha);
        hat.pitch = this.move(hat.pitch, head.pitch, alpha);
        hat.roll = this.move(hat.roll, head.roll, alpha);
        if (!animateHands) {
            return;
        }
        rightArm.pitch = this.move(rightArm.pitch, -1.04f, alpha);
        rightArm.yaw = this.move(rightArm.yaw, -0.28f, alpha);
        rightArm.roll = this.move(rightArm.roll, 0.48f, alpha);
        leftArm.pitch = this.move(leftArm.pitch, 0.1f, alpha);
        leftArm.roll = this.move(leftArm.roll, -0.16f, alpha);
    }

    private void applyStretch(float alpha, boolean animateHands, ModelPart body, ModelPart rightArm, ModelPart leftArm) {
        float stretchWave = this.pingPong(System.currentTimeMillis(), 320L);
        body.pitch = this.move(body.pitch, -0.12f - 0.06f * stretchWave, alpha);
        body.originY = this.move(body.originY, -0.8f - 0.3f * stretchWave, alpha);
        if (!animateHands) {
            return;
        }
        rightArm.pitch = this.move(rightArm.pitch, -2.8f, alpha);
        leftArm.pitch = this.move(leftArm.pitch, -2.8f, alpha);
        rightArm.roll = this.move(rightArm.roll, 0.22f, alpha);
        leftArm.roll = this.move(leftArm.roll, -0.22f, alpha);
        rightArm.yaw = this.move(rightArm.yaw, 0.05f, alpha);
        leftArm.yaw = this.move(leftArm.yaw, -0.05f, alpha);
    }

    private void applyDance(float alpha, long elapsed, ModelPart head, ModelPart hat, ModelPart body, ModelPart rightArm, ModelPart leftArm, ModelPart rightLeg, ModelPart leftLeg) {
        float sway = this.pingPong(elapsed, 220L) * 2.0f - 1.0f;
        float bounce = this.pingPong(elapsed + 90L, 150L);
        body.roll = this.move(body.roll, 0.28f * sway, alpha);
        body.yaw = this.move(body.yaw, 0.25f * sway, alpha);
        body.pitch = this.move(body.pitch, -0.03f + 0.06f * bounce, alpha);
        head.roll = this.move(head.roll, -0.18f * sway, alpha);
        head.yaw = this.move(head.yaw, 0.22f * sway, alpha);
        hat.roll = this.move(hat.roll, head.roll, alpha);
        hat.yaw = this.move(hat.yaw, head.yaw, alpha);
        rightArm.pitch = this.move(rightArm.pitch, -0.8f + 0.5f * sway, alpha);
        leftArm.pitch = this.move(leftArm.pitch, -0.8f - 0.5f * sway, alpha);
        rightArm.roll = this.move(rightArm.roll, 0.55f + 0.3f * bounce, alpha);
        leftArm.roll = this.move(leftArm.roll, -0.55f - 0.3f * bounce, alpha);
        rightLeg.pitch = this.move(rightLeg.pitch, 0.18f + 0.38f * sway, alpha);
        leftLeg.pitch = this.move(leftLeg.pitch, 0.18f - 0.38f * sway, alpha);
    }

    private void applyFacepalm(float alpha, boolean animateHands, ModelPart head, ModelPart hat, ModelPart rightArm, ModelPart leftArm) {
        if (animateHands) {
            rightArm.pitch = this.move(rightArm.pitch, -1.62f, alpha);
            rightArm.yaw = this.move(rightArm.yaw, -0.3f, alpha);
            rightArm.roll = this.move(rightArm.roll, 0.48f, alpha);
            leftArm.pitch = this.move(leftArm.pitch, -0.06f, alpha);
            leftArm.roll = this.move(leftArm.roll, -0.05f, alpha);
        }
        head.pitch = this.move(head.pitch, 0.48f, alpha);
        head.roll = this.move(head.roll, -0.08f, alpha);
        hat.pitch = this.move(hat.pitch, head.pitch, alpha);
        hat.roll = this.move(hat.roll, head.roll, alpha);
    }

    private void applyShrug(float alpha, boolean animateHands, ModelPart head, ModelPart hat, ModelPart rightArm, ModelPart leftArm) {
        if (animateHands) {
            float shrugPulse = this.pingPong(System.currentTimeMillis(), 260L);
            rightArm.pitch = this.move(rightArm.pitch, -0.42f - 0.12f * shrugPulse, alpha);
            leftArm.pitch = this.move(leftArm.pitch, -0.42f - 0.12f * shrugPulse, alpha);
            rightArm.roll = this.move(rightArm.roll, 1.05f + 0.15f * shrugPulse, alpha);
            leftArm.roll = this.move(leftArm.roll, -1.05f - 0.15f * shrugPulse, alpha);
        }
        head.pitch = this.move(head.pitch, 0.1f, alpha);
        head.yaw = this.move(head.yaw, 0.08f, alpha);
        hat.pitch = this.move(hat.pitch, head.pitch, alpha);
        hat.yaw = this.move(hat.yaw, head.yaw, alpha);
    }

    private boolean canAnimateHands(PlayerEntity targetPlayer) {
        return EmotionWheelManager.mc.player == null || targetPlayer != EmotionWheelManager.mc.player || !EmotionWheelManager.mc.options.getPerspective().isFirstPerson();
    }

    private void resetModelPose(ModelPart head, ModelPart hat, ModelPart body, ModelPart rightArm, ModelPart leftArm, ModelPart rightLeg, ModelPart leftLeg) {
        head.originX = 0.0f;
        head.originY = 0.0f;
        head.originZ = 0.0f;
        hat.originX = 0.0f;
        hat.originY = 0.0f;
        hat.originZ = 0.0f;
        body.originX = 0.0f;
        body.originY = 0.0f;
        body.originZ = 0.0f;
        rightArm.originX = -5.0f;
        rightArm.originY = 2.0f;
        rightArm.originZ = 0.0f;
        leftArm.originX = 5.0f;
        leftArm.originY = 2.0f;
        leftArm.originZ = 0.0f;
        rightLeg.originX = -1.9f;
        rightLeg.originY = 12.0f;
        rightLeg.originZ = 0.0f;
        leftLeg.originX = 1.9f;
        leftLeg.originY = 12.0f;
        leftLeg.originZ = 0.0f;
    }

    private float move(float current, float target, float alpha) {
        return MathHelper.lerp((float)alpha, (float)current, (float)target);
    }

    private float pingPong(long elapsed, long halfCycleMs) {
        if (halfCycleMs <= 0L) {
            return 0.0f;
        }
        float cycle = (float)elapsed % ((float)halfCycleMs * 2.0f) / (float)halfCycleMs;
        return cycle <= 1.0f ? cycle : 2.0f - cycle;
    }

    private float griddyEye(long elapsed) {
        long cycle = elapsed % 3100L;
        if (cycle < 2100L) {
            return 0.0f;
        }
        return this.pingPong(cycle - 2100L, 500L);
    }

    private void ensureSyncConnection() {
        if (this.syncClient != null && (this.syncClient.isOpen() || this.syncClient.isConnectingState())) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now < this.nextReconnectAttemptMs) {
            return;
        }
        if (BackendConfig.webSocketUrl().isEmpty()) {
            return;
        }
        this.nextReconnectAttemptMs = now + 10000L;
        try {
            this.syncClient = new EmotionSyncWebSocketClient(URI.create(BackendConfig.webSocketUrl()), this::handleIncomingMessage);
            this.syncClient.connect();
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    private void disconnectSync() {
        if (this.syncClient != null) {
            try {
                this.syncClient.close();
            }
            catch (Exception exception) {
                // empty catch block
            }
            this.syncClient = null;
        }
    }

    public boolean isSyncConnected() {
        return this.syncClient != null && this.syncClient.isOpen();
    }

    public boolean isSyncConnecting() {
        return this.syncClient != null && this.syncClient.isConnectingState();
    }

    public String getSyncUrl() {
        return BackendConfig.webSocketUrl();
    }

    public String getDebugServerId() {
        return this.getCurrentServerId();
    }

    public boolean hasCompatibleClient(UUID uuid) {
        return this.knownClientPlayers.containsKey(uuid);
    }

    public void addRelayListener(Consumer<JsonObject> listener) {
        if (listener != null && !this.relayListeners.contains(listener)) {
            this.relayListeners.add(listener);
        }
    }

    public void removeRelayListener(Consumer<JsonObject> listener) {
        this.relayListeners.remove(listener);
    }

    public int getKnownClientCount() {
        return this.knownClientPlayers.size();
    }

    private void sendEmotionPacket(EmotionType emotionType, long startedAt) {
        this.sendRelayPacket(emotionType.name(), startedAt, null);
    }

    private void sendPresenceIfNeeded() {
        long now = System.currentTimeMillis();
        if (now < this.nextPresenceBroadcastMs) {
            return;
        }
        this.nextPresenceBroadcastMs = now + 20000L;
        this.sendSpecialPacket(PRESENCE_EMOTION, now);
    }

    private void sendSpecialPacket(String emotionValue, long startedAt) {
        this.sendRelayPacket(emotionValue, startedAt, null);
    }

    public boolean sendRelayPacket(String emotionValue, long startedAt, Consumer<JsonObject> packetCustomizer) {
        if (this.syncClient == null || !this.syncClient.isOpen() || EmotionWheelManager.mc.player == null) {
            return false;
        }
        JsonObject json = new JsonObject();
        json.addProperty("type", "play_emotion");
        json.addProperty("playerUuid", EmotionWheelManager.mc.player.getUuidAsString());
        json.addProperty("playerName", EmotionWheelManager.mc.player.getName().getString());
        json.addProperty("emotion", emotionValue);
        json.addProperty("startedAt", (Number)startedAt);
        json.addProperty("serverId", this.getCurrentServerId());
        if (packetCustomizer != null) {
            packetCustomizer.accept(json);
        }
        this.syncClient.send(gson.toJson((JsonElement)json));
        return true;
    }

    private void pruneKnownClients() {
        long cutoff = System.currentTimeMillis() - 35000L;
        this.knownClientPlayers.entrySet().removeIf(entry -> (Long)entry.getValue() < cutoff);
    }

    private void handleIncomingMessage(String message) {
        try {
            long startedAt;
            JsonObject json = JsonParser.parseString((String)message).getAsJsonObject();
            if (!json.has("type") || !"play_emotion".equals(json.get("type").getAsString())) {
                return;
            }
            if (!json.has("serverId") || !this.getCurrentServerId().equals(json.get("serverId").getAsString())) {
                return;
            }
            if (EmotionWheelManager.mc.world == null || !json.has("playerUuid") || !json.has("emotion")) {
                return;
            }
            UUID uuid = UUID.fromString(json.get("playerUuid").getAsString());
            if (EmotionWheelManager.mc.player != null && EmotionWheelManager.mc.player.getUuid().equals(uuid)) {
                return;
            }
            this.knownClientPlayers.put(uuid, System.currentTimeMillis());
            for (Consumer<JsonObject> relayListener : this.relayListeners) {
                relayListener.accept(json);
            }
            String emotionValue = json.get("emotion").getAsString();
            if (PRESENCE_EMOTION.equals(emotionValue)) {
                return;
            }
            EmotionType type = EmotionType.valueOf(emotionValue);
            long l = startedAt = json.has("startedAt") ? json.get("startedAt").getAsLong() : System.currentTimeMillis();
            if (this.applyEmotionToPlayer(uuid, type, startedAt)) {
                return;
            }
            this.pendingEmotions.put(uuid, new PendingEmotion(type, startedAt));
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    private void applyPendingEmotions() {
        if (this.pendingEmotions.isEmpty() || EmotionWheelManager.mc.world == null) {
            return;
        }
        Iterator<Map.Entry<UUID, PendingEmotion>> iterator = this.pendingEmotions.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PendingEmotion> entry = iterator.next();
            PendingEmotion pending = entry.getValue();
            if (pending.isExpired()) {
                iterator.remove();
                continue;
            }
            if (!this.applyEmotionToPlayer(entry.getKey(), pending.type(), pending.startedAt())) continue;
            iterator.remove();
        }
    }

    private boolean applyEmotionToPlayer(UUID uuid, EmotionType type, long startedAt) {
        for (PlayerEntity player : EmotionWheelManager.mc.world.getPlayers()) {
            if (!player.getUuid().equals(uuid)) continue;
            this.startEmotion(player, type, false, startedAt);
            return true;
        }
        return false;
    }

    private void startEmotion(PlayerEntity player, EmotionType emotionType, boolean local, long startedAt) {
        Perspective previous = Perspective.FIRST_PERSON;
        if (local) {
            ActiveEmotion previousLocal = this.activeEmotions.get(player.getUuid());
            previous = previousLocal != null && previousLocal.local ? previousLocal.previousPerspective : EmotionWheelManager.mc.options.getPerspective();
            EmotionWheelManager.mc.options.setPerspective(Perspective.THIRD_PERSON_FRONT);
        }
        this.activeEmotions.put(player.getUuid(), new ActiveEmotion(emotionType, startedAt, previous, local));
    }

    private String getCurrentServerId() {
        if (mc.isInSingleplayer()) {
            IntegratedServer server = mc.getServer();
            return server != null ? "singleplayer:" + server.getSaveProperties().getLevelName() : "singleplayer";
        }
        return mc.getCurrentServerEntry() != null ? EmotionWheelManager.mc.getCurrentServerEntry().address : "unknown";
    }

    private static final class ActiveEmotion {
        private final EmotionType type;
        private final long startedAt;
        private final Perspective previousPerspective;
        private final boolean local;

        private ActiveEmotion(EmotionType type, long startedAt, Perspective previousPerspective, boolean local) {
            this.type = type;
            this.startedAt = startedAt;
            this.previousPerspective = previousPerspective;
            this.local = local;
        }

        private long getElapsedMs() {
            return System.currentTimeMillis() - this.startedAt;
        }

        private boolean isExpired() {
            return this.getElapsedMs() >= this.type.getDurationMs();
        }

        private float getBlend() {
            long elapsed = this.getElapsedMs();
            long remaining = Math.max(0L, this.type.getDurationMs() - elapsed);
            float fadeIn = MathHelper.clamp((float)((float)elapsed / 300.0f), (float)0.0f, (float)1.0f);
            float fadeOut = MathHelper.clamp((float)((float)remaining / 300.0f), (float)0.0f, (float)1.0f);
            float blend = Math.min(fadeIn, fadeOut);
            return blend * blend * (3.0f - 2.0f * blend);
        }
    }

    private static final class PendingEmotion {
        private final EmotionType type;
        private final long startedAt;

        private PendingEmotion(EmotionType type, long startedAt) {
            this.type = type;
            this.startedAt = startedAt;
        }

        private EmotionType type() {
            return this.type;
        }

        private long startedAt() {
            return this.startedAt;
        }

        private boolean isExpired() {
            return System.currentTimeMillis() - this.startedAt >= this.type.getDurationMs();
        }
    }
}
