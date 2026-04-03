package com.example.lexiyaddons.property.properties;

import com.google.gson.JsonObject;
import org.junit.Test;

import static org.junit.Assert.*;

public class BooleanPropertyTest {

    @Test
    public void constructorSetsNameAndValue() {
        BooleanProperty prop = new BooleanProperty("test-flag", true);
        assertEquals("test-flag", prop.getName());
        assertTrue(prop.getValue());
    }

    @Test
    public void defaultValueFalse() {
        BooleanProperty prop = new BooleanProperty("disabled", false);
        assertFalse(prop.getValue());
    }

    @Test
    public void setValueChangesValue() {
        BooleanProperty prop = new BooleanProperty("toggle", false);
        assertTrue(prop.setValue(true));
        assertTrue(prop.getValue());
    }

    @Test
    public void getValuePromptReturnsTrueFalse() {
        BooleanProperty prop = new BooleanProperty("test", true);
        assertEquals("true/false", prop.getValuePrompt());
    }

    // --- formatValue ---

    @Test
    public void formatValueTrueReturnsGreenTrue() {
        BooleanProperty prop = new BooleanProperty("test", true);
        assertEquals("&atrue", prop.formatValue());
    }

    @Test
    public void formatValueFalseReturnsRedFalse() {
        BooleanProperty prop = new BooleanProperty("test", false);
        assertEquals("&cfalse", prop.formatValue());
    }

    // --- parseString ---

    @Test
    public void parseStringNullTogglesValue() {
        BooleanProperty prop = new BooleanProperty("toggle", true);
        prop.parseString(null);
        assertFalse(prop.getValue());
    }

    @Test
    public void parseStringNullTogglesBackToTrue() {
        BooleanProperty prop = new BooleanProperty("toggle", false);
        prop.parseString(null);
        assertTrue(prop.getValue());
    }

    @Test
    public void parseStringTrueSetsTrueIgnoringCase() {
        BooleanProperty prop = new BooleanProperty("test", false);
        assertTrue(prop.parseString("TRUE"));
        assertTrue(prop.getValue());
    }

    @Test
    public void parseStringOnSetsTrue() {
        BooleanProperty prop = new BooleanProperty("test", false);
        assertTrue(prop.parseString("on"));
        assertTrue(prop.getValue());
    }

    @Test
    public void parseStringOneSetsTrue() {
        BooleanProperty prop = new BooleanProperty("test", false);
        assertTrue(prop.parseString("1"));
        assertTrue(prop.getValue());
    }

    @Test
    public void parseStringFalseSetsFalse() {
        BooleanProperty prop = new BooleanProperty("test", true);
        assertTrue(prop.parseString("false"));
        assertFalse(prop.getValue());
    }

    @Test
    public void parseStringOffSetsFalse() {
        BooleanProperty prop = new BooleanProperty("test", true);
        assertTrue(prop.parseString("off"));
        assertFalse(prop.getValue());
    }

    @Test
    public void parseStringZeroSetsFalse() {
        BooleanProperty prop = new BooleanProperty("test", true);
        assertTrue(prop.parseString("0"));
        assertFalse(prop.getValue());
    }

    @Test
    public void parseStringInvalidReturnsFalse() {
        BooleanProperty prop = new BooleanProperty("test", true);
        assertFalse(prop.parseString("invalid"));
        assertTrue(prop.getValue()); // unchanged
    }

    // --- JSON serialization ---

    @Test
    public void writeTrueToJson() {
        BooleanProperty prop = new BooleanProperty("enabled", true);
        JsonObject json = new JsonObject();
        prop.write(json);
        assertTrue(json.get("enabled").getAsBoolean());
    }

    @Test
    public void writeFalseToJson() {
        BooleanProperty prop = new BooleanProperty("enabled", false);
        JsonObject json = new JsonObject();
        prop.write(json);
        assertFalse(json.get("enabled").getAsBoolean());
    }

    @Test
    public void readFromJson() {
        BooleanProperty prop = new BooleanProperty("enabled", false);
        JsonObject json = new JsonObject();
        json.addProperty("enabled", true);
        prop.read(json);
        assertTrue(prop.getValue());
    }

    // --- visibility ---

    @Test
    public void isVisibleTrueWhenNoChecker() {
        BooleanProperty prop = new BooleanProperty("test", true);
        assertTrue(prop.isVisible());
    }

    @Test
    public void isVisibleRespectsChecker() {
        BooleanProperty prop = new BooleanProperty("test", true, () -> false);
        assertFalse(prop.isVisible());
    }

    @Test
    public void isVisibleTrueWhenCheckerReturnsTrue() {
        BooleanProperty prop = new BooleanProperty("test", true, () -> true);
        assertTrue(prop.isVisible());
    }
}
