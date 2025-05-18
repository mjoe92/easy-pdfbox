package io.github.mjoe92.easypdfbox;

import org.apache.pdfbox.pdmodel.font.PDType0Font;

/**
 * Helps to define the {@link PDType0Font} and underline for an object (e.g. text) in certain char width.
 *
 * @param width
 * @param font
 * @param underlined
 */
public record FontFragment(int width, PDType0Font font, boolean underlined) { }
