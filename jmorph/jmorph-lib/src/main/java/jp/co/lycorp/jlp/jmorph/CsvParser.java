// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

import java.util.ArrayList;
import java.util.List;

public class CsvParser {

    public static List<String> parse(String input) {
        List<String> fields = new ArrayList<>();
        int index = 0;

        while (index < input.length()) {
            if (input.charAt(index) != '"') {
                StringBuilder field = new StringBuilder();
                while (index < input.length() && input.charAt(index) != ',') {
                    field.append(input.charAt(index++));
                }
                fields.add(field.toString());
                // Skip a comma for the next value.
                if (index < input.length() && input.charAt(index) == ',') {
                    index++;
                }
            } else {
                // Handle quoted fields
                index++;
                StringBuilder field = new StringBuilder();
                while (index < input.length()) {
                    if (input.charAt(index) == '"' && (index + 1 < input.length()
                            && input.charAt(index + 1) == '"')) {
                        field.append('"');
                        index += 2;
                    } else if (input.charAt(index) == '"') {
                        index++;
                        while (index < input.length() && input.charAt(index) == ' ') {
                            index++;
                        }
                        if (index < input.length() && input.charAt(index) == ',') {
                            index++;
                        }
                        break;
                    } else {
                        field.append(input.charAt(index++));
                    }
                }
                fields.add(field.toString());
            }
        }
        return fields;
    }
}
