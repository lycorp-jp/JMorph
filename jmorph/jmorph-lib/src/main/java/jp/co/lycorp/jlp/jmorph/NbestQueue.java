// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

import java.util.LinkedList;
import java.util.ListIterator;

class NbestQueue {
    private final LinkedList<Anode> list = new LinkedList<>();

    public boolean isEmpty() {
        return list.isEmpty();
    }

    public Anode poll() {
        return list.poll();
    }

    public void add(Anode anode) {
        add(anode, TimeoutGuard.disabled());
    }

    void add(Anode anode, TimeoutGuard timeout) {
        ListIterator<Anode> iterator = list.listIterator();
        while (iterator.hasNext()) {
            timeout.checkPeriodically();
            Anode tnode = iterator.next();
            if (anode.mnode.pathCost + anode.costG < tnode.mnode.pathCost + tnode.costG) {
                iterator.previous();
                iterator.add(anode);
                return;
            }
        }
        list.add(anode);
    }
}
