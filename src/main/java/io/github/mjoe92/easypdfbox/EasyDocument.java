package io.github.mjoe92.easypdfbox;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

/**
 * The API for easy {@link PDDocument} creation which encapsulates the PDFBox functionalities.
 */
public class EasyDocument {

    private static final PDRectangle PDF_RECT = PDRectangle.A4;
    private static final DocText NEW_LINE = DocText.of(TextType.NEW_LINE);
    private static final DocText PAGE_BREAK = DocText.of(TextType.PAGE_BREAK);
    private static final DocText INSERT_PAGE = DocText.of(TextType.INSERT_PAGE);

    /** Map where a universal width of a char is saved. This can be used to precisely calculate text bounds. */
    private static final Map<Character, Float> CHAR_WIDTH_MAP = new ConcurrentHashMap<>(128);

    private final float marginTop;
    private final float marginRight;
    private final float marginBottom;
    private final float marginLeft;

    private final Queue<DocText> textBuffer;
    private final Queue<byte[]> pageBuffer;

    /** Header appears on every site. */
    private final List<DocText> header;
    /** Footers appears on every site. */
    private final List<DocText> footer;

    private final PDDocument document;
    private final PDType0Font normalFont;
    private final PDType0Font boldFont;

    /** Cursor for the current y position of the document, used to further advance in the document. */
    private float yCursor;
    /** Current pages to insert immediately, when it's not null. */
    private byte[] pages;
    private PDDocumentInformation information;

    public EasyDocument(float margin) {
        this(margin, margin);
    }

    public EasyDocument(float marginVertical, float marginHorizontal) {
        this(marginVertical, marginHorizontal, marginVertical, marginHorizontal);
    }

    public EasyDocument(float marginTop, float marginRight, float marginBottom, float marginLeft) {
        this.marginTop = marginTop;
        this.marginRight = marginRight;
        this.marginBottom = marginBottom;
        this.marginLeft = marginLeft;

        header = new ArrayList<>(8);
        footer = new ArrayList<>(8);
        textBuffer = new LinkedList<>();
        pageBuffer = new LinkedList<>();

        document = new PDDocument();
        try (InputStream normalFontStream = getClass().getResourceAsStream("calibri.ttf");
                InputStream boldFontStream = getClass().getResourceAsStream("calibri-bold.ttf")) {
            normalFont = PDType0Font.load(document, normalFontStream);
            boldFont = PDType0Font.load(document, boldFontStream);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not load font", e);
        }
    }

    /**
     * @param lines
     *         the string that contains multiple lines seperated with \n.
     * @param delimiter
     *         the delimiter
     * @param indent
     *         the indent
     */
    public void addList(String lines, String delimiter, float indent) {
        addList(lines, false, false, delimiter, indent);
    }

    /**
     * @param lines
     *         the string that contains multiple lines seperated with \n.
     * @param bold
     *         <code>true</code>, whether the text is bold
     * @param underlined
     *         <code>true</code>, whether the text is underlined
     * @param delimiter
     *         the delimiter
     * @param indent
     *         the indent
     */
    public void addList(String lines, boolean bold, boolean underlined, String delimiter, float indent) {
        float xStart = marginLeft + indent;

        for (String item : lines.split(Constants.NEW_LINE)) {
            Collection<FontFragment> fontFragment = createSingleFontFragment(item.length(), bold, underlined);
            toBuffer(delimiter + item, xStart, TextType.LIST, fontFragment);
        }
    }

    /**
     * @param items
     *         the list items to add
     * @param delimiter
     *         the delimiter
     * @param indent
     *         the indent
     */
    public void addList(Collection<String> items, String delimiter, float indent) {
        addList(items, false, false, delimiter, indent);
    }

    /**
     * @param items
     *         the list items to add
     * @param bold
     *         <code>true</code>, whether the text is bold
     * @param underlined
     *         <code>true</code>, whether the text is underlined
     * @param delimiter
     *         the delimiter
     * @param indent
     *         the indent
     */
    public void addList(Collection<String> items, boolean bold, boolean underlined, String delimiter, float indent) {
        float xStart = marginLeft + indent;

        for (String item : items) {
            Collection<FontFragment> fontFragment = createSingleFontFragment(item.length(), bold, underlined);
            toBuffer(delimiter + item, xStart, TextType.LIST, fontFragment);
        }
    }

