// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

final class UserDicEntryParser {
    private UserDicEntryParser() {
    }

    static List<UserDicEntry> parse(JsonNode entries) {
        List<UserDicEntry> entryList = new ArrayList<>();
        if (entries == null) {
            return entryList;
        }
        for (JsonNode entry : entries) {
            UserDicEntry userDicEntry = parse(entry.asText());
            if (userDicEntry != null) {
                entryList.add(userDicEntry);
            }
        }
        return entryList;
    }

    static UserDicEntry parse(String entryString) {
        List<String> properties = CsvParser.parse(entryString);
        if (properties.isEmpty() || properties.get(0).isEmpty()) {
            return null;
        }

        String token = properties.get(0);
        if (properties.size() >= 2 && !properties.get(1).isEmpty()) {
            return new UserDicEntry(token, properties.get(1));
        }
        return new UserDicEntry(token);
    }
}
