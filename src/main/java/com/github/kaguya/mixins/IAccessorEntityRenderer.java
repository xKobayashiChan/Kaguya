package com.github.kaguya.mixins;

import net.minecraft.client.renderer.EntityRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@SideOnly(Side.CLIENT)
@Mixin({EntityRenderer.class})
public interface IAccessorEntityRenderer {
    @Invoker
    void callSetupCameraTransform(float float1, int integer);

    @Accessor
    float getThirdPersonDistance();

    @Accessor
    void setThirdPersonDistance(float distance);
}