    /**
     * @param pageData
     *         the pages to add in bytes
     */
    public void addNewPages(byte[] pageData) {
        pageBuffer.add(pageData);
        textBuffer.add(INSERT_PAGE);
    }

    /**
     * Adds a new line.
     */
    public void addNewline() {
        textBuffer.add(NEW_LINE);
    }

    /**
     * @param text
     *         the paragraph
     */
    public void addParagraph(String text) {
        addParagraph(text, false, false);
    }

    /**
     * @param text
     *         the paragraph
     * @param bold
     *         <code>true</code>, whether the text is bold
     * @param underlined
     *         <code>true</code>, whether the text is underlined
     */
    public void addParagraph(String text, boolean bold, boolean underlined) {
        Collection<FontFragment> fontFragment = createSingleFontFragment(text.length(), bold, underlined);
        toBuffer(text, marginLeft, TextType.PARAGRAPH, fontFragment);
    }

    /**
     * @param text
     *         the text
     */
    public void addHeading(String text) {
        addHeading(text, false, false);
    }

    /**
     * @param text
     *         the text
     * @param bold
     *         <code>true</code>, whether the text is bold
     * @param underlined
     *         <code>true</code>, whether the text is underlined
     */
    public void addHeading(String text, boolean bold, boolean underlined) {
        Collection<FontFragment> fontFragment = createSingleFontFragment(text.length(), bold, underlined);
        toBuffer(text, marginLeft, TextType.HEADING, fontFragment);
    }

    /**
     * @param text
     *         the text
     */
    public void addSubHeading(String text) {
        addSubHeading(text, false, false);
    }

    /**
     * @param text
     *         the text
     * @param bold
     *         <code>true</code>, whether the text is bold
     * @param underlined
     *         <code>true</code>, whether the text is underlined
     */
    public void addSubHeading(String text, boolean bold, boolean underlined) {
        Collection<FontFragment> fontFragment = createSingleFontFragment(text.length(), bold, underlined);
        toBuffer(text, marginLeft, TextType.SUB_HEADING, fontFragment);
    }

    /** Inserts a break point for the page. */
    public void addPageBreak() {
        textBuffer.add(PAGE_BREAK);
    }

