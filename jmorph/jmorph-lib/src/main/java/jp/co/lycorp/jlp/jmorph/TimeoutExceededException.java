// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

public final class TimeoutExceededException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public TimeoutExceededException(String message) {
        super(message);
    }
}
