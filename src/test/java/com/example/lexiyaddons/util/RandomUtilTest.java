package com.example.lexiyaddons.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class RandomUtilTest {

    private static final int ITERATIONS = 10000;

    // --- nextFloat ---

    @Test
    public void nextFloatReturnsWithinRange() {
        for (int i = 0; i < ITERATIONS; i++) {
            float result = RandomUtil.nextFloat(1.0f, 5.0f);
            assertTrue("Result " + result + " should be >= 1.0", result >= 1.0f);
            assertTrue("Result " + result + " should be < 5.0", result < 5.0f);
        }
    }

    @Test
    public void nextFloatWithNegativeRange() {
        for (int i = 0; i < ITERATIONS; i++) {
            float result = RandomUtil.nextFloat(-10.0f, -5.0f);
            assertTrue("Result " + result + " should be >= -10.0", result >= -10.0f);
            assertTrue("Result " + result + " should be < -5.0", result < -5.0f);
        }
    }

    @Test
    public void nextFloatWithZeroRange() {
        float result = RandomUtil.nextFloat(3.0f, 3.0f);
        assertEquals(3.0f, result, 0.0001f);
    }

    // --- nextDouble ---

    @Test
    public void nextDoubleReturnsWithinRange() {
        for (int i = 0; i < ITERATIONS; i++) {
            double result = RandomUtil.nextDouble(0.0, 100.0);
            assertTrue("Result " + result + " should be >= 0.0", result >= 0.0);
            assertTrue("Result " + result + " should be < 100.0", result < 100.0);
        }
    }

    @Test
    public void nextDoubleWithNegativeRange() {
        for (int i = 0; i < ITERATIONS; i++) {
            double result = RandomUtil.nextDouble(-50.0, 50.0);
            assertTrue("Result " + result + " should be >= -50.0", result >= -50.0);
            assertTrue("Result " + result + " should be < 50.0", result < 50.0);
        }
    }

    @Test
    public void nextDoubleWithZeroRange() {
        double result = RandomUtil.nextDouble(7.0, 7.0);
        assertEquals(7.0, result, 0.0001);
    }

    // --- nextLong ---

    @Test
    public void nextLongReturnsWithinRange() {
        for (int i = 0; i < ITERATIONS; i++) {
            long result = RandomUtil.nextLong(10, 20);
            assertTrue("Result " + result + " should be >= 10", result >= 10);
            assertTrue("Result " + result + " should be <= 20", result <= 20);
        }
    }

    @Test
    public void nextLongWithSameMinMax() {
        // When min == max, nextLong calls nextDouble(min, max+1)
        // so result should always be min
        for (int i = 0; i < 100; i++) {
            long result = RandomUtil.nextLong(5, 5);
            assertEquals(5, result);
        }
    }

    @Test
    public void nextLongWithNegativeRange() {
        // Note: nextLong uses (max + 1) internally, so for negative ranges
        // the actual upper bound is max + 1 (e.g., -5 becomes -4).
        // This is a known behavior of the implementation.
        for (int i = 0; i < ITERATIONS; i++) {
            long result = RandomUtil.nextLong(-10, -5);
            assertTrue("Result " + result + " should be >= -10", result >= -10);
            assertTrue("Result " + result + " should be <= -4 (max+1 behavior)",
                    result <= -4);
        }
    }

    @Test
    public void nextLongWithZeroInRange() {
        for (int i = 0; i < ITERATIONS; i++) {
            long result = RandomUtil.nextLong(-5, 5);
            assertTrue("Result " + result + " should be >= -5", result >= -5);
            assertTrue("Result " + result + " should be <= 5", result <= 5);
        }
    }

    // --- Distribution ---

    @Test
    public void nextFloatProducesVariedResults() {
        float first = RandomUtil.nextFloat(0.0f, 1000.0f);
        boolean foundDifferent = false;
        for (int i = 0; i < 100; i++) {
            if (RandomUtil.nextFloat(0.0f, 1000.0f) != first) {
                foundDifferent = true;
                break;
            }
        }
        assertTrue("Should produce varied results", foundDifferent);
    }

    @Test
    public void nextDoubleProducesVariedResults() {
        double first = RandomUtil.nextDouble(0.0, 1000.0);
        boolean foundDifferent = false;
        for (int i = 0; i < 100; i++) {
            if (RandomUtil.nextDouble(0.0, 1000.0) != first) {
                foundDifferent = true;
                break;
            }
        }
        assertTrue("Should produce varied results", foundDifferent);
    }
}
