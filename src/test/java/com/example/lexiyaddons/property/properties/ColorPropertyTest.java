package com.example.lexiyaddons.property.properties;

import com.google.gson.JsonObject;
import org.junit.Test;

import static org.junit.Assert.*;

public class ColorPropertyTest {

    @Test
    public void constructorSetsNameAndValue() {
        ColorProperty prop = new ColorProperty("color", 0xFF0000);
        assertEquals("color", prop.getName());
        assertEquals(Integer.valueOf(0xFF0000), prop.getValue());
    }

    @Test
    public void setValueWithinRange() {
        ColorProperty prop = new ColorProperty("color", 0);
        assertTrue(prop.setValue(0x00FF00));
        assertEquals(Integer.valueOf(0x00FF00), prop.getValue());
    }

    @Test
    public void setValueRejectsAboveMax() {
        // Validator: rgb <= 16777215 (0xFFFFFF)
        ColorProperty prop = new ColorProperty("color", 0);
        assertFalse(prop.setValue(16777216)); // 0x1000000
        assertEquals(Integer.valueOf(0), prop.getValue());
    }

    @Test
    public void setValueAcceptsMaxValue() {
        ColorProperty prop = new ColorProperty("color", 0);
        assertTrue(prop.setValue(16777215)); // 0xFFFFFF
        assertEquals(Integer.valueOf(16777215), prop.getValue());
    }

    @Test
    public void setValueAcceptsZero() {
        ColorProperty prop = new ColorProperty("color", 0xFF0000);
        assertTrue(prop.setValue(0));
        assertEquals(Integer.valueOf(0), prop.getValue());
    }

    @Test
    public void getValuePromptReturnsRGB() {
        ColorProperty prop = new ColorProperty("color", 0);
        assertEquals("RGB", prop.getValuePrompt());
    }

    // --- formatValue ---

    @Test
    public void formatValueShowsHexComponents() {
        ColorProperty prop = new ColorProperty("color", 0xFF8800);
        String formatted = prop.formatValue();
        // Should contain the hex components split into R, G, B
        assertTrue(formatted.contains("FF"));
        assertTrue(formatted.contains("88"));
        assertTrue(formatted.contains("00"));
    }

    @Test
    public void formatValueForBlack() {
        ColorProperty prop = new ColorProperty("color", 0x000000);
        String formatted = prop.formatValue();
        assertEquals("&c00&a00&900", formatted);
    }

    @Test
    public void formatValueForWhite() {
        ColorProperty prop = new ColorProperty("color", 0xFFFFFF);
        String formatted = prop.formatValue();
        assertEquals("&cFF&aFF&9FF", formatted);
    }

    // --- parseString ---

    @Test
    public void parseStringParsesHex() {
        ColorProperty prop = new ColorProperty("color", 0);
        assertTrue(prop.parseString("FF0000"));
        assertEquals(Integer.valueOf(0xFF0000), prop.getValue());
    }

    @Test
    public void parseStringParsesHexWithHash() {
        ColorProperty prop = new ColorProperty("color", 0);
        assertTrue(prop.parseString("#00FF00"));
        assertEquals(Integer.valueOf(0x00FF00), prop.getValue());
    }

    @Test
    public void parseStringParsesLowercaseHex() {
        ColorProperty prop = new ColorProperty("color", 0);
        assertTrue(prop.parseString("ff8800"));
        assertEquals(Integer.valueOf(0xFF8800), prop.getValue());
    }

    // --- JSON ---

    @Test
    public void writeToJson() {
        ColorProperty prop = new ColorProperty("color", 0xFF0000);
        JsonObject json = new JsonObject();
        prop.write(json);
        assertEquals("FF0000", json.get("color").getAsString());
    }

    @Test
    public void writeToJsonPadsWithZeros() {
        ColorProperty prop = new ColorProperty("color", 0x000100);
        JsonObject json = new JsonObject();
        prop.write(json);
        assertEquals("000100", json.get("color").getAsString());
    }

    @Test
    public void readFromJson() {
        ColorProperty prop = new ColorProperty("color", 0);
        JsonObject json = new JsonObject();
        json.addProperty("color", "00FF00");
        prop.read(json);
        assertEquals(Integer.valueOf(0x00FF00), prop.getValue());
    }

    @Test
    public void writeAndReadRoundTrip() {
        ColorProperty prop1 = new ColorProperty("color", 0xABCDEF);
        JsonObject json = new JsonObject();
        prop1.write(json);

        ColorProperty prop2 = new ColorProperty("color", 0);
        prop2.read(json);
        assertEquals(prop1.getValue(), prop2.getValue());
    }

    // --- visibility ---

    @Test
    public void isVisibleTrueByDefault() {
        ColorProperty prop = new ColorProperty("color", 0);
        assertTrue(prop.isVisible());
    }

    @Test
    public void isVisibleRespectsChecker() {
        ColorProperty prop = new ColorProperty("color", 0, () -> false);
        assertFalse(prop.isVisible());
    }
}
