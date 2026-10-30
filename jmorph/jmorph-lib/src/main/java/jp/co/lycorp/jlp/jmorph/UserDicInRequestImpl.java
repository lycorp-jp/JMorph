// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

public class UserDicInRequestImpl implements MorphDic {

    private final List<? extends UserDicEntry> userDicEntries;
    private final int userDicEntriesIndex;
    private final MorphDic baseDic;
    private final UnknownDic unknownDic;
    private final boolean outputFeatures;
    private final byte dumpDelim;
    private final String userDicTemplate;
    private final String fallbackUnknownOtherTemplate;
    private final short cost = (short) 0;
    private static final short featureSetId = (short) -1;

    public UserDicInRequestImpl(List<? extends UserDicEntry> userDicEntries, int userDicEntriesIndex,
            MorphDic baseDic, UnknownDic unknownDic, boolean outputFeatures, byte dumpDelim,
            String userDicTemplate, String fallbackUnknownOtherTemplate) {
        this.userDicEntries = userDicEntries;
        this.userDicEntriesIndex = userDicEntriesIndex;
        this.baseDic = baseDic;
        this.unknownDic = unknownDic;
        this.outputFeatures = outputFeatures;
        this.dumpDelim = dumpDelim;
        this.userDicTemplate = userDicTemplate;
        this.fallbackUnknownOtherTemplate = fallbackUnknownOtherTemplate;
    }

    public List<NodeImpl> lookup(byte[] sentence, int pos, int dictId) {
        return lookup(sentence, pos, dictId, TimeoutGuard.disabled());
    }

    public List<NodeImpl> lookup(byte[] sentence, int pos, int dictId, TimeoutGuard timeout) {
        List<NodeImpl> nodeList = new ArrayList<>();
        for (int i = 0; i < userDicEntries.size(); i++) {
            timeout.checkPeriodically();
            if (matchDicEntry(userDicEntries.get(i), sentence, pos)) {
                Token token = new TokenMecab((short) userDicEntriesIndex, (short) userDicEntriesIndex,
                        cost, featureSetId, i);
                nodeList.add(
                        new NodeImpl(token, dictId, pos, ByteUtils.toBytes(userDicEntries.get(i).getToken()).length));
            }
        }
        return nodeList;
    }

    public NodeImpl lookup(byte[] sentence, int pos) {
        return null;
    }

    private boolean matchDicEntry(UserDicEntry userDicEntry, byte[] sentence, int pos) {
        int entrySize = ByteUtils.toBytes(userDicEntry.getToken()).length;

        // 位置posからユーザ辞書のEntryの文字列のサイズを加えた値が入力文の長さを超える場合
        if (sentence.length < pos + entrySize) {
            return false;
        }

        // 位置posからユーザ辞書のEntryの文字列と同じサイズだけ抜き出した文字列がEntryの文字列と一致するか
        return ByteUtils.toString(sentence, pos, entrySize).equals(userDicEntry.getToken());
    }

    public byte[][] getFeatures(NodeImpl node) {
        UserDicEntry entry = userDicEntries.get(node.token.getTokenId());
        return getUserDicFeaturesByConfig(entry.getToken(), entry.getReading());
    }

    public List<NodeImpl> getPhraseNode(NodeImpl pnode) {
        return null;
    }

    public boolean isPhrase(NodeImpl node) {
        return false;
    }

    public List<NodeImpl> getPhrase(NodeImpl node) {
        return null;
    }

    private byte[][] expandFeaturesByMorphDic(String val, byte[][] byteFeatures, String surface, String reading) {
        byte[] valBytes = ByteUtils.toBytes(val);
        List<byte[]> list = new ArrayList<>();
        ByteArrayOutputStream os = new ByteArrayOutputStream(128);
        String readingOrSurface = (reading != null) ? reading : surface;
        int index = 0;
        for (byte b : valBytes) {
            if (b == dumpDelim) {
                String s = os.toString(StandardCharsets.UTF_8);
                boolean changed = false;
                if (s.contains("%s")) {
                    s = s.replaceAll("%s", Matcher.quoteReplacement(surface));
                    changed = true;
                }
                if (s.contains("%y")) {
                    s = s.replaceAll("%y", Matcher.quoteReplacement(readingOrSurface));
                    changed = true;
                }
                if (changed) {
                    list.add(ByteUtils.toBytes(s));
                } else {
                    if (index >= byteFeatures.length) {
                        throw new ArrayIndexOutOfBoundsException("Number of columns does not match");
                    }
                    list.add(byteFeatures[index]);
                }
                os.reset();
                index++;
            } else {
                os.write(b);
            }
        }
        if (os.size() > 0) {
            String s = os.toString(StandardCharsets.UTF_8);
            boolean changed = false;
            if (s.contains("%s")) {
                s = s.replaceAll("%s", Matcher.quoteReplacement(surface));
                changed = true;
            }
            if (s.contains("%y")) {
                s = s.replaceAll("%y", Matcher.quoteReplacement(readingOrSurface));
                changed = true;
            }
            if (changed) {
                list.add(ByteUtils.toBytes(s));
            } else {
                if (index >= byteFeatures.length) {
                    throw new ArrayIndexOutOfBoundsException("Number of columns does not match");
                }
                list.add(byteFeatures[index]);
            }
        }

        return list.toArray(new byte[list.size()][]);
    }

    private byte[][] getUserDicFeaturesByConfig(String surface, String reading) {
        if (!outputFeatures) {
            throw new IllegalStateException("Features are disabled.");
        }
        if (userDicTemplate == null) {
            // dump.userdicが存在しない場合は未知語扱いとする
            return unknownDic.getFeaturesByConfig(surface, null);
        }
        return expandFeatures(userDicTemplate, surface, reading);
    }

    private byte[][] expandFeatures(String val, String surface, String reading) {
        byte[] valBytes = ByteUtils.toBytes(val);

        List<byte[]> list = new ArrayList<>();
        ByteArrayOutputStream os = new ByteArrayOutputStream(128);
        String readingOrSurface = (reading != null) ? reading : surface;

        for (byte b : valBytes) {
            if (b == dumpDelim) {
                String s = os.toString(StandardCharsets.UTF_8);
                if (s.contains("%s")) {
                    s = s.replaceAll("%s", Matcher.quoteReplacement(surface));
                }
                if (s.contains("%y")) {
                    s = s.replaceAll("%y", Matcher.quoteReplacement(readingOrSurface));
                }
                list.add(ByteUtils.toBytes(s));
                os.reset();
            } else {
                os.write(b);
            }
        }
        if (os.size() > 0) {
            String s = os.toString(StandardCharsets.UTF_8);
            if (s.contains("%s")) {
                s = s.replaceAll("%s", Matcher.quoteReplacement(surface));
            }
            if (s.contains("%y")) {
                s = s.replaceAll("%y", Matcher.quoteReplacement(readingOrSurface));
            }
            list.add(ByteUtils.toBytes(s));
        }

        return list.toArray(new byte[list.size()][]);
    }
}
