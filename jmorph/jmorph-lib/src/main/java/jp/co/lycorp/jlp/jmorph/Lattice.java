// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class Lattice {
    private final List<NodeImpl> nodeTable;
    private final CostMatrix costMatrix;
    private final byte[] sentence;
    private final NodeImpl bos;
    private NodeImpl eos = null;
    private int nbestCount = 0;
    private NbestQueue nbestQueue = null;

    public Lattice(CostMatrix costMatrix, byte[] sentence) {
        nodeTable = new ArrayList<>(Collections.nCopies(sentence.length + 2, null));
        this.costMatrix = costMatrix;
        this.sentence = sentence;
        bos = new NodeImpl(-1, 1);
        push(bos);
    }

    public NodeImpl get(int loc) {
        return nodeTable.get(loc);
    }

    public void connect(List<NodeImpl> rnodeList) {
        connect(rnodeList, TimeoutGuard.disabled());
    }

    void connect(List<NodeImpl> rnodeList, TimeoutGuard timeout) {
        for (NodeImpl rnode : rnodeList) {
            timeout.checkPeriodically();
            int bestCost = Integer.MAX_VALUE;
            NodeImpl bestNode = null;
            NodeImpl lnode = get(rnode.loc);
            do {
                timeout.checkPeriodically();
                int cost = lnode.getPathCost() + costMatrix.getCost(lnode.token.getRc(), rnode.token.getLc());
                if (cost < bestCost) {
                    bestCost = cost;
                    bestNode = lnode;
                }
            } while ((lnode = lnode.getNext()) != null);
            rnode.pathCost = bestCost + rnode.token.getCost();
            rnode.leftLink = bestNode;
            push(rnode);
        }
    }

    public void finish() {
        eos = new NodeImpl(sentence.length, 1);
        connect(Collections.singletonList(eos));
    }

    void finish(TimeoutGuard timeout) {
        eos = new NodeImpl(sentence.length, 1);
        connect(Collections.singletonList(eos), timeout);
    }

    public byte[] getSentence() {
        return sentence;
    }

    public NodeImpl getEos() {
        return eos;
    }

    public List<NodeImpl> getBestPath(int nbestNum) {
        return getBestPath(nbestNum, TimeoutGuard.disabled());
    }

    List<NodeImpl> getBestPath(int nbestNum, TimeoutGuard timeout) {
        nbestCount = nbestNum;
        if (nbestCount < 1) {
            return Collections.emptyList();
        }
        if (nbestCount == 1) {
            // traceback
            LinkedList<NodeImpl> nodeList = new LinkedList<>();
            for (NodeImpl node = eos.leftLink; node != bos; node = node.leftLink) {
                timeout.checkPeriodically();
                nodeList.push(node);
            }
            --nbestCount;
            return nodeList;
        }
        // N-Best
        nbestQueue = new NbestQueue();
        nbestQueue.add(new Anode(null, eos, 0), timeout);
        return getNextPath(timeout);
    }

    public List<NodeImpl> getNextPath() {
        return getNextPath(TimeoutGuard.disabled());
    }

    List<NodeImpl> getNextPath(TimeoutGuard timeout) {
        if (nbestCount < 1) {
            return Collections.emptyList();
        }
        while (!nbestQueue.isEmpty()) {
            timeout.checkPeriodically();
            Anode anode = nbestQueue.poll();
            if (anode.mnode == bos) {
                List<NodeImpl> nodeList = new LinkedList<>();
                for (anode = anode.parent; anode.parent != null; anode = anode.parent) {
                    timeout.checkPeriodically();
                    nodeList.add(anode.mnode);
                }
                --nbestCount;
                return nodeList;
            }
            NodeImpl lnode = get(anode.mnode.loc);
            do {
                timeout.checkPeriodically();
                int costG = costMatrix.getCost(lnode.token.getRc(), anode.mnode.token.getLc())
                        + anode.mnode.token.getCost()
                        + anode.costG;
                nbestQueue.add(new Anode(anode, lnode, costG), timeout);
            } while ((lnode = lnode.next) != null);
        }
        return Collections.emptyList();
    }

    private void push(NodeImpl node) {
        int loc = node.loc + node.length;
        node.next = nodeTable.get(loc);
        nodeTable.set(loc, node);
    }
}
