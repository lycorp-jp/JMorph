// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

import java.util.List;

public interface Tokenizer {
    List<TokenInfo> analyze(byte[] sentence);

    List<TokenInfo> analyze(String sentence);

    List<TokenInfo> next();
}
