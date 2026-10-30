// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

class Anode {
    public Anode parent;
    public NodeImpl mnode;
    public int costG;

    public Anode(Anode parent, NodeImpl mnode, int costG) {
        this.parent = parent;
        this.mnode = mnode;
        this.costG = costG;
    }
}
