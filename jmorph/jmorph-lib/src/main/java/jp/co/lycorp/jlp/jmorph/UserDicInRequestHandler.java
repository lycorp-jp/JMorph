// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

import java.io.IOException;
import java.util.List;

public interface UserDicInRequestHandler {
    public void addUserDicInRequest(List<? extends UserDicEntry> userDicEntries) throws IOException;

    public void removeUserDicInRequest();
}
