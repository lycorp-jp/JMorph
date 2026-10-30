// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

import java.util.List;

public interface UnknownDic {
    NodeImpl unkNode(byte[] sentence, int pos);

    List<NodeImpl> concat(byte[] sentence, List<NodeImpl> nodeList);

    byte[][] getFeaturesByConfig(String surface, String reading);
}