    /** @return the converted data in bytes of the PDF document */
    public byte[] convert(int size) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(size);
                PDDocument pdfDocument = createDocument()) {
            if (pdfDocument == null) {
                return null;
            }
            
            pdfDocument.save(outputStream);
            return outputStream.toByteArray();
        }
    }

    /**
     * Sets the {@link PDDocumentInformation} properties for the PDF.
     *
     * @param creator
     *         the document creator
     * @param author
     *         the document author
     * @param producer
     *         the document producer
     * @param title
     *         the document title
     * @param subject
     *         the document subject
     * @param creationDate
     *         the {@link Calendar} date of creation
     * @param keywords
     *         the document keywords
     * @param trapped
     *         the document trapped information
     */
    public void setDocumentInformation(String creator, String author, String producer, String title, String subject, Calendar creationDate, String keywords, Boolean trapped) {
        if (information != null) {
            String simpleName = PDDocumentInformation.class.getSimpleName();
            throw new IllegalStateException(simpleName + " can be configured once!");
        }

        information = new PDDocumentInformation();
        information.setCreator(creator);
        information.setAuthor(author);
        information.setProducer(producer);
        information.setTitle(title);
        information.setSubject(subject);
        information.setCreationDate(creationDate);
        information.setModificationDate(creationDate);
        information.setKeywords(keywords);

        String trappedStr;
        if (trapped == null) {
            trappedStr = "Unknown";
        } else if (trapped) {
            trappedStr = "True";
        } else {
            trappedStr = "False";
        }
        
        information.setTrapped(trappedStr);
    }

    /**
     * @param text
     *         the footer text to set
     */
    public void setFooter(String text) {
        setFooter(text, false, false);
    }

    /**
     * @param text
     *         the footer text to set
     * @param bold
     *         <code>true</code>, whether the text is bold
     * @param underlined
     *         <code>true</code>, whether the text is underlined
     */
    public void setFooter(String text, boolean bold, boolean underlined) {
        footer.clear();

        Collection<String> wrappedText = wrapText(text, marginLeft, TextType.FOOTER);
        for (String line : wrappedText) {
            Collection<FontFragment> fontFragment = createSingleFontFragment(line.length(), bold, underlined);
            DocText docText = new DocText(line, marginLeft, TextType.FOOTER, fontFragment);
            footer.add(docText);
        }
    }

    /**
     * @param text
     *         the header text to set
     * @param bold
     *         <code>true</code>, whether the text is bold
     * @param underlined
     *         <code>true</code>, whether the text is underlined
     */
    public void setHeader(String text, boolean bold, boolean underlined) {
        header.clear();

        Collection<String> wrappedText = wrapText(text, marginLeft, TextType.HEADER);
        for (String line : wrappedText) {
            Collection<FontFragment> fontFragment = createSingleFontFragment(line.length(), bold, underlined);
            DocText docText = new DocText(line, marginLeft, TextType.HEADER, fontFragment);
            header.add(docText);
        }
    }

    /**
     * @param title
     *         the underlined title
     * @param text
     *         the text to set as value
     */
    public void addUnderlinedTitleColonValue(String title, String text) {
        String line = title + Constants.COLON_SPACE + text;
        FontFragment titleFont = new FontFragment(title.length(), normalFont, true);
        FontFragment textFont = new FontFragment(line.length() - title.length(), normalFont, false);

        toBuffer(line, marginLeft, TextType.PARAGRAPH, List.of(titleFont, textFont));
    }

    private void appendLine(PDPageContentStream contentStream, DocText docText) throws IOException {
        contentStream.beginText();
        // for now, we assume the font set for a text line
        PDType0Font font = docText.fontFragments().iterator().next().font();
        contentStream.setFont(font, docText.type().getFontSize());
        contentStream.newLineAtOffset(docText.xStart(), yCursor);
        String text = docText.text();

        try {
            contentStream.showText(text);
        } finally {
            contentStream.endText();
        }
    }

    private PDDocument createDocument() throws IOException {
        PDFMergerUtility merger = new PDFMergerUtility();

        do {
            if (pages == null) {
                PDPage page = createPage(document);
                if (page != null) {
                    document.addPage(page);
                }
            } else {
                try (PDDocument toAppend = Loader.loadPDF(pages)) {
                    merger.appendDocument(document, toAppend);
                }

                pages = null;
            }
        } while (!textBuffer.isEmpty() || pages != null);

        if (document.getNumberOfPages() == 0) {
            return null;
        }

        if (information != null) {
            document.setDocumentInformation(information);
        }

        return document;
    }

    private PDPage createPage(PDDocument document) throws IOException {
        yCursor = PDF_RECT.getHeight();

        PDPage page = new PDPage(PDF_RECT);
        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            do {
                DocText docText = textBuffer.poll();
                if (docText == null) {
                    return null;
                }
                
                switch (docText.type()) {
                    case PAGE_BREAK -> yCursor = 0;
                    case NEW_LINE -> yCursor -= docText.type().getLeading();
                    case INSERT_PAGE -> {
                        pages = pageBuffer.poll();
                        // corner case: the insertable pages must be added directly without blank pages in between
                        if (yCursor == PDF_RECT.getHeight()) {
                            return null;
                        } else {
                            yCursor = 0;
                        }
                    }
                    default -> {
                        appendLine(contentStream, docText);
                        drawLine(contentStream, docText);

                        yCursor -= docText.type().getLeading();
                    }
                }
            } while (!textBuffer.isEmpty() && yCursor > marginBottom);

            writeHeaderAndFooter(contentStream);
        }

        return page;
    }

    private void drawLine(PDPageContentStream contentStream, DocText docText) throws IOException {
        // index of line drawing must be controlled by the fragments ALSO whenever text is wrapped,
        // but for now we assume only short / one-line texts
        float startIndex = marginLeft;
        for (FontFragment fontFragment : docText.fontFragments()) {
            if (!fontFragment.underlined()) {
                startIndex += fontFragment.width();
                continue;
            }

            float lineWidth = docText.type().getFontSize() / TextType.HEADING.getFontSize();
            float underlineY = yCursor - 2 * lineWidth;

            contentStream.setLineWidth(lineWidth);
            contentStream.moveTo(startIndex, underlineY);

            float width = startIndex;
            String text = docText.text();
            for (int index = 0; index < text.length(); index++) {
                if (index >= fontFragment.width()) {
                    break;
                }

                char charAt = text.charAt(index);
                width += getCharFontWidth(charAt, docText.type());
            }

            contentStream.lineTo(width, underlineY);
            contentStream.stroke();

            startIndex += fontFragment.width();
        }
    }

    private void toBuffer(String text, float xStart, TextType type, Collection<FontFragment> fontFragments) {
        Collection<String> wrappedText = wrapText(text, xStart, type);

        for (String line : wrappedText) {
            DocText docText = new DocText(line, xStart, type, fontFragments);
            textBuffer.add(docText);
        }
    }

    /**
     * Wraps the text based on any \n and visual text bounds and returns a list of lines.
     *
     * @param text
     *         the text
     * @param xStart
     *         where the text should start, typically calculating in the left margin.
     * @param type
     *         the text type
     * @return a list of lines (wrapped)
     */
    private Collection<String> wrapText(String text, float xStart, TextType type) {
        float width = PDF_RECT.getWidth();
        float reservedSpacing = xStart + marginRight;
        width -= reservedSpacing;

        // the TAB character seems to be not present in some bold, replacing with spaces here (spaces > tabs)
        String[] lines = text.replace(Constants.TAB, "    ").split(Constants.NEW_LINE);

        Collection<String> wrappedText = new ArrayList<>();
        for (String line : lines) {
            float currentLineWidth = 0;
            int substringIndex = 0;

            for (int index = 0; index < line.length(); index++) {
                char charAt = line.charAt(index);

                currentLineWidth += getCharFontWidth(charAt, type);

                if (currentLineWidth >= width) {
                    // line is too long, chop it down 'till the last fitting word and continue
                    String maxText = line.substring(substringIndex, index).trim();
                    int indexOfSpace = maxText.lastIndexOf(Constants.SPACE);
                    if (indexOfSpace != -1) {
                        index = substringIndex + indexOfSpace + 1;
                    }

                    String maxWords = line.substring(substringIndex, index);
                    wrappedText.add(maxWords);

                    currentLineWidth = 0;
                    substringIndex = index;
                }
            }

            // last (remaining) line
            wrappedText.add(line.substring(substringIndex));
        }

        return wrappedText;
    }

    private float getCharFontWidth(char charAt, TextType type) {
        float length = CHAR_WIDTH_MAP.computeIfAbsent(charAt, this::calculateCharWidth);
        return length * type.getFontSize();
    }

    private float calculateCharWidth(char charAt) {
        try {
            return normalFont.getStringWidth(String.valueOf(charAt)) / 1000f;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not compute char width", e);
        }
    }

    private void writeHeaderAndFooter(PDPageContentStream contentStream) throws IOException {
        yCursor = PDF_RECT.getHeight();
        for (DocText docText : header) {
            yCursor -= docText.type().getLeading();

            appendLine(contentStream, docText);
        }

        yCursor = 0;
        for (DocText docText : footer.reversed()) {
            yCursor += docText.type().getLeading();

            appendLine(contentStream, docText);
        }
    }

    private Collection<FontFragment> createSingleFontFragment(int endIndex, boolean bold, boolean underlined) {
        PDType0Font font = bold ? boldFont : normalFont;
        return List.of(new FontFragment(endIndex, font, underlined));
    }
}
