package io.github.mjoe92.easypdfbox;

import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.util.Calendar;

import org.junit.jupiter.api.Test;

class EasyDocumentUnitTest {

    @Test
    void testConvertDocumentWithNoContent() throws IOException {
        EasyDocument document = new EasyDocument(50);

        document.setDocumentInformation("sample creator", "sample author", "sample producer", "sample title", "sample subject",
                Calendar.getInstance(), "sample keywords", null);

        byte[] result = document.convert(8096);

        assertNull(result);
    }
}
