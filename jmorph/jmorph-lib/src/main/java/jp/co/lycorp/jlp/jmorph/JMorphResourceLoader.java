// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

import java.io.IOException;

public interface JMorphResourceLoader {
    JMorphResource load(JMorphConfig config) throws IOException;
}
