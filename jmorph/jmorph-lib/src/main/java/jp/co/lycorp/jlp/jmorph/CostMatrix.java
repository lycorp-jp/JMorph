// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

public interface CostMatrix {

    int getUserDicEntriesIndex();

    boolean isValidRc(int rc);

    boolean isValidLc(int lc);

    int getCost(int rc, int lc);
}
