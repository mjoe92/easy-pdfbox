package io.github.mjoe92.easypdfbox;

import java.util.Collection;

/**
 * The buffer object for writing document text.
 *
 * @param text
 * @param xStart
 * @param type
 * @param fontFragments
 */
public record DocText(String text, float xStart, TextType type, Collection<FontFragment> fontFragments) {

    /** For empty-text related semantic operations */
    public static DocText of(TextType type) {
        return new DocText(null, 0, type, null);
    }
}
