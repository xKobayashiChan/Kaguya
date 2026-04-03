package com.example.lexiyaddons.enums;

import org.junit.Test;

import static org.junit.Assert.*;

public class ChatColorsTest {

    // --- Enum values ---

    @Test
    public void allColorEnumsExist() {
        assertNotNull(ChatColors.BLACK);
        assertNotNull(ChatColors.DARK_BLUE);
        assertNotNull(ChatColors.DARK_GREEN);
        assertNotNull(ChatColors.DARK_AQUA);
        assertNotNull(ChatColors.DARK_RED);
        assertNotNull(ChatColors.DARK_PURPLE);
        assertNotNull(ChatColors.GOLD);
        assertNotNull(ChatColors.GRAY);
        assertNotNull(ChatColors.DARK_GRAY);
        assertNotNull(ChatColors.BLUE);
        assertNotNull(ChatColors.GREEN);
        assertNotNull(ChatColors.AQUA);
        assertNotNull(ChatColors.RED);
        assertNotNull(ChatColors.LIGHT_PURPLE);
        assertNotNull(ChatColors.YELLOW);
        assertNotNull(ChatColors.WHITE);
    }

    @Test
    public void formattingCodesExist() {
        assertNotNull(ChatColors.MAGIC);
        assertNotNull(ChatColors.BOLD);
        assertNotNull(ChatColors.STRIKETHROUGH);
        assertNotNull(ChatColors.UNDERLINE);
        assertNotNull(ChatColors.ITALIC);
        assertNotNull(ChatColors.RESET);
    }

    // --- toString ---

    @Test
    public void toStringReturnsColorCode() {
        String redCode = ChatColors.RED.toString();
        assertEquals(2, redCode.length());
        assertEquals('\u00A7', redCode.charAt(0)); // §
        assertEquals('c', redCode.charAt(1));
    }

    @Test
    public void toStringBlackCode() {
        String code = ChatColors.BLACK.toString();
        assertEquals("\u00A70", code);
    }

    @Test
    public void toStringWhiteCode() {
        String code = ChatColors.WHITE.toString();
        assertEquals("\u00A7f", code);
    }

    @Test
    public void toStringBoldCode() {
        String code = ChatColors.BOLD.toString();
        assertEquals("\u00A7l", code);
    }

    @Test
    public void toStringResetCode() {
        String code = ChatColors.RESET.toString();
        assertEquals("\u00A7r", code);
    }

    // --- toAwtColor ---

    @Test
    public void toAwtColorBlackIsNegative() {
        assertEquals(-16777216, ChatColors.BLACK.toAwtColor());
    }

    @Test
    public void toAwtColorWhiteIsMinusOne() {
        assertEquals(-1, ChatColors.WHITE.toAwtColor());
    }

    @Test
    public void toAwtColorFormattingCodesAreZero() {
        assertEquals(0, ChatColors.MAGIC.toAwtColor());
        assertEquals(0, ChatColors.BOLD.toAwtColor());
        assertEquals(0, ChatColors.STRIKETHROUGH.toAwtColor());
        assertEquals(0, ChatColors.UNDERLINE.toAwtColor());
        assertEquals(0, ChatColors.ITALIC.toAwtColor());
        assertEquals(0, ChatColors.RESET.toAwtColor());
    }

    // --- COLOR_CHAR ---

    @Test
    public void colorCharIsSectionSign() {
        assertEquals('\u00A7', ChatColors.COLOR_CHAR);
    }

    // --- formatColor ---

    @Test
    public void formatColorConvertsAmpersandCodes() {
        String result = ChatColors.formatColor("&aHello &cWorld");
        assertEquals("\u00A7aHello \u00A7cWorld", result);
    }

    @Test
    public void formatColorConvertsUppercaseCodes() {
        String result = ChatColors.formatColor("&AHello");
        assertEquals("\u00A7aHello", result);
    }

    @Test
    public void formatColorIgnoresInvalidCodes() {
        String result = ChatColors.formatColor("&xHello");
        assertEquals("&xHello", result);
    }

    @Test
    public void formatColorConvertsNumberCodes() {
        String result = ChatColors.formatColor("&0Black &1Blue &9Blue2");
        assertEquals("\u00A70Black \u00A71Blue \u00A79Blue2", result);
    }

    @Test
    public void formatColorConvertsFormattingCodes() {
        String result = ChatColors.formatColor("&lBold &nUnderline &oItalic &rReset");
        assertEquals("\u00A7lBold \u00A7nUnderline \u00A7oItalic \u00A7rReset", result);
    }

    @Test
    public void formatColorHandlesEmptyString() {
        String result = ChatColors.formatColor("");
        assertEquals("", result);
    }

    @Test
    public void formatColorHandlesNoAmps() {
        String result = ChatColors.formatColor("Hello World");
        assertEquals("Hello World", result);
    }

    @Test
    public void formatColorHandlesTrailingAmp() {
        // & at end of string should not cause issues (loop checks i < length - 1)
        String result = ChatColors.formatColor("Hello&");
        assertEquals("Hello&", result);
    }

    @Test
    public void formatColorHandlesMultipleConsecutive() {
        String result = ChatColors.formatColor("&a&lGreen Bold");
        assertEquals("\u00A7a\u00A7lGreen Bold", result);
    }
}
