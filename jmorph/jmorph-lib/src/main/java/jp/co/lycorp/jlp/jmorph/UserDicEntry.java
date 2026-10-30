// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

import java.util.Objects;

public class UserDicEntry {
    private String token; //表記
    private String reading; //読み

    public UserDicEntry(String token, String reading) {
        this.token = token;
        this.reading = reading;
    }

    public UserDicEntry(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public String getReading() {
        return reading;
    }

    public String toString() {
        String s = "{" + token;
        if (reading != null) s += "," + reading;
        s += "}";
        return s;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserDicEntry that = (UserDicEntry) o;
        return Objects.equals(token, that.token) && Objects.equals(reading, that.reading);
    }

    @Override
    public int hashCode() {
        return Objects.hash(token, reading);
    }
}
