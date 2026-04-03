package com.example.lexiyaddons.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class TimerUtilTest {

    @Test
    public void newTimerReportsLargeElapsedTime() {
        // A new TimerUtil with lastMS=0 should report large elapsed time
        TimerUtil timer = new TimerUtil();
        assertTrue("New timer should report large elapsed time (since epoch)",
                timer.getElapsedTime() > 0);
    }

    @Test
    public void resetMakesElapsedTimeSmall() throws InterruptedException {
        TimerUtil timer = new TimerUtil();
        timer.reset();
        long elapsed = timer.getElapsedTime();
        assertTrue("Elapsed time after reset should be very small, was: " + elapsed,
                elapsed < 100);
    }

    @Test
    public void hasTimeElapsedReturnsTrueForNewTimer() {
        // A new TimerUtil has lastMS=0 (epoch), so any reasonable ms has elapsed
        TimerUtil timer = new TimerUtil();
        assertTrue(timer.hasTimeElapsed(1000));
    }

    @Test
    public void hasTimeElapsedReturnsFalseAfterReset() {
        TimerUtil timer = new TimerUtil();
        timer.reset();
        assertFalse("Should not have elapsed 10 seconds right after reset",
                timer.hasTimeElapsed(10000));
    }

    @Test
    public void hasTimeElapsedWithZeroMs() {
        TimerUtil timer = new TimerUtil();
        timer.reset();
        assertTrue("0ms should always have elapsed", timer.hasTimeElapsed(0));
    }

    @Test
    public void setTimeResetsToEpoch() {
        TimerUtil timer = new TimerUtil();
        timer.reset();
        timer.setTime();
        // After setTime(), lastMS is 0 (epoch), so elapsed time should be very large
        assertTrue("After setTime(), elapsed time should be since epoch",
                timer.getElapsedTime() > 1000000);
    }

    @Test
    public void getElapsedTimeIncreasesOverTime() throws InterruptedException {
        TimerUtil timer = new TimerUtil();
        timer.reset();
        long elapsed1 = timer.getElapsedTime();
        Thread.sleep(50);
        long elapsed2 = timer.getElapsedTime();
        assertTrue("Elapsed time should increase, was " + elapsed1 + " then " + elapsed2,
                elapsed2 >= elapsed1);
    }

    @Test
    public void multipleResetsWork() {
        TimerUtil timer = new TimerUtil();
        timer.reset();
        long elapsed1 = timer.getElapsedTime();
        timer.reset();
        long elapsed2 = timer.getElapsedTime();
        assertTrue("Second reset should give small elapsed time too",
                elapsed2 < 100);
    }
}
