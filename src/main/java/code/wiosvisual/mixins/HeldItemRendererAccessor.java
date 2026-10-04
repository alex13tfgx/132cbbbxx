/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1306
 *  net.minecraft.class_1309
 *  net.minecraft.class_1799
 *  net.minecraft.class_4587
 *  net.minecraft.class_4597
 *  net.minecraft.class_759
 *  net.minecraft.class_811
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.gen.Invoker
 */
package code.wiosvisual.mixins;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value={HeldItemRenderer.class})
public interface HeldItemRendererAccessor {
    @Invoker(value="renderItem")
    public void wiosvisual$renderItem(LivingEntity var1, ItemStack var2, ItemDisplayContext var3, boolean var4, MatrixStack var5, VertexConsumerProvider var6, int var7);

    @Invoker(value="renderArmHoldingItem")
    public void invokeRenderArmHoldingItem(MatrixStack var1, VertexConsumerProvider var2, int var3, float var4, float var5, Arm var6);

    @Invoker(value="applyEquipOffset")
    public void invokeApplyEquipOffset(MatrixStack var1, Arm var2, float var3);

    @Invoker(value="swingArm")
    public void invokeSwingArm(float var1, float var2, MatrixStack var3, int var4, Arm var5);

    @Invoker(value="renderMapInBothHands")
    public void invokeRenderMapInBothHands(MatrixStack var1, VertexConsumerProvider var2, int var3, float var4, float var5, float var6);

    @Invoker(value="renderMapInOneHand")
    public void invokeRenderMapInOneHand(MatrixStack var1, VertexConsumerProvider var2, int var3, float var4, Arm var5, float var6, ItemStack var7);

    @Invoker(value="renderItem")
    public void invokeRenderItem(LivingEntity var1, ItemStack var2, ItemDisplayContext var3, boolean var4, MatrixStack var5, VertexConsumerProvider var6, int var7);
}
