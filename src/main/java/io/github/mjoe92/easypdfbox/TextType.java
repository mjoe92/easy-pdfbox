package io.github.mjoe92.easypdfbox;

/**
 * The type of the document text containing the necessary properties for the page layout.
 */
public enum TextType {
    HEADING(16),
    SUB_HEADING(15),
    PARAGRAPH(12),
    LIST(11),
    HEADER(8),
    FOOTER(8),
    PAGE_NUMBER(10),
    NEW_LINE(18),
    PAGE_BREAK(0),
    INSERT_PAGE(0);

    private final float fontSize;

    TextType(float fontSize) {
        this.fontSize = fontSize;
    }

    public float getFontSize() {
        return fontSize;
    }

    /**
     * Text leading that express the vertical space between adjacent lines.
     * This is a typical phrase in the typography, more information can be found online.
     *
     * @return the leading
     */
    public float getLeading() {
        return fontSize * 1.33f;
    }

}
