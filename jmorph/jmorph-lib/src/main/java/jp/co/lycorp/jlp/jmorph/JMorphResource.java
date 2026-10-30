// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

import java.util.List;
import java.util.Objects;

/**
 * Manages read-only resources used by {@link JMorphAnalyzer}.
 * Holds only resources that may be shared across analyzer instances.
 * Excludes any state that varies per instance or per request.
 */
public final class JMorphResource {
    private final List<MorphDic> morphDicList;
    private final CostMatrix costMatrix;
    private final UnknownDic unknownDic;
    private final UserDicInRequestProvider userDicInRequestProvider;

    public JMorphResource(List<MorphDic> morphDicList, CostMatrix costMatrix, UnknownDic unknownDic,
            UserDicInRequestProvider userDicInRequestProvider) {
        this.morphDicList = List.copyOf(Objects.requireNonNull(morphDicList));
        this.costMatrix = Objects.requireNonNull(costMatrix);
        this.unknownDic = Objects.requireNonNull(unknownDic);
        this.userDicInRequestProvider = Objects.requireNonNull(userDicInRequestProvider);
    }

    public List<MorphDic> getMorphDicList() {
        return morphDicList;
    }

    public CostMatrix getCostMatrix() {
        return costMatrix;
    }

    public UnknownDic getUnknownDic() {
        return unknownDic;
    }

    public UserDicInRequestProvider getUserDicInRequestProvider() {
        return userDicInRequestProvider;
    }

    @Override
    protected final Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }
}
