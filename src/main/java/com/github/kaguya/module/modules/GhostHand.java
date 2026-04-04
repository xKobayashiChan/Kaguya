package com.github.kaguya.module.modules;

import com.github.kaguya.module.Module;
import com.github.kaguya.util.ItemUtil;
import com.github.kaguya.util.TeamUtil;
import com.github.kaguya.property.properties.BooleanProperty;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

public class GhostHand extends Module {
    public final BooleanProperty teamsOnly = new BooleanProperty("team-only", true);
    public final BooleanProperty ignoreWeapons = new BooleanProperty("ignore-weapons", false);

    public GhostHand() {
        super("GhostHand", false);
    }

    public boolean shouldSkip(Entity entity) {
        return entity instanceof EntityPlayer
                && !TeamUtil.isBot((EntityPlayer) entity)
                && (!this.teamsOnly.getValue() || TeamUtil.isSameTeam((EntityPlayer) entity))
                && (!this.ignoreWeapons.getValue() || !ItemUtil.hasRawUnbreakingEnchant());
    }
}
