package com.example.lexiyaddons.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class LangFallbackTest {

    // --- looksLikeTranslationKey ---

    @Test
    public void looksLikeTranslationKeyReturnsTrueForValidKey() {
        assertTrue(LangFallback.looksLikeTranslationKey("menu.quit"));
    }

    @Test
    public void looksLikeTranslationKeyReturnsTrueForDottedKey() {
        assertTrue(LangFallback.looksLikeTranslationKey("potion.moveSpeed"));
    }

    @Test
    public void looksLikeTranslationKeyReturnsTrueForMultipleDots() {
        assertTrue(LangFallback.looksLikeTranslationKey("enchantment.damage.all"));
    }

    @Test
    public void looksLikeTranslationKeyReturnsTrueForUnderscores() {
        assertTrue(LangFallback.looksLikeTranslationKey("tile.stone_slab.stone"));
    }

    @Test
    public void looksLikeTranslationKeyReturnsTrueForNumbers() {
        assertTrue(LangFallback.looksLikeTranslationKey("item.record13.desc"));
    }

    @Test
    public void looksLikeTranslationKeyReturnsFalseForNull() {
        assertFalse(LangFallback.looksLikeTranslationKey(null));
    }

    @Test
    public void looksLikeTranslationKeyReturnsFalseForEmptyString() {
        assertFalse(LangFallback.looksLikeTranslationKey(""));
    }

    @Test
    public void looksLikeTranslationKeyReturnsFalseForSingleChar() {
        assertFalse(LangFallback.looksLikeTranslationKey("a"));
    }

    @Test
    public void looksLikeTranslationKeyReturnsFalseForTwoChars() {
        assertFalse(LangFallback.looksLikeTranslationKey("ab"));
    }

    @Test
    public void looksLikeTranslationKeyReturnsFalseForSpaces() {
        assertFalse(LangFallback.looksLikeTranslationKey("has spaces.key"));
    }

    @Test
    public void looksLikeTranslationKeyReturnsFalseForNoDot() {
        assertFalse(LangFallback.looksLikeTranslationKey("nodothere"));
    }

    @Test
    public void looksLikeTranslationKeyReturnsFalseForLeadingDigit() {
        assertFalse(LangFallback.looksLikeTranslationKey("1invalid.key"));
    }

    @Test
    public void looksLikeTranslationKeyReturnsFalseForVeryLongString() {
        StringBuilder sb = new StringBuilder("a.");
        for (int i = 0; i < 80; i++) {
            sb.append("x");
        }
        assertFalse(LangFallback.looksLikeTranslationKey(sb.toString()));
    }

    @Test
    public void looksLikeTranslationKeyReturnsFalseForLeadingDot() {
        assertFalse(LangFallback.looksLikeTranslationKey(".leading.dot"));
    }

    @Test
    public void looksLikeTranslationKeyReturnsFalseForSpecialChars() {
        assertFalse(LangFallback.looksLikeTranslationKey("key.with@special"));
    }

    // --- translateFormatted ---

    @Test
    public void translateFormattedReturnsNullForUnknownKey() {
        // Since en_US.lang likely won't be on classpath in test, this should return null
        String result = LangFallback.translateFormatted("nonexistent.key.xyz123");
        assertNull(result);
    }

    @Test
    public void translateReturnsNullForUnknownKey() {
        String result = LangFallback.translate("nonexistent.key.abc456");
        assertNull(result);
    }
}
