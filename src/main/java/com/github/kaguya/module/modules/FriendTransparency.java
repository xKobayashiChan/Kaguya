package com.github.kaguya.module.modules;

import com.github.kaguya.event.EventTarget;
import com.github.kaguya.events.RenderLivingEvent;
import com.github.kaguya.module.Module;
import com.github.kaguya.property.properties.FloatProperty;
import com.github.kaguya.util.TeamUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;

public class FriendTransparency extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final FloatProperty alpha = new FloatProperty("alpha", 0.3f, 0.0f, 1.0f);
    public final FloatProperty range = new FloatProperty("range", 10.0f, 1.0f, 20.0f);

    public FriendTransparency() {
        super("Ftrans", false);
    }

    private boolean shouldApply(EntityPlayer player) {
        if (player == mc.thePlayer || player == mc.getRenderViewEntity()) return false;
        if (player.deathTime > 0) return false;
        if (!TeamUtil.isFriend(player)) return false;
        return mc.getRenderViewEntity().getDistanceToEntity(player) <= range.getValue();
    }

    @EventTarget
    public void onRenderLiving(RenderLivingEvent event) {
        if (!this.isEnabled()) return;
        if (!(event.getEntity() instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) event.getEntity();
        if (!shouldApply(player)) return;

        switch (event.getType()) {
            case PRE:
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                GL11.glColor4f(1.0f, 1.0f, 1.0f, alpha.getValue());
                break;
            case POST:
                GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
                GL11.glDisable(GL11.GL_BLEND);
                break;
        }
    }
}
