// SPDX-License-Identifier: BSD-3-Clause

package jp.co.lycorp.jlp.jmorph;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * Java implementation of MeCab Darts (Double-ARray Trie System).
 * This code is based on MeCab's darts.h, which is licensed under the BSD 3-Clause license.
 * https://github.com/taku910/mecab/blob/master/mecab/src/darts.h
 * Copyright(C) 2001-2007 Taku Kudo &lt;taku@chasen.org&gt;
 */
public class DartsMecab {
    static class Unit {
        int base;
        int check;
        public static final int BYTE_SIZE = 8;

        public Unit(int base, int check) {
            this.base = base;
            this.check = check;
        }
    }

    public static class Result {
        public int value;
        public int length;

        public Result(int value, int length) {
            this.value = value;
            this.length = length;
        }

        @Override
        public String toString() {
            return "Result{value=" + value + ", length=" + length + "}";
        }
    }

    private Unit[] array;
    private int size;

    public DartsMecab() {
        array = null;
        size = 0;
    }

    public void setArray(byte[] dartsArray) {
        this.size = dartsArray.length / Unit.BYTE_SIZE;
        this.array = new Unit[size];
        ByteBuffer dartsBuffer = ByteBuffer.wrap(dartsArray).order(ByteOrder.LITTLE_ENDIAN);

        for (int i = 0; i < size; i++) {
            int base = dartsBuffer.getInt();
            int check = dartsBuffer.getInt();
            this.array[i] = new Unit(base, check);
        }
    }

    public List<Result> commonPrefixSearch(byte[] key, int maxResult) {
        return commonPrefixSearch(key, 0, maxResult);
    }

    public List<Result> commonPrefixSearch(byte[] key, int offset, int maxResult) {
        if (array == null) {
            throw new IllegalStateException("Darts array has not been initialized");
        }
        if (offset < 0 || offset > key.length) {
            throw new IndexOutOfBoundsException("Offset " + offset + " is out of bounds for key length " + key.length);
        }

        List<Result> resultList = new ArrayList<>();
        int b = array[0].base;
        int num = 0;
        int n;
        int p;
        int len = key.length - offset;

        for (int i = 0; i < len; i++) {
            p = b;
            n = array[p].base;

            if (b == array[p].check && n < 0) {
                if (num < maxResult) {
                    resultList.add(new Result(-n - 1, i));
                    num++;
                }
            }

            int charCode = key[offset + i] & 0xFF;
            p = b + charCode + 1;

            if (b == array[p].check) {
                b = array[p].base;
            } else {
                return resultList;
            }
        }

        p = b;
        n = array[p].base;
        if (b == array[p].check && n < 0) {
            if (num < maxResult) {
                resultList.add(new Result(-n - 1, len));
            }
        }
        return resultList;
    }

    public int getSize() {
        return size;
    }
}
