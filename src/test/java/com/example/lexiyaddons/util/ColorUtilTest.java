package com.example.lexiyaddons.util;

import org.junit.Test;

import java.awt.Color;

import static org.junit.Assert.*;

public class ColorUtilTest {

    // --- Constants ---

    @Test
    public void constantRedIsCorrect() {
        assertEquals(255, ColorUtil.RED.getRed());
        assertEquals(0, ColorUtil.RED.getGreen());
        assertEquals(0, ColorUtil.RED.getBlue());
    }

    @Test
    public void constantGoldIsCorrect() {
        assertEquals(255, ColorUtil.GOLD.getRed());
        assertEquals(165, ColorUtil.GOLD.getGreen());
        assertEquals(0, ColorUtil.GOLD.getBlue());
    }

    @Test
    public void constantYellowIsCorrect() {
        assertEquals(255, ColorUtil.YELLOW.getRed());
        assertEquals(255, ColorUtil.YELLOW.getGreen());
        assertEquals(0, ColorUtil.YELLOW.getBlue());
    }

    @Test
    public void constantGreenIsCorrect() {
        assertEquals(0, ColorUtil.GREEN.getRed());
        assertEquals(255, ColorUtil.GREEN.getGreen());
        assertEquals(0, ColorUtil.GREEN.getBlue());
    }

    // --- fromHSB ---

    @Test
    public void fromHSBReturnsRedForHue0() {
        Color color = ColorUtil.fromHSB(0.0f, 1.0f, 1.0f);
        assertEquals(255, color.getRed());
        assertEquals(0, color.getGreen());
        assertEquals(0, color.getBlue());
    }

    @Test
    public void fromHSBReturnsBlackForZeroBrightness() {
        Color color = ColorUtil.fromHSB(0.5f, 1.0f, 0.0f);
        assertEquals(0, color.getRed());
        assertEquals(0, color.getGreen());
        assertEquals(0, color.getBlue());
    }

    @Test
    public void fromHSBReturnsWhiteForZeroSaturationFullBrightness() {
        Color color = ColorUtil.fromHSB(0.0f, 0.0f, 1.0f);
        assertEquals(255, color.getRed());
        assertEquals(255, color.getGreen());
        assertEquals(255, color.getBlue());
    }

    // --- interpolate ---

    @Test
    public void interpolateAtZeroReturnsStartColor() {
        Color result = ColorUtil.interpolate(0.0f, Color.RED, Color.BLUE);
        assertEquals(Color.RED.getRed(), result.getRed());
        assertEquals(Color.RED.getGreen(), result.getGreen());
        assertEquals(Color.RED.getBlue(), result.getBlue());
    }

    @Test
    public void interpolateAtOneReturnsEndColor() {
        Color result = ColorUtil.interpolate(1.0f, Color.RED, Color.BLUE);
        assertEquals(Color.BLUE.getRed(), result.getRed());
        assertEquals(Color.BLUE.getGreen(), result.getGreen());
        assertEquals(Color.BLUE.getBlue(), result.getBlue());
    }

    @Test
    public void interpolateAtHalfReturnsMidpoint() {
        Color start = new Color(0, 0, 0);
        Color end = new Color(200, 100, 50);
        Color result = ColorUtil.interpolate(0.5f, start, end);
        assertEquals(100, result.getRed());
        assertEquals(50, result.getGreen());
        assertEquals(25, result.getBlue());
    }

    @Test
    public void interpolateClampsProgressBelowZero() {
        Color result = ColorUtil.interpolate(-1.0f, Color.RED, Color.BLUE);
        assertEquals(Color.RED.getRed(), result.getRed());
        assertEquals(Color.RED.getGreen(), result.getGreen());
        assertEquals(Color.RED.getBlue(), result.getBlue());
    }

