// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

import java.io.IOException;
import java.util.List;

public interface Dictionary {

    CostMatrix getCostMatrix();

    List<NodeImpl> lookup(byte[] sentence, int pos);

    List<NodeImpl> lookup(byte[] sentence, int pos, TimeoutGuard timeout);

    byte[][] getEos(NodeImpl eos);

    List<byte[][]> getAllFeatures(byte[] sentence, NodeImpl node);

    void addUserDicInRequest(List<? extends UserDicEntry> userDicEntries) throws IOException;

    void addUserDicInRequest(
            List<? extends UserDicEntry> userDicEntries, TimeoutGuard timeout) throws IOException;

    void removeUserDicInRequest();

    List<NodeImpl> concat(byte[] sentence, List<NodeImpl> nodeList);
}
