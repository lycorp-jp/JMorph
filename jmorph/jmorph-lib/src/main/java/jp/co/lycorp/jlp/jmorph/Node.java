// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

public interface Node {
    Token getToken();

    int getDictId();

    int getLoc();

    int getLength();

    int getPathCost();
}
