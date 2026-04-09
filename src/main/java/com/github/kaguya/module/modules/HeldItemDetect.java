package com.github.kaguya.module.modules;

import com.github.kaguya.Kaguya;
import com.github.kaguya.event.EventTarget;
import com.github.kaguya.event.types.EventType;
import com.github.kaguya.events.LoadWorldEvent;
import com.github.kaguya.events.TickEvent;
import com.github.kaguya.module.Module;
import com.github.kaguya.util.ChatUtil;
import com.github.kaguya.util.TeamUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class HeldItemDetect extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    // プレイヤー名 → 前tickのアイテムキー ("splash", "notch", "")
    private final Map<String, String> lastHeld = new HashMap<>();

    public HeldItemDetect() {
        super("HeldItemDetect", false);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.POST) return;
        if (mc.theWorld == null || mc.thePlayer == null) return;

        for (Object obj : mc.theWorld.playerEntities) {
            if (!(obj instanceof EntityPlayer)) continue;
            EntityPlayer player = (EntityPlayer) obj;
            if (player == mc.thePlayer) continue;
            if (TeamUtil.isBot(player)) continue;

            String name = player.getName();
            ItemStack held = player.getHeldItem();

            String current = "";
            if (held != null) {
                if (isSplashPotion(held)) current = "splash";
                else if (isNotchApple(held)) current = "notch";
            }

            String prev = lastHeld.getOrDefault(name, "");
            lastHeld.put(name, current);

            // 前tickと違うアイテムに切り替えた瞬間だけ通知
            if (!current.equals(prev) && !current.isEmpty()) {
                String itemColor = held.getRarity().rarityColor.toString();
                String itemName = itemColor + held.getDisplayName();
                ChatUtil.sendFormatted(
                        String.format("%s&b%s &fis holding a %s&f.", Kaguya.clientName, name, itemName)
                );
            }
        }
    }

    private boolean isSplashPotion(ItemStack stack) {
        return stack.getItem() == Items.potionitem && (stack.getMetadata() & 16384) == 16384;
    }

    private boolean isNotchApple(ItemStack stack) {
        return stack.getItem() == Items.golden_apple && stack.getMetadata() == 1;
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        lastHeld.clear();
    }

    @Override
    public void onDisabled() {
        lastHeld.clear();
    }
}
