/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.logging.log4j.LogManager
 */
package code.wiosvisual.features.module;

import code.wiosvisual.features.impl.customization.ThemeEditor;
import code.wiosvisual.features.impl.misc.Assist;
import code.wiosvisual.features.impl.misc.AutoAuth;
import code.wiosvisual.features.impl.misc.AutoInvest;
import code.wiosvisual.features.impl.misc.AutoClanUpgrade;
import code.wiosvisual.features.impl.misc.Sounds;
import code.wiosvisual.features.impl.misc.AutoCommand;
import code.wiosvisual.features.impl.misc.AutoTpAccept;
import code.wiosvisual.features.impl.misc.ClickFriend;
import code.wiosvisual.features.impl.misc.DeathCoords;
import code.wiosvisual.features.impl.misc.Globals;
import code.wiosvisual.features.impl.misc.ItemScroller;
import code.wiosvisual.features.impl.misc.ServerJoiner;
import code.wiosvisual.features.impl.misc.ShulkerBypass;
import code.wiosvisual.features.impl.misc.SoundsRemove;
import code.wiosvisual.features.impl.misc.ShiftTab;
import code.wiosvisual.features.impl.misc.TapeMouse;
import code.wiosvisual.features.impl.misc.TotemPop;
import code.wiosvisual.features.impl.player.Structures;
import code.wiosvisual.features.impl.player.AutoRespawn;
import code.wiosvisual.features.impl.player.Sprint;
import code.wiosvisual.features.impl.player.BurrowHelper;
import code.wiosvisual.features.impl.player.FreeLook;
import code.wiosvisual.features.impl.render.Ambience;
import code.wiosvisual.features.impl.render.ArrowNames;
import code.wiosvisual.features.impl.render.AspectRatio;
import code.wiosvisual.features.impl.render.AuctionHelper;
import code.wiosvisual.features.impl.render.BlockOverlay;
import code.wiosvisual.features.impl.render.DashTrails;
import code.wiosvisual.features.impl.render.Zoom;
import code.wiosvisual.features.impl.render.ClickGui;
import code.wiosvisual.features.impl.render.CrossHair;
import code.wiosvisual.features.impl.render.CustomModels;
import code.wiosvisual.features.impl.render.Fullbright;
import code.wiosvisual.features.impl.render.BetterWorld;
import code.wiosvisual.features.impl.render.BodyGlow;
import code.wiosvisual.features.impl.render.BodyOutline;
import code.wiosvisual.features.impl.render.HandShader;
import code.wiosvisual.features.impl.render.HitColor;
import code.wiosvisual.features.impl.render.HitMarker;
import code.wiosvisual.features.impl.render.HitRange;
import code.wiosvisual.features.impl.misc.ElytraSwap;
import code.wiosvisual.features.impl.misc.FastSwap;
import code.wiosvisual.features.impl.misc.ItemSwap;
import code.wiosvisual.features.impl.misc.NoFriendDamage;
import code.wiosvisual.features.impl.render.ChinaHat;
import code.wiosvisual.features.impl.render.Cosmetics;
import code.wiosvisual.features.impl.render.HitSounds;
import code.wiosvisual.features.impl.render.MotionBlur;
import code.wiosvisual.features.impl.render.HitboxCustomizer;
import code.wiosvisual.features.impl.render.Hud;
import code.wiosvisual.features.impl.render.ItemPhysic;
import code.wiosvisual.features.impl.render.ItemRadius;
import code.wiosvisual.features.impl.render.KillEffect;
import code.wiosvisual.features.impl.render.NoHurtCam;
import code.wiosvisual.features.impl.render.NoRender;
import code.wiosvisual.features.impl.render.TargetESP;
import code.wiosvisual.features.impl.render.TntTimer;
import code.wiosvisual.features.impl.render.EventWaypoints;
import code.wiosvisual.features.impl.render.TotemParticles;
import code.wiosvisual.features.impl.render.Trajectories;
import code.wiosvisual.features.impl.render.ShaderSky;
import code.wiosvisual.features.impl.render.ViewModel;
import code.wiosvisual.features.impl.render.wings.Wings;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.utils.client.managers.event.EventManager;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;

public class ModuleRepository {
    private final List<Module> modules = new ArrayList<Module>();

    public void setup() {
        this.register(new ItemPhysic(), new Ambience(), new code.wiosvisual.features.impl.render.InvSpoofer(), new ArrowNames(), new AspectRatio(), new FreeLook(), new ClickFriend(), new Globals(), new ItemScroller(), new Hud(), new HitboxCustomizer(), new ClickGui(), new ThemeEditor(), new AuctionHelper(), new ServerJoiner(), new AutoRespawn(), new BurrowHelper(), new TargetESP(), new TntTimer(), new ViewModel(), new AutoTpAccept(), new NoRender(), new Fullbright(), new BodyGlow(), new BodyOutline(), new BetterWorld(), new ItemRadius(), new AutoAuth(), new Assist(), new Sounds(), new Structures(), new CrossHair(), new HitMarker(), new code.wiosvisual.features.impl.render.HitEffect(), new HitColor(), new HitRange(), new HitSounds(), new ChinaHat(), new Cosmetics(), new MotionBlur(), new ElytraSwap(), new ItemSwap(), new FastSwap(), new NoFriendDamage(), new DeathCoords(), new KillEffect(), new ShulkerBypass(), new SoundsRemove(), new Trajectories(), new DashTrails(), new TapeMouse(), new Wings(), new ShaderSky(), new BlockOverlay(), new HandShader(), new NoHurtCam(), new ShiftTab(), new Zoom(), new Sprint(), new AutoCommand(), new CustomModels(), new TotemPop(), new TotemParticles(), new EventWaypoints(), new AutoInvest(), new AutoClanUpgrade(), new ru.mentions.module.impl.render.JumpCircle(), new ru.mentions.module.impl.render.Particles(), new ru.mentions.module.impl.render.TotemGhost(), new ru.mentions.module.impl.render.FireFlies(), new ru.mentions.module.impl.display.InterFace());
        this.sanitizeBrokenModules();
    }

    private void sanitizeBrokenModules() {
        this.modules.removeIf(module -> {
            if (EventManager.canInspectEventHandlers(module)) {
                return false;
            }
            LogManager.getLogger((String)"Wiosvisual").warn("Removing broken module {} from registry", (Object)module.getClass().getName());
            if (module.isState()) {
                module.setState(false);
            }
            return true;
        });
    }

    public void register(Module ... module) {
        this.modules.addAll(List.of(module));
    }

    public List<Module> modules() {
        return this.modules;
    }
}
