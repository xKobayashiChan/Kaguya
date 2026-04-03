package com.example.lexiyaddons.data;

import org.junit.Test;

import static org.junit.Assert.*;

public class BoxTest {

    @Test
    public void constructorSetsValue() {
        Box<String> box = new Box<>("hello");
        assertEquals("hello", box.value);
    }

    @Test
    public void canHoldInteger() {
        Box<Integer> box = new Box<>(42);
        assertEquals(Integer.valueOf(42), box.value);
    }

    @Test
    public void canHoldNull() {
        Box<Object> box = new Box<>(null);
        assertNull(box.value);
    }

    @Test
    public void valueCanBeModified() {
        Box<String> box = new Box<>("initial");
        box.value = "modified";
        assertEquals("modified", box.value);
    }

    @Test
    public void canHoldComplexTypes() {
        Box<int[]> box = new Box<>(new int[]{1, 2, 3});
        assertArrayEquals(new int[]{1, 2, 3}, box.value);
    }

    @Test
    public void canHoldBoolean() {
        Box<Boolean> box = new Box<>(true);
        assertTrue(box.value);
    }

    @Test
    public void valueTypeChangesWithAssignment() {
        Box<Object> box = new Box<>("string");
        assertEquals("string", box.value);
        box.value = 123;
        assertEquals(123, box.value);
    }
}
