// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.logging.Logger;

public class CostMatrixMecab implements CostMatrix {
    private static final Logger LOGGER = Logger.getLogger(CostMatrixMecab.class.getName());

    private final ShortBuffer matrix;
    private final int lsize; // 前件サイズ
    private final int rsize; // 後件サイズ

    public final static short userDicEntriesIndex = Short.MAX_VALUE;
    private final static int userDicEntriesCost = 10;

    public CostMatrixMecab() {
        matrix = ShortBuffer.allocate(1);
        lsize = 0;
        rsize = 0;
    }

    public CostMatrixMecab(String filePath) throws IOException {
        ByteBuffer buffer = FileUtils.openBuffer(filePath).order(ByteOrder.LITTLE_ENDIAN);
        int headerSize = Short.BYTES + Short.BYTES;
        if (buffer.limit() < headerSize) {
            throw new IOException("Invalid header size: " + filePath);
        }
        lsize = Short.toUnsignedInt(buffer.getShort(0));
        rsize = Short.toUnsignedInt(buffer.getShort(Short.BYTES));

        int pos = headerSize;
        int size = buffer.limit() - pos;
        if ((long) lsize * rsize * Short.BYTES != size) {
            throw new IOException("Invalid matrix size: " + filePath);
        }
        ByteBuffer matrixBuffer = buffer.duplicate().order(ByteOrder.LITTLE_ENDIAN);
        matrixBuffer.position(pos);
        matrixBuffer.limit(buffer.limit());
        matrix = matrixBuffer.slice().order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
    }

    public int getUserDicEntriesIndex() {
        return userDicEntriesIndex;
    }

    public int getLsize() {
        return lsize;
    }

    public int getRsize() {
        return rsize;
    }

    @Override
    public boolean isValidRc(int rc) {
        return rc == userDicEntriesIndex || (rc >= 0 && rc < lsize);
    }

    @Override
    public boolean isValidLc(int lc) {
        return lc == userDicEntriesIndex || (lc >= 0 && lc < rsize);
    }

    public int getCost(int rc, int lc) {
        if (rc == userDicEntriesIndex || lc == userDicEntriesIndex) {
            return userDicEntriesCost;
        }
        return matrix.get(rc + lsize * lc);
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            LOGGER.severe("args.length != 1");
            System.exit(1);
        }
        String filePath = args[0];
        CostMatrixMecab cm = new CostMatrixMecab(filePath);

        System.out.println(cm.getLsize() + " " + cm.getRsize());
        for (int rc = 0; rc < cm.getLsize(); rc++) {
            for (int lc = 0; lc < cm.getRsize(); lc++) {
                System.out.println(rc + " " + lc + " " + cm.getCost(rc, lc));
            }
        }
    }
}
