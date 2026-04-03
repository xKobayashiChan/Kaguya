package com.example.lexiyaddons.property.properties;

import com.google.gson.JsonObject;
import org.junit.Test;

import static org.junit.Assert.*;

public class ModePropertyTest {

    private static final String[] MODES = {"Alpha", "Beta", "Gamma"};

    @Test
    public void constructorSetsNameAndValue() {
        ModeProperty prop = new ModeProperty("mode", 0, MODES);
        assertEquals("mode", prop.getName());
        assertEquals(Integer.valueOf(0), prop.getValue());
    }

    @Test
    public void getModeStringReturnsCorrectMode() {
        ModeProperty prop = new ModeProperty("mode", 1, MODES);
        assertEquals("Beta", prop.getModeString());
    }

    @Test
    public void getModeStringReturnsEmptyForOutOfBounds() {
        ModeProperty prop = new ModeProperty("mode", 5, MODES);
        assertEquals("", prop.getModeString());
    }

    @Test
    public void getModeStringReturnsEmptyForNegativeIndex() {
        ModeProperty prop = new ModeProperty("mode", -1, MODES);
        assertEquals("", prop.getModeString());
    }

    @Test
    public void getValuePromptJoinsModes() {
        ModeProperty prop = new ModeProperty("mode", 0, MODES);
        assertEquals("Alpha, Beta, Gamma", prop.getValuePrompt());
    }

    // --- formatValue ---

    @Test
    public void formatValueShowsModeName() {
        ModeProperty prop = new ModeProperty("mode", 0, MODES);
        assertEquals("&9Alpha", prop.formatValue());
    }

    @Test
    public void formatValueShowsQuestionMarkForInvalidIndex() {
        ModeProperty prop = new ModeProperty("mode", 10, MODES);
        assertEquals("&4?", prop.formatValue());
    }

    // --- parseString ---

    @Test
    public void parseStringMatchesExactMode() {
        ModeProperty prop = new ModeProperty("mode", 0, MODES);
        assertTrue(prop.parseString("Beta"));
        assertEquals(Integer.valueOf(1), prop.getValue());
    }

    @Test
    public void parseStringIsCaseInsensitive() {
        ModeProperty prop = new ModeProperty("mode", 0, MODES);
        assertTrue(prop.parseString("gamma"));
        assertEquals(Integer.valueOf(2), prop.getValue());
    }

    @Test
    public void parseStringReturnsFalseForUnknownMode() {
        ModeProperty prop = new ModeProperty("mode", 0, MODES);
        assertFalse(prop.parseString("Delta"));
        assertEquals(Integer.valueOf(0), prop.getValue()); // unchanged
    }

    @Test
    public void parseStringIgnoresUnderscores() {
        String[] modes = {"Long_Name", "Short"};
        ModeProperty prop = new ModeProperty("mode", 0, modes);
        assertTrue(prop.parseString("LongName"));
        assertEquals(Integer.valueOf(0), prop.getValue());
    }

    // --- nextMode / previousMode ---

    @Test
    public void nextModeIncrementsIndex() {
        ModeProperty prop = new ModeProperty("mode", 0, MODES);
        prop.nextMode();
        assertEquals(Integer.valueOf(1), prop.getValue());
    }

    @Test
    public void nextModeWrapsAround() {
        ModeProperty prop = new ModeProperty("mode", 2, MODES);
        prop.nextMode();
        assertEquals(Integer.valueOf(0), prop.getValue());
    }

    @Test
    public void previousModeDecrementsIndex() {
        ModeProperty prop = new ModeProperty("mode", 2, MODES);
        prop.previousMode();
        assertEquals(Integer.valueOf(1), prop.getValue());
    }

    @Test
    public void previousModeWrapsAround() {
        ModeProperty prop = new ModeProperty("mode", 0, MODES);
        prop.previousMode();
        assertEquals(Integer.valueOf(2), prop.getValue());
    }

    @Test
    public void nextAndPreviousRoundTrip() {
        ModeProperty prop = new ModeProperty("mode", 1, MODES);
        prop.nextMode();
        prop.previousMode();
        assertEquals(Integer.valueOf(1), prop.getValue());
    }

    // --- JSON ---

    @Test
    public void writeToJson() {
        ModeProperty prop = new ModeProperty("mode", 1, MODES);
        JsonObject json = new JsonObject();
        prop.write(json);
        assertEquals("Beta", json.get("mode").getAsString());
    }

    @Test
    public void readFromJson() {
        ModeProperty prop = new ModeProperty("mode", 0, MODES);
        JsonObject json = new JsonObject();
        json.addProperty("mode", "Gamma");
        prop.read(json);
        assertEquals(Integer.valueOf(2), prop.getValue());
    }

    @Test
    public void readFromJsonReturnsFalseForUnknown() {
        ModeProperty prop = new ModeProperty("mode", 0, MODES);
        JsonObject json = new JsonObject();
        json.addProperty("mode", "Unknown");
        assertFalse(prop.read(json));
        assertEquals(Integer.valueOf(0), prop.getValue());
    }

    // --- visibility ---

    @Test
    public void isVisibleTrueByDefault() {
        ModeProperty prop = new ModeProperty("mode", 0, MODES);
        assertTrue(prop.isVisible());
    }

    @Test
    public void isVisibleRespectsChecker() {
        ModeProperty prop = new ModeProperty("mode", 0, MODES, () -> false);
        assertFalse(prop.isVisible());
    }
}
