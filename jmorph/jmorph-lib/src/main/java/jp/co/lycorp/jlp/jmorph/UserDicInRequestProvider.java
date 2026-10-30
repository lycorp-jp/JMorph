// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

import java.util.List;

public interface UserDicInRequestProvider {
    MorphDic create(List<? extends UserDicEntry> userDicEntries);

    MorphDic create(List<? extends UserDicEntry> userDicEntries, TimeoutGuard timeout);
}
