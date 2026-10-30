// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

import java.util.List;

public interface MorphDic {
    List<NodeImpl> lookup(byte[] sentence, int pos, int dicId);

    List<NodeImpl> lookup(byte[] sentence, int pos, int dicId, TimeoutGuard timeout);

    byte[][] getFeatures(NodeImpl node);

    boolean isPhrase(NodeImpl node);

    List<NodeImpl> getPhrase(NodeImpl node);
}
