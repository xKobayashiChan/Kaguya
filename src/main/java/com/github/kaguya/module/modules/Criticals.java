package com.github.kaguya.module.modules;

import com.github.kaguya.Kaguya;
import com.github.kaguya.event.EventTarget;
import com.github.kaguya.event.types.EventType;
import com.github.kaguya.events.AttackEvent;
import com.github.kaguya.events.PacketEvent;
import com.github.kaguya.mixins.IAccessorC03PacketPlayer;
import com.github.kaguya.module.Module;
import com.github.kaguya.property.properties.FloatProperty;
import com.github.kaguya.property.properties.IntProperty;
import com.github.kaguya.property.properties.ModeProperty;
import com.github.kaguya.util.PacketUtil;
import com.github.kaguya.util.TimerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.client.C03PacketPlayer;

public class Criticals extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{
            "Packet", "NCPPacket", "OldBlocksMC", "OldBlocksMC2", "NoGround",
            "Hop", "TPHop", "Jump", "LowJump", "CustomMotion"
    });
    public final IntProperty delay = new IntProperty("delay", 0, 0, 500);
    public final IntProperty hurtTime = new IntProperty("hurt-time", 10, 0, 10);
    public final FloatProperty customMotionY = new FloatProperty("custom-y", 0.2F, 0.01F, 0.42F);

    private final TimerUtil timer = new TimerUtil();

    public Criticals() {
        super("Criticals", false);
    }

    @Override
    public void onEnabled() {
        if (this.mode.getValue() == 4 && mc.thePlayer != null) {
            mc.thePlayer.jump();
        }
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (!this.isEnabled()) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!(event.getTarget() instanceof EntityLivingBase)) return;

        EntityLivingBase target = (EntityLivingBase) event.getTarget();
        if (!mc.thePlayer.onGround) return;
        if (mc.thePlayer.isUsingItem()) return;
        if (mc.thePlayer.isInWater() || mc.thePlayer.isInLava()) return;
        if (mc.thePlayer.ridingEntity != null) return;
        if (target.hurtTime > this.hurtTime.getValue()) return;

        Fly fly = (Fly) Kaguya.moduleManager.modules.get(Fly.class);
        if (fly != null && fly.isEnabled()) return;

        if (!timer.hasTimeElapsed(this.delay.getValue())) return;

        double x = mc.thePlayer.posX;
        double y = mc.thePlayer.posY;
        double z = mc.thePlayer.posZ;

        switch (this.mode.getValue()) {
            case 0:
                PacketUtil.sendPacket(new C03PacketPlayer.C04PacketPlayerPosition(x, y + 0.0625, z, true));
                PacketUtil.sendPacket(new C03PacketPlayer.C04PacketPlayerPosition(x, y, z, false));
                mc.thePlayer.attackTargetEntityWithCurrentItem(target);
                break;
            case 1:
                PacketUtil.sendPacket(new C03PacketPlayer.C04PacketPlayerPosition(x, y + 0.11, z, false));
                PacketUtil.sendPacket(new C03PacketPlayer.C04PacketPlayerPosition(x, y + 0.1100013579, z, false));
                PacketUtil.sendPacket(new C03PacketPlayer.C04PacketPlayerPosition(x, y + 1.3579E-6, z, false));
                mc.thePlayer.attackTargetEntityWithCurrentItem(target);
                break;
            case 2:
                PacketUtil.sendPacket(new C03PacketPlayer.C04PacketPlayerPosition(x, y + 0.001091981, z, true));
                PacketUtil.sendPacket(new C03PacketPlayer.C04PacketPlayerPosition(x, y, z, false));
                break;
            case 3:
                if (mc.thePlayer.ticksExisted % 4 == 0) {
                    PacketUtil.sendPacket(new C03PacketPlayer.C04PacketPlayerPosition(x, y + 0.0011, z, true));
                    PacketUtil.sendPacket(new C03PacketPlayer.C04PacketPlayerPosition(x, y, z, false));
                }
                break;
            case 4:
                break;
            case 5:
                mc.thePlayer.motionY = 0.1;
                mc.thePlayer.fallDistance = 0.1F;
                mc.thePlayer.onGround = false;
                break;
            case 6:
                PacketUtil.sendPacket(new C03PacketPlayer.C04PacketPlayerPosition(x, y + 0.02, z, false));
                PacketUtil.sendPacket(new C03PacketPlayer.C04PacketPlayerPosition(x, y + 0.01, z, false));
                mc.thePlayer.setPosition(x, y + 0.01, z);
                break;
            case 7:
                mc.thePlayer.motionY = 0.42;
                break;
            case 8:
                mc.thePlayer.motionY = 0.3425;
                break;
            case 9:
                mc.thePlayer.motionY = this.customMotionY.getValue();
                break;
        }

        timer.reset();
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled()) return;
        if (event.getType() != EventType.SEND) return;
        if (this.mode.getValue() == 4 && event.getPacket() instanceof C03PacketPlayer) {
            ((IAccessorC03PacketPlayer) event.getPacket()).setOnGround(false);
        }
    }

    @Override
    public String[] getSuffix() {
        String[] modes = {"Packet", "NCPPacket", "OldBlocksMC", "OldBlocksMC2", "NoGround",
                "Hop", "TPHop", "Jump", "LowJump", "CustomMotion"};
        return new String[]{modes[this.mode.getValue()]};
    }
}
