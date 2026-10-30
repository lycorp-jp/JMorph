// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

public class CsvParserTest {

    @Test
    public void testParse() {
        // exec
        List<String> actual = CsvParser.parse("a,b,c");

        // verify
        assertEquals(List.of("a", "b", "c"), actual);
    }

    @Test
    public void testParseQuotedComma() {
        // exec
        List<String> actual = CsvParser.parse("\"a,b\",c");

        // verify
        assertEquals(List.of("a,b", "c"), actual);
    }

    @Test
    public void testParseEscapedQuote() {
        // exec
        List<String> actual = CsvParser.parse("\"a\"\"b\",c");

        // verify
        assertEquals(List.of("a\"b", "c"), actual);
    }

    @Test
    public void testParseSpacesAfterQuote() {
        // exec
        List<String> actual = CsvParser.parse("\"a\" ,b");

        // verify
        assertEquals(List.of("a", "b"), actual);
    }

    @Test
    public void testParseEmptyField() {
        // exec
        List<String> actual = CsvParser.parse("a,,c");

        // verify
        assertEquals(List.of("a", "", "c"), actual);
    }

    @Test
    public void testParseEmptyQuotedField() {
        // exec
        List<String> actual = CsvParser.parse("\"\",b");

        // verify
        assertEquals(List.of("", "b"), actual);
    }
}
