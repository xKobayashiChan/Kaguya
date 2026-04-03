package com.example.lexiyaddons.property.properties;

import com.google.gson.JsonObject;
import org.junit.Test;

import static org.junit.Assert.*;

public class FloatPropertyTest {

    @Test
    public void constructorSetsNameAndValue() {
        FloatProperty prop = new FloatProperty("speed", 1.5f, 0.0f, 10.0f);
        assertEquals("speed", prop.getName());
        assertEquals(1.5f, prop.getValue(), 0.001f);
    }

    @Test
    public void minimumAndMaximumAreStored() {
        FloatProperty prop = new FloatProperty("range", 5.0f, 1.0f, 20.0f);
        assertEquals(1.0f, prop.getMinimum(), 0.001f);
        assertEquals(20.0f, prop.getMaximum(), 0.001f);
    }

    @Test
    public void setValueWithinRange() {
        FloatProperty prop = new FloatProperty("speed", 1.0f, 0.0f, 10.0f);
        assertTrue(prop.setValue(5.0f));
        assertEquals(5.0f, prop.getValue(), 0.001f);
    }

    @Test
    public void setValueRejectsNegative() {
        // Validator checks floatV >= 0
        FloatProperty prop = new FloatProperty("speed", 1.0f, 0.0f, 10.0f);
        assertFalse(prop.setValue(-1.0f));
        assertEquals(1.0f, prop.getValue(), 0.001f); // unchanged
    }

    @Test
    public void getValuePromptShowsRange() {
        FloatProperty prop = new FloatProperty("speed", 1.0f, 0.5f, 9.5f);
        assertEquals("0.5-9.5", prop.getValuePrompt());
    }

    @Test
    public void formatValueShowsColoredValue() {
        FloatProperty prop = new FloatProperty("speed", 3.5f, 0.0f, 10.0f);
        assertEquals("&63.5", prop.formatValue());
    }

    // --- parseString ---

    @Test
    public void parseStringSetsFloat() {
        FloatProperty prop = new FloatProperty("speed", 1.0f, 0.0f, 10.0f);
        assertTrue(prop.parseString("7.5"));
        assertEquals(7.5f, prop.getValue(), 0.001f);
    }

    @Test(expected = NumberFormatException.class)
    public void parseStringThrowsOnInvalidInput() {
        FloatProperty prop = new FloatProperty("speed", 1.0f, 0.0f, 10.0f);
        prop.parseString("not-a-number");
    }

    // --- JSON ---

    @Test
    public void writeToJson() {
        FloatProperty prop = new FloatProperty("speed", 4.2f, 0.0f, 10.0f);
        JsonObject json = new JsonObject();
        prop.write(json);
        assertEquals(4.2f, json.get("speed").getAsFloat(), 0.001f);
    }

    @Test
    public void readFromJson() {
        FloatProperty prop = new FloatProperty("speed", 1.0f, 0.0f, 10.0f);
        JsonObject json = new JsonObject();
        json.addProperty("speed", 8.3f);
        prop.read(json);
        assertEquals(8.3f, prop.getValue(), 0.01f);
    }

    // --- visibility ---

    @Test
    public void isVisibleTrueByDefault() {
        FloatProperty prop = new FloatProperty("speed", 1.0f, 0.0f, 10.0f);
        assertTrue(prop.isVisible());
    }

    @Test
    public void isVisibleRespectsChecker() {
        FloatProperty prop = new FloatProperty("speed", 1.0f, 0.0f, 10.0f, () -> false);
        assertFalse(prop.isVisible());
    }
}
