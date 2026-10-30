// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

public class NodeImpl implements Node {

    public Token token;
    public int dictId;
    public int loc;
    public int length;
    public int pathCost;
    public NodeImpl leftLink;
    public NodeImpl next;

    public NodeImpl(int loc, int length) { //bos,eos
        this(new TokenBoundary(), -1, loc, length);
    }

    public NodeImpl(Token token, int dictId, int loc, int length) {
        this.token = token;
        this.dictId = dictId;
        this.loc = loc;
        this.length = length;
        this.pathCost = 0;
        this.leftLink = null;
        this.next = null;
    }

    @Override
    public Token getToken() {
        return token;
    }

    @Override
    public int getDictId() {
        return dictId;
    }

    @Override
    public int getLoc() {
        return loc;
    }

    @Override
    public int getLength() {
        return length;
    }

    @Override
    public int getPathCost() {
        return pathCost;
    }

    public NodeImpl getLeftLink() {
        return leftLink;
    }

    public NodeImpl getNext() {
        return next;
    }

    @Override
    public String toString() {
        return "NodeImpl{" +
                "token=" + token +
                ", dictId=" + dictId +
                ", loc=" + loc +
                ", length=" + length +
                ", pathCost=" + pathCost +
                ", leftLink=" + leftLink +
                ", next=" + next +
                '}';
    }
}
