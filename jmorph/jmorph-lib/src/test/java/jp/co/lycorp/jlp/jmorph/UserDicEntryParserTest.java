// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserDicEntryParserTest {
    @Test
    void testParseNullEntries() {
        // execute
        List<UserDicEntry> actual = UserDicEntryParser.parse((JsonNode) null);

        // verify
        assertEquals(0, actual.size());
    }

    @Test
    void testParseNoEntries() throws IOException {
        // setup
        JsonNode entries = new ObjectMapper().readTree("[]");

        // execute
        List<UserDicEntry> actual = UserDicEntryParser.parse(entries);

        // verify
        assertEquals(0, actual.size());
    }

    @Test
    void testParseOnlyTokens() throws IOException {
        // setup
        JsonNode entries = new ObjectMapper().readTree(
                "[\"架空ランドマーク\",\"架空冒険譚\",\"例示姓\",\"例示名\"]");
        List<UserDicEntry> expected = Arrays.asList(
                new UserDicEntry("架空ランドマーク"),
                new UserDicEntry("架空冒険譚"),
                new UserDicEntry("例示姓"),
                new UserDicEntry("例示名")
        );

        // execute
        List<UserDicEntry> actual = UserDicEntryParser.parse(entries);

        // verify
        assertEquals(expected, actual);
    }

    @Test
    void testParseTokensAndReadings() throws IOException {
        // setup
        JsonNode entries = new ObjectMapper().readTree(
                "[\"架空ランドマーク\",\"架空冒険譚,かくうぼうけんたん\",\"例示姓,れいじせい\",\"例示名,れいじめい\"]");
        List<UserDicEntry> expected = Arrays.asList(
                new UserDicEntry("架空ランドマーク"),
                new UserDicEntry("架空冒険譚", "かくうぼうけんたん"),
                new UserDicEntry("例示姓", "れいじせい"),
                new UserDicEntry("例示名", "れいじめい")
        );

        // execute
        List<UserDicEntry> actual = UserDicEntryParser.parse(entries);

        // verify
        assertEquals(expected, actual);
    }

    @Test
    void testParseIgnoresEntriesWithEmptyTokens() {
        // setup
        JsonNode entries = new ObjectMapper().createArrayNode()
                .add("")
                .add(",")
                .add(",,")
                .add(",読み")
                .add("\"\",読み")
                .add("表層");
        List<UserDicEntry> expected = List.of(new UserDicEntry("表層"));

        // execute
        List<UserDicEntry> actual = UserDicEntryParser.parse(entries);

        // verify
        assertEquals(expected, actual);
    }

    @Test
    void testParseTreatsEmptyReadingsAsMissing() {
        // setup
        JsonNode entries = new ObjectMapper().createArrayNode()
                .add("表層1,")
                .add("表層2,\"\"");
        List<UserDicEntry> expected = Arrays.asList(
                new UserDicEntry("表層1"),
                new UserDicEntry("表層2")
        );

        // execute
        List<UserDicEntry> actual = UserDicEntryParser.parse(entries);

        // verify
        assertEquals(expected, actual);
    }

    @Test
    void testParseIgnoresPropertiesAfterReading() {
        // setup
        JsonNode entries = new ObjectMapper().createArrayNode()
                .add("表層1,読み1,余剰")
                .add("表層2,読み2,余剰,余剰");
        List<UserDicEntry> expected = Arrays.asList(
                new UserDicEntry("表層1", "読み1"),
                new UserDicEntry("表層2", "読み2")
        );

        // execute
        List<UserDicEntry> actual = UserDicEntryParser.parse(entries);

        // verify
        assertEquals(expected, actual);
    }

    @Test
    void testParseQuotedCsvFields() {
        // setup
        JsonNode entries = new ObjectMapper().createArrayNode()
                .add("\"表層,表記\",読み,余剰");
        List<UserDicEntry> expected = List.of(
                new UserDicEntry("表層,表記", "読み")
        );

        // execute
        List<UserDicEntry> actual = UserDicEntryParser.parse(entries);

        // verify
        assertEquals(expected, actual);
    }
}