    @Test
    public void interpolateClampsProgressAboveOne() {
        Color result = ColorUtil.interpolate(2.0f, Color.RED, Color.BLUE);
        assertEquals(Color.BLUE.getRed(), result.getRed());
        assertEquals(Color.BLUE.getGreen(), result.getGreen());
        assertEquals(Color.BLUE.getBlue(), result.getBlue());
    }

    // --- getHealthBlend ---

    @Test
    public void healthBlendReturnsGreenForHighHealth() {
        Color result = ColorUtil.getHealthBlend(1.0f);
        assertEquals(ColorUtil.GREEN, result);
    }

    @Test
    public void healthBlendReturnsGreenFor90Percent() {
        Color result = ColorUtil.getHealthBlend(0.9f);
        assertEquals(ColorUtil.GREEN, result);
    }

    @Test
    public void healthBlendReturnsYellowFor45to55Percent() {
        Color result = ColorUtil.getHealthBlend(0.50f);
        assertEquals(ColorUtil.YELLOW, result);
    }

    @Test
    public void healthBlendReturnsRedForLowHealth() {
        Color result = ColorUtil.getHealthBlend(0.05f);
        assertEquals(ColorUtil.RED, result);
    }

    @Test
    public void healthBlendReturnsRedForZeroHealth() {
        Color result = ColorUtil.getHealthBlend(0.0f);
        assertEquals(ColorUtil.RED, result);
    }

    @Test
    public void healthBlendInterpolatesBetweenYellowAndGreen() {
        Color result = ColorUtil.getHealthBlend(0.7f);
        // Should be between yellow and green
        assertTrue(result.getGreen() == 255);
        assertTrue(result.getRed() > 0 && result.getRed() < 255);
    }

    @Test
    public void healthBlendInterpolatesBetweenRedAndYellow() {
        Color result = ColorUtil.getHealthBlend(0.3f);
        // Should be between red and yellow
        assertTrue(result.getRed() == 255);
        assertTrue(result.getGreen() > 0 && result.getGreen() < 255);
    }

    // --- darker ---

    @Test
    public void darkerReducesBrightness() {
        Color original = new Color(200, 100, 50);
        Color result = ColorUtil.darker(original, 0.5f);
        assertEquals(100, result.getRed());
        assertEquals(50, result.getGreen());
        assertEquals(25, result.getBlue());
        assertEquals(original.getAlpha(), result.getAlpha());
    }

    @Test
    public void darkerPreservesAlpha() {
        Color original = new Color(200, 100, 50, 128);
        Color result = ColorUtil.darker(original, 0.5f);
        assertEquals(128, result.getAlpha());
    }

    // --- scale ---

    @Test
    public void scaleMultipliesComponents() {
        Color original = new Color(100, 200, 50);
        Color result = ColorUtil.scale(original, 2.0f, 255);
        assertEquals(200, result.getRed());
        assertEquals(255, result.getGreen()); // clamped to 255
        assertEquals(100, result.getBlue());
        assertEquals(255, result.getAlpha());
    }

    @Test
    public void scaleClampsToZero() {
        Color original = new Color(100, 200, 50);
        Color result = ColorUtil.scale(original, -1.0f, 255);
        assertEquals(0, result.getRed());
        assertEquals(0, result.getGreen());
        assertEquals(0, result.getBlue());
    }

    @Test
    public void scaleClampsTo255() {
        Color original = new Color(200, 200, 200);
        Color result = ColorUtil.scale(original, 5.0f, 200);
        assertEquals(255, result.getRed());
        assertEquals(255, result.getGreen());
        assertEquals(255, result.getBlue());
        assertEquals(200, result.getAlpha());
    }

    @Test
    public void scaleWithZeroFactorReturnsBlack() {
        Color original = new Color(100, 200, 50);
        Color result = ColorUtil.scale(original, 0.0f, 100);
        assertEquals(0, result.getRed());
        assertEquals(0, result.getGreen());
        assertEquals(0, result.getBlue());
        assertEquals(100, result.getAlpha());
    }
}
