package com.example.lexiyaddons.management;

import com.example.lexiyaddons.enums.ChatColors;

import java.awt.*;
import java.io.File;

public class FriendManager extends PlayerFileManager {
    public FriendManager() {
        super(new File("./config/Myau/", "friends.txt"), new Color(ChatColors.DARK_GREEN.toAwtColor()));
    }
}
