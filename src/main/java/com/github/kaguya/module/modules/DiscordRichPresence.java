package com.github.kaguya.module.modules;

import com.github.kaguya.discord.DiscordIPC;
import com.github.kaguya.event.EventTarget;
import com.github.kaguya.events.TickEvent;
import com.github.kaguya.event.types.EventType;
import com.github.kaguya.module.Module;
import com.github.kaguya.property.properties.BooleanProperty;
import com.github.kaguya.property.properties.TextProperty;
import com.github.kaguya.util.TimerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

public class DiscordRichPresence extends Module {

    private static final Minecraft mc = Minecraft.getMinecraft();

    public final TextProperty appId      = new TextProperty("appId", "1490352070098682058");
    public final BooleanProperty showServer = new BooleanProperty("showServer", true);

    private DiscordIPC ipc;
    private final TimerUtil updateTimer = new TimerUtil();
    private long startTimestamp;

    private String lastDetails = "";
    private String lastState   = "";

    public DiscordRichPresence() {
        super("DiscordRPC", false, true);
    }

    @Override
    public void onEnabled() {
        startTimestamp = System.currentTimeMillis() / 1000L;
        lastDetails = "";
        lastState   = "";
        String id = appId.getValue().trim();
        if (!id.isEmpty()) {
            ipc = new DiscordIPC(id);
            ipc.start(); // non-blocking – connects on background thread
        }
    }

    @Override
    public void onDisabled() {
        if (ipc != null) {
            ipc.stop(); // non-blocking
            ipc = null;
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (event.getType() != EventType.PRE || !this.isEnabled()) return;
        if (ipc == null) return;

        String details = buildDetails();
        String state   = buildState();
        boolean changed = !details.equals(lastDetails) || !state.equals(lastState);

        if (changed || updateTimer.hasTimeElapsed(15_000L)) {
            updateTimer.reset();
            lastDetails = details;
            lastState   = state;
            // setActivity is non-blocking (queues to background thread)
            ipc.setActivity(details, state, startTimestamp, "minecraft", "Minecraft 1.8.9");
        }
    }

    private String buildDetails() {
        if (mc.thePlayer == null) return "In Menu";
        return "Playing Minecraft 1.8.9";
    }

    private String buildState() {
        if (!showServer.getValue() || mc.thePlayer == null) return "";
        ServerData serverData = mc.getCurrentServerData();
        if (serverData != null) return "On: " + serverData.serverIP;
        if (mc.isSingleplayer()) return "Singleplayer";
        return "";
    }
}
