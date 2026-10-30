// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class TokenMecab implements Token {

    public short lc;
    public short rc;
    public short cost;
    public int featureSetId;
    public int tokenId;
    public static final int NUM_BYTES = 10;

    public TokenMecab() {
        this((short) 0, (short) 0, (short) 10000, -1);
    }

    public TokenMecab(short lc, short rc, short cost,
                      int featureSetId) {
        this.lc = lc;
        this.rc = rc;
        this.cost = cost;
        this.featureSetId = featureSetId;
        this.tokenId = -1;
    }

    // ユーザ辞書用
    public TokenMecab(short lc, short rc, short cost,
                      int featureSetId, int tokenId) {
        this.lc = lc;
        this.rc = rc;
        this.cost = cost;
        this.featureSetId = featureSetId;
        this.tokenId = tokenId;
    }

    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(NUM_BYTES);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putShort(lc);
        buffer.putShort(rc);
        buffer.putShort(cost);
        buffer.putInt(featureSetId);
        return buffer.array();
    }


    @Override
    public short getLc() {
        return lc;
    }

    @Override
    public short getRc() {
        return rc;
    }

    @Override
    public short getCost() {
        return cost;
    }

    @Override
    public int getFeatureSetId() {
        return featureSetId;
    }

    @Override
    public int getTokenId() {
        return this.tokenId;
    }

    @Override
    public String toString() {
        return "TokenMecab{" +
                "lc=" + lc +
                ", rc=" + rc +
                ", cost=" + cost +
                ", featureSetId=" + featureSetId +
                ", tokenId=" + tokenId +
                '}';
    }
}
