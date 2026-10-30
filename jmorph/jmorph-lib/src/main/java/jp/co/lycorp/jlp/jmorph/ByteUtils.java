// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class ByteUtils {

    private ByteUtils() {
    }

    private static final byte[] UTF8_CHSIZE_DATA = {
            1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
            2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
            2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
            3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,
            4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 6, 6, 1, 1
    };

    public static int chsize(byte b) {
        return UTF8_CHSIZE_DATA[Byte.toUnsignedInt(b)];
    }

    public static byte[] quote(byte[] text) {
        for (int i = 0; i < text.length; i += chsize(text[i])) {
            if (text[i] == ',' || text[i] == '"' || text[i] == '\r' || text[i] == '\n') {
                ByteArrayOutputStream os = new ByteArrayOutputStream(128);
                os.write('"');
                for (i = 0; i < text.length; ++i) {
                    os.write(text[i]);
                    if (text[i] == '"') {
                        os.write('"');
                    }
                }
                os.write('"');
                return os.toByteArray();
            }
        }
        return text;
    }

    public static byte[] join(byte[] delim, List<byte[]> elem) {
        if (!elem.isEmpty()) {
            ByteArrayOutputStream os = new ByteArrayOutputStream(128);
            byte[] e = elem.get(0);
            os.write(e, 0, e.length);
            for (int i = 1; i < elem.size(); ++i) {
                os.write(delim, 0, delim.length);
                e = elem.get(i);
                os.write(e, 0, e.length);
            }
            return os.toByteArray();
        }
        return new byte[0];
    }

    public static byte[] valueOf(int i) {
        return String.valueOf(i).getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] toBytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    public static String toString(byte[] b) {
        return new String(b, StandardCharsets.UTF_8);
    }

    public static String toString(byte[] b, int o, int l) {
        return new String(b, o, l, StandardCharsets.UTF_8);
    }

    public static int bytencmp(byte[] b1, int p1, ByteBuffer b2, int p2, int n) {
        if (p1 + n > b1.length) {
            return 1;
        }
        if (p2 + n > b2.limit()) {
            return 1;
        }
        for (n += p1; p1 < n; ++p1, ++p2) {
            if (b1[p1] != b2.get(p2)) {
                return 1;
            }
        }
        return 0; //match
    }

    public static int bytencmp(ByteBuffer b1, int p1, ByteBuffer b2, int p2, int n) {
        if (p1 + n > b1.limit()) {
            return 1;
        }
        if (p2 + n > b2.limit()) {
            return 1;
        }
        for (n += p1; p1 < n; ++p1, ++p2) {
            if (b1.get(p1) != b2.get(p2)) {
                return 1;
            }
        }
        return 0; //match
    }

    /**
     * 整数値をリトルエンディアンのバイト配列に変換する
     */
    public static byte[] intToBytesLE(int value) {
        return new byte[]{
                (byte) value,
                (byte) (value >>> 8),
                (byte) (value >>> 16),
                (byte) (value >>> 24)
        };
    }
}
