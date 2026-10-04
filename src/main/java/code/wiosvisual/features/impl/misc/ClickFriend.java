/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1297
 *  net.minecraft.class_1657
 *  net.minecraft.class_239
 *  net.minecraft.class_3966
 */
package code.wiosvisual.features.impl.misc;

import code.wiosvisual.common.repository.friend.FriendUtils;
import code.wiosvisual.events.keyboard.KeyEvent;
import code.wiosvisual.features.module.Module;
import code.wiosvisual.features.module.ModuleCategory;
import code.wiosvisual.features.module.setting.implement.BindSetting;
import code.wiosvisual.utils.client.managers.event.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

public class ClickFriend
extends Module {
    private final BindSetting friendBind = new BindSetting("\u0414\u043e\u0431\u0430\u0432\u0438\u0442\u044c \u0434\u0440\u0443\u0433\u0430", "\u0414\u043e\u0431\u0430\u0432\u0438\u0442\u044c/\u0443\u0434\u0430\u043b\u0438\u0442\u044c \u0434\u0440\u0443\u0433\u0430");

    public ClickFriend() {
        super("ClickFriend", "Click Friend", ModuleCategory.MISC);
        this.setup(this.friendBind);
    }

    @EventHandler
    public void onKey(KeyEvent e) {
        EntityHitResult result;
        HitResult hitResult;
        if (e.isKeyDown(this.friendBind.getKey()) && (hitResult = ClickFriend.mc.crosshairTarget) instanceof EntityHitResult && (result = (EntityHitResult)hitResult).getEntity() instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity)result.getEntity();
            if (FriendUtils.isFriend((Entity)player)) {
                FriendUtils.removeFriend(player);
            } else {
                FriendUtils.addFriend(player);
            }
        }
    }
}
