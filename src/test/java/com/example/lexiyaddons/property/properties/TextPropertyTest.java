package com.example.lexiyaddons.property.properties;

import com.google.gson.JsonObject;
import org.junit.Test;

import static org.junit.Assert.*;

public class TextPropertyTest {

    @Test
    public void constructorSetsNameAndValue() {
        TextProperty prop = new TextProperty("label", "hello");
        assertEquals("label", prop.getName());
        assertEquals("hello", prop.getValue());
    }

    @Test
    public void getValuePromptReturnsText() {
        TextProperty prop = new TextProperty("label", "test");
        assertEquals("text", prop.getValuePrompt());
    }

    @Test
    public void formatValueShowsWhiteText() {
        TextProperty prop = new TextProperty("label", "world");
        assertEquals("&fworld", prop.formatValue());
    }

    @Test
    public void setValueChangesText() {
        TextProperty prop = new TextProperty("label", "old");
        assertTrue(prop.setValue("new"));
        assertEquals("new", prop.getValue());
    }

    // --- parseString ---

    @Test
    public void parseStringSetsValue() {
        TextProperty prop = new TextProperty("label", "before");
        assertTrue(prop.parseString("after"));
        assertEquals("after", prop.getValue());
    }

    @Test
    public void parseStringAcceptsEmptyString() {
        TextProperty prop = new TextProperty("label", "something");
        assertTrue(prop.parseString(""));
        assertEquals("", prop.getValue());
    }

    @Test
    public void parseStringAcceptsSpecialCharacters() {
        TextProperty prop = new TextProperty("label", "plain");
        assertTrue(prop.parseString("hello!@#$%^&*()"));
        assertEquals("hello!@#$%^&*()", prop.getValue());
    }

    // --- JSON ---

    @Test
    public void writeToJson() {
        TextProperty prop = new TextProperty("label", "test-value");
        JsonObject json = new JsonObject();
        prop.write(json);
        assertEquals("test-value", json.get("label").getAsString());
    }

    @Test
    public void readFromJson() {
        TextProperty prop = new TextProperty("label", "old");
        JsonObject json = new JsonObject();
        json.addProperty("label", "new-value");
        prop.read(json);
        assertEquals("new-value", prop.getValue());
    }

    // --- visibility ---

    @Test
    public void isVisibleTrueByDefault() {
        TextProperty prop = new TextProperty("label", "test");
        assertTrue(prop.isVisible());
    }

    @Test
    public void isVisibleRespectsChecker() {
        TextProperty prop = new TextProperty("label", "test", () -> false);
        assertFalse(prop.isVisible());
    }
}
