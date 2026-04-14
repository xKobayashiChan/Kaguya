package com.github.kaguya.module.modules;

import com.github.kaguya.event.EventTarget;
import com.github.kaguya.event.types.EventType;
import com.github.kaguya.events.Render3DEvent;
import com.github.kaguya.events.UpdateEvent;
import com.github.kaguya.module.Module;
import com.github.kaguya.property.properties.FloatProperty;
import net.minecraft.client.Minecraft;

public class Zenith extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final FloatProperty speed = new FloatProperty("speed", 5.0F, 0.5F, 30.0F);
    public final FloatProperty pitch = new FloatProperty("pitch", 0.0F, -90.0F, 90.0F);

    private float spinYaw = 0.0F;
    private float prevSpinYaw = 0.0F;

    public Zenith() {
        super("Zenith", false);
    }

    @Override
    public void onEnabled() {
        if (mc.thePlayer != null) {
            spinYaw = mc.thePlayer.rotationYaw;
            prevSpinYaw = spinYaw;
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null || mc.theWorld == null) return;
        if (event.getType() != EventType.PRE) return;

        prevSpinYaw = spinYaw;
        spinYaw += speed.getValue();

        // setRotation: サーバーにはspinYawを送信、一人称カメラは変更しない（postUpdateで復元）
        event.setRotation(spinYaw, pitch.getValue(), 50);
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null) return;
        // 三人称のみ: プレイヤーモデルのボディとヘッドをスピン
        if (mc.gameSettings.thirdPersonView != 0) {
            mc.thePlayer.renderYawOffset = spinYaw;
            mc.thePlayer.prevRenderYawOffset = prevSpinYaw;
            mc.thePlayer.rotationYawHead = spinYaw;
            mc.thePlayer.prevRotationYawHead = prevSpinYaw;
        }
    }
}