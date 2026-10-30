// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

public interface Token {
    short getLc();

    short getRc();

    short getCost();

    int getFeatureSetId();

    int getTokenId();

    byte[] toBytes();
}
