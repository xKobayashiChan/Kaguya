package com.github.kaguya.command.commands;

import com.github.kaguya.Kaguya;
import com.github.kaguya.command.Command;
import com.github.kaguya.util.ChatUtil;
import net.minecraft.client.Minecraft;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

public class VclipCommand extends Command {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final DecimalFormat df = new DecimalFormat("#.##", new DecimalFormatSymbols(Locale.US));

    public VclipCommand() {
        super(new ArrayList<>(Collections.singletonList("vclip")));
    }

    @Override
    public void runCommand(ArrayList<String> args) {
        if (args.size() >= 2) {
            double distance = 0.0;
            try {
                distance = Double.parseDouble(args.get(1));
            } catch (NumberFormatException e) {
            } finally {
                mc.thePlayer.setPositionAndUpdate(mc.thePlayer.posX, mc.thePlayer.posY + distance, mc.thePlayer.posZ);
                ChatUtil.sendFormatted(String.format("%sClipped (%s blocks)", Kaguya.clientName, df.format(distance)));
            }
            return;
        }
        ChatUtil.sendFormatted(
                String.format("%sUsage: .%s <&odistance&r>&r", Kaguya.clientName, args.get(0).toLowerCase(Locale.ROOT))
        );
    }
}
