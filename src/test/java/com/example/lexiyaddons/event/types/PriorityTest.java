package com.example.lexiyaddons.event.types;

import org.junit.Test;

import static org.junit.Assert.*;

public class PriorityTest {

    @Test
    public void highestIsZero() {
        assertEquals(0, Priority.HIGHEST);
    }

    @Test
    public void highIsOne() {
        assertEquals(1, Priority.HIGH);
    }

    @Test
    public void mediumIsTwo() {
        assertEquals(2, Priority.MEDIUM);
    }

    @Test
    public void lowIsThree() {
        assertEquals(3, Priority.LOW);
    }

    @Test
    public void lowestIsFour() {
        assertEquals(4, Priority.LOWEST);
    }

    @Test
    public void valueArrayContainsAllPriorities() {
        assertEquals(5, Priority.VALUE_ARRAY.length);
        assertEquals(Priority.HIGHEST, Priority.VALUE_ARRAY[0]);
        assertEquals(Priority.HIGH, Priority.VALUE_ARRAY[1]);
        assertEquals(Priority.MEDIUM, Priority.VALUE_ARRAY[2]);
        assertEquals(Priority.LOW, Priority.VALUE_ARRAY[3]);
        assertEquals(Priority.LOWEST, Priority.VALUE_ARRAY[4]);
    }

    @Test
    public void priorityOrderIsAscending() {
        assertTrue(Priority.HIGHEST < Priority.HIGH);
        assertTrue(Priority.HIGH < Priority.MEDIUM);
        assertTrue(Priority.MEDIUM < Priority.LOW);
        assertTrue(Priority.LOW < Priority.LOWEST);
    }
}
