// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

class TokenBoundary implements Token {

    public short lc;
    public short rc;
    public short cost;
    public short featureSetId;
    public int tokenId;
    public int extension;
    public static final int NUM_BYTES = 16;

    public TokenBoundary() {
        this.lc = 0;
        this.rc = 0;
        this.cost = 0;
        this.featureSetId = -1;
        this.tokenId = -1;
        this.extension = -1;    }

    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(NUM_BYTES);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putShort(lc);
        buffer.putShort(rc);
        buffer.putShort(cost);
        buffer.putShort(featureSetId);
        buffer.putInt(tokenId);
        buffer.putInt(extension);
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
        return tokenId;
    }

    public int getExtension() {
        return extension;
    }

    @Override
    public String toString() {
        return "TokenBoundary{" +
                "lc=" + lc +
                ", rc=" + rc +
                ", cost=" + cost +
                ", featureSetId=" + featureSetId +
                ", tokenId=" + tokenId +
                ", extension=" + extension +
                '}';
    }
}
