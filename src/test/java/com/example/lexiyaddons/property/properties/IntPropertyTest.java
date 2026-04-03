package com.example.lexiyaddons.property.properties;

import com.google.gson.JsonObject;
import org.junit.Test;

import static org.junit.Assert.*;

public class IntPropertyTest {

    @Test
    public void constructorSetsNameAndValue() {
        IntProperty prop = new IntProperty("count", 5, 0, 100);
        assertEquals("count", prop.getName());
        assertEquals(Integer.valueOf(5), prop.getValue());
    }

    @Test
    public void minimumAndMaximumAreStored() {
        IntProperty prop = new IntProperty("count", 10, 1, 50);
        assertEquals(Integer.valueOf(1), prop.getMinimum());
        assertEquals(Integer.valueOf(50), prop.getMaximum());
    }

    @Test
    public void setValueWithinRange() {
        IntProperty prop = new IntProperty("count", 5, 0, 100);
        assertTrue(prop.setValue(42));
        assertEquals(Integer.valueOf(42), prop.getValue());
    }

    @Test
    public void setValueAtMinimum() {
        IntProperty prop = new IntProperty("count", 5, 0, 100);
        assertTrue(prop.setValue(0));
        assertEquals(Integer.valueOf(0), prop.getValue());
    }

    @Test
    public void setValueAtMaximum() {
        IntProperty prop = new IntProperty("count", 5, 0, 100);
        assertTrue(prop.setValue(100));
        assertEquals(Integer.valueOf(100), prop.getValue());
    }

    @Test
    public void setValueBelowMinimumIsRejected() {
        IntProperty prop = new IntProperty("count", 5, 0, 100);
        assertFalse(prop.setValue(-1));
        assertEquals(Integer.valueOf(5), prop.getValue());
    }

    @Test
    public void setValueAboveMaximumIsRejected() {
        IntProperty prop = new IntProperty("count", 5, 0, 100);
        assertFalse(prop.setValue(101));
        assertEquals(Integer.valueOf(5), prop.getValue());
    }

    @Test
    public void getValuePromptShowsRange() {
        IntProperty prop = new IntProperty("count", 5, 1, 20);
        assertEquals("1-20", prop.getValuePrompt());
    }

    @Test
    public void formatValueShowsColoredValue() {
        IntProperty prop = new IntProperty("count", 42, 0, 100);
        assertEquals("&e42", prop.formatValue());
    }

    // --- parseString ---

    @Test
    public void parseStringSetsInteger() {
        IntProperty prop = new IntProperty("count", 5, 0, 100);
        assertTrue(prop.parseString("50"));
        assertEquals(Integer.valueOf(50), prop.getValue());
    }

    @Test
    public void parseStringRejectsOutOfRange() {
        IntProperty prop = new IntProperty("count", 5, 0, 100);
        assertFalse(prop.parseString("200"));
        assertEquals(Integer.valueOf(5), prop.getValue());
    }

    @Test(expected = NumberFormatException.class)
    public void parseStringThrowsOnInvalidInput() {
        IntProperty prop = new IntProperty("count", 5, 0, 100);
        prop.parseString("abc");
    }

    // --- JSON ---

    @Test
    public void writeToJson() {
        IntProperty prop = new IntProperty("count", 42, 0, 100);
        JsonObject json = new JsonObject();
        prop.write(json);
        assertEquals(42, json.get("count").getAsInt());
    }

    @Test
    public void readFromJson() {
        IntProperty prop = new IntProperty("count", 5, 0, 100);
        JsonObject json = new JsonObject();
        json.addProperty("count", 77);
        prop.read(json);
        assertEquals(Integer.valueOf(77), prop.getValue());
    }

    @Test
    public void readFromJsonRejectsOutOfRange() {
        IntProperty prop = new IntProperty("count", 5, 0, 100);
        JsonObject json = new JsonObject();
        json.addProperty("count", 200);
        assertFalse(prop.read(json));
        assertEquals(Integer.valueOf(5), prop.getValue());
    }

    // --- visibility ---

    @Test
    public void isVisibleTrueByDefault() {
        IntProperty prop = new IntProperty("count", 5, 0, 100);
        assertTrue(prop.isVisible());
    }

    @Test
    public void isVisibleRespectsChecker() {
        IntProperty prop = new IntProperty("count", 5, 0, 100, () -> false);
        assertFalse(prop.isVisible());
    }
}
