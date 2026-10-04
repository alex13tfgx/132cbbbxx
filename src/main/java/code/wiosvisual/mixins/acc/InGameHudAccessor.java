/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2960
 *  net.minecraft.class_329
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.gen.Accessor
 */
package code.wiosvisual.mixins.acc;

import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value={InGameHud.class})
public interface InGameHudAccessor {
    @Accessor(value="FOOD_FULL_TEXTURE")
    public Identifier getFoodFullTexture();

    @Accessor(value="FOOD_HALF_TEXTURE")
    public Identifier getFoodHalfTexture();

    @Accessor(value="FOOD_EMPTY_TEXTURE")
    public Identifier getFoodEmptyTexture();

    @Accessor(value="FOOD_FULL_HUNGER_TEXTURE")
    public Identifier getFoodFullHungerTexture();

    @Accessor(value="FOOD_HALF_HUNGER_TEXTURE")
    public Identifier getFoodHalfHungerTexture();

    @Accessor(value="FOOD_EMPTY_HUNGER_TEXTURE")
    public Identifier getFoodEmptyHungerTexture();
}
