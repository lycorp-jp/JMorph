// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserDicInRequestProviderBasic implements UserDicInRequestProvider {
    private final int userDicEntriesIndex;
    private final MorphDic baseDic;
    private final UnknownDic unknownDic;
    private final boolean outputFeatures;
    private final byte dumpDelim;
    private final String userDicTemplate;
    private final String fallbackUnknownOtherTemplate;

    public UserDicInRequestProviderBasic(String dumpUserDicLocation, String dumpUnknownLocation,
            boolean outputFeatures, String delim,
            int userDicEntriesIndex, MorphDic baseDic, UnknownDic unknownDic) throws IOException {
        if (ByteUtils.toBytes(delim).length != 1) {
            throw new RuntimeException("'delim' is not a single-byte character");
        }
        this.userDicEntriesIndex = userDicEntriesIndex;
        this.baseDic = baseDic;
        this.unknownDic = unknownDic;
        this.outputFeatures = outputFeatures;
        this.dumpDelim = ByteUtils.toBytes(delim)[0];
        this.userDicTemplate = outputFeatures ? loadUserDicTemplate(dumpUserDicLocation) : null;
        this.fallbackUnknownOtherTemplate = outputFeatures
                ? loadFallbackUnknownOtherTemplate(dumpUnknownLocation)
                : null;
    }

    public MorphDic create(List<? extends UserDicEntry> userDicEntries) {
        return create(userDicEntries, TimeoutGuard.disabled());
    }

    public MorphDic create(List<? extends UserDicEntry> userDicEntries, TimeoutGuard timeout) {
        timeout.check();
        MorphDic dictionary = new UserDicInRequestImpl(userDicEntries, userDicEntriesIndex, baseDic, unknownDic,
                outputFeatures, dumpDelim, userDicTemplate, fallbackUnknownOtherTemplate);
        timeout.check();
        return dictionary;
    }

    private String loadUserDicTemplate(String dumpUserDicLocation) throws IOException {
        if (!FileUtils.exists(dumpUserDicLocation)) {
            return null;
        }

        Map<String, String> userDicDump = loadDump(dumpUserDicLocation);
        String val = userDicDump.get("u");
        if (val == null) {
            throw new IOException("Cannot get 'u' value: " + dumpUserDicLocation);
        }
        return val;
    }

    private String loadFallbackUnknownOtherTemplate(String dumpUnknownLocation) throws IOException {
        Map<String, String> unknownDump = loadDump(dumpUnknownLocation);
        String val = unknownDump.get("o");
        if (val == null) {
            throw new IOException("Cannot get 'o' value: " + dumpUnknownLocation);
        }
        return val;
    }

    private Map<String, String> loadDump(String fileName) throws IOException {
        Map<String, String> dump = new HashMap<>();
        for (String line : FileUtils.readAllLines(fileName)) {
            String[] data = line.split(",");
            dump.put(data[0], data[1]);
        }
        return dump;
    }
}
