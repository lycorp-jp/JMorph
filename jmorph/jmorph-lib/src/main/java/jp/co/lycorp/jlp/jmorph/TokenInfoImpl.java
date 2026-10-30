// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

public class TokenInfoImpl implements TokenInfo {
    private final byte[][] result;

    public TokenInfoImpl(byte[][] result) {
        this.result = result;
    }

    public String[] get() {
        String[] resultStrings = new String[result.length];
        for (int i = 0; i < result.length; i++) {
            resultStrings[i] = ByteUtils.toString(result[i]);
        }
        return resultStrings;
    }

    public byte[][] getBytes() {
        return result;
    }
}
