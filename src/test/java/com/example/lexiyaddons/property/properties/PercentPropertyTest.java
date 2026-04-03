package com.example.lexiyaddons.property.properties;

import com.google.gson.JsonObject;
import org.junit.Test;

import static org.junit.Assert.*;

public class PercentPropertyTest {

    @Test
    public void constructorSetsNameAndValue() {
        PercentProperty prop = new PercentProperty("opacity", 50);
        assertEquals("opacity", prop.getName());
        assertEquals(Integer.valueOf(50), prop.getValue());
    }

    @Test
    public void defaultRangeIs0To100() {
        PercentProperty prop = new PercentProperty("opacity", 50);
        assertEquals(Integer.valueOf(0), prop.getMinimum());
        assertEquals(Integer.valueOf(100), prop.getMaximum());
    }

    @Test
    public void customRangeIsStored() {
        PercentProperty prop = new PercentProperty("opacity", 50, 10, 90, null);
        assertEquals(Integer.valueOf(10), prop.getMinimum());
        assertEquals(Integer.valueOf(90), prop.getMaximum());
    }

    @Test
    public void setValueWithinRangeSucceeds() {
        PercentProperty prop = new PercentProperty("opacity", 50);
        assertTrue(prop.setValue(75));
        assertEquals(Integer.valueOf(75), prop.getValue());
    }

    @Test
    public void setValueAtBoundaries() {
        PercentProperty prop = new PercentProperty("opacity", 50);
        assertTrue(prop.setValue(0));
        assertEquals(Integer.valueOf(0), prop.getValue());
        assertTrue(prop.setValue(100));
        assertEquals(Integer.valueOf(100), prop.getValue());
    }

    @Test
    public void setValueBelowMinimumIsRejected() {
        PercentProperty prop = new PercentProperty("opacity", 50);
        assertFalse(prop.setValue(-1));
        assertEquals(Integer.valueOf(50), prop.getValue());
    }

    @Test
    public void setValueAboveMaximumIsRejected() {
        PercentProperty prop = new PercentProperty("opacity", 50);
        assertFalse(prop.setValue(101));
        assertEquals(Integer.valueOf(50), prop.getValue());
    }

    @Test
    public void getValuePromptShowsPercentRange() {
        PercentProperty prop = new PercentProperty("opacity", 50);
        assertEquals("0-100%", prop.getValuePrompt());
    }

    @Test
    public void formatValueShowsPercentSign() {
        PercentProperty prop = new PercentProperty("opacity", 75);
        assertEquals("&b75%", prop.formatValue());
    }

    // --- parseString ---

    @Test
    public void parseStringSetsValue() {
        PercentProperty prop = new PercentProperty("opacity", 50);
        assertTrue(prop.parseString("80"));
        assertEquals(Integer.valueOf(80), prop.getValue());
    }

    @Test
    public void parseStringStripsPercentSign() {
        PercentProperty prop = new PercentProperty("opacity", 50);
        assertTrue(prop.parseString("60%"));
        assertEquals(Integer.valueOf(60), prop.getValue());
    }

    @Test
    public void parseStringRejectsOutOfRange() {
        PercentProperty prop = new PercentProperty("opacity", 50);
        assertFalse(prop.parseString("150"));
        assertEquals(Integer.valueOf(50), prop.getValue());
    }

    @Test(expected = NumberFormatException.class)
    public void parseStringThrowsOnInvalidInput() {
        PercentProperty prop = new PercentProperty("opacity", 50);
        prop.parseString("abc");
    }

    // --- JSON ---

    @Test
    public void writeToJson() {
        PercentProperty prop = new PercentProperty("opacity", 65);
        JsonObject json = new JsonObject();
        prop.write(json);
        assertEquals(65, json.get("opacity").getAsInt());
    }

    @Test
    public void readFromJson() {
        PercentProperty prop = new PercentProperty("opacity", 50);
        JsonObject json = new JsonObject();
        json.addProperty("opacity", 90);
        prop.read(json);
        assertEquals(Integer.valueOf(90), prop.getValue());
    }

    @Test
    public void readFromJsonRejectsOutOfRange() {
        PercentProperty prop = new PercentProperty("opacity", 50);
        JsonObject json = new JsonObject();
        json.addProperty("opacity", 200);
        assertFalse(prop.read(json));
        assertEquals(Integer.valueOf(50), prop.getValue());
    }

    // --- visibility ---

    @Test
    public void isVisibleTrueByDefault() {
        PercentProperty prop = new PercentProperty("opacity", 50);
        assertTrue(prop.isVisible());
    }

    @Test
    public void isVisibleRespectsChecker() {
        PercentProperty prop = new PercentProperty("opacity", 50, () -> false);
        assertFalse(prop.isVisible());
    }
}
