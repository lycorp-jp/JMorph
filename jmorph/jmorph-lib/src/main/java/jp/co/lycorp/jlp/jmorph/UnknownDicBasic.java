// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

public class UnknownDicBasic implements UnknownDic {
    private final TokenMecab unkToken;
    private final Set<String> concatSet = new HashSet<>();
    private Map<String, String> unknownDump = null;
    private final byte dumpDelim; // dump文字列の区切り文字
    private final boolean outputFeatures;

    public UnknownDicBasic(String unkCsvLocation, String dumpUnknownLocation, boolean outputFeatures,
            UnknownDicBasicConfig config) throws IOException {
        this.outputFeatures = outputFeatures;

        Boolean concat = false;
        Boolean ncck = false;
        if (config.getConcatMode() == ConcatMode.ALL) {
            concat = true;
            ncck = false;
        } else if (config.getConcatMode() == ConcatMode.ALL_EXCEPT_KATAKANA) {
            concat = true;
            ncck = true;
        }

        if (ByteUtils.toBytes(config.getDelim()).length != 1) {
            throw new RuntimeException("'delim' is not a single-byte character");
        }
        dumpDelim = ByteUtils.toBytes(config.getDelim())[0];

        unkToken = new TokenMecab();
        for (String line : FileUtils.readAllLines(unkCsvLocation)) {
            String[] data = line.split(",");
            if (data[0].equals("UNK") && Integer.parseInt(data[4]) == 1) {
                unkToken.lc = Short.parseShort(data[1]);
                unkToken.rc = Short.parseShort(data[2]);
                unkToken.cost = Short.parseShort(data[3]);
                unkToken.featureSetId = 1; // UNK
            }
        }
        if (concat) {
            concatSet.add("a");
            concatSet.add("n");
            concatSet.add("N");
            if (!ncck) {
                concatSet.add("k");
            }
        }
        if (outputFeatures) {
            unknownDump = new HashMap<>();
            for (String line : FileUtils.readAllLines(dumpUnknownLocation)) {
                String[] data = line.split(",");
                unknownDump.put(data[0], data[1]);
            }
            String val = unknownDump.get("o");
            if (val == null) {
                throw new IOException("Cannot get 'o' value: " + dumpUnknownLocation);
            }
        }
    }

    // 未知語ノードの生成
    public NodeImpl unkNode(byte[] sentence, int pos) {
        int length = Math.min(ByteUtils.chsize(sentence[pos]), sentence.length - pos);
        return unkNode(pos, length);
    }

    protected NodeImpl unkNode(int pos, int length) {
        return new NodeImpl(unkToken, -1, pos, length);
    }

    // 連結処理
    public List<NodeImpl> concat(byte[] sentence, List<NodeImpl> nodeList) {
        List<NodeImpl> newNodeList = new LinkedList<>();
        List<NodeImpl> tmpNodeList = new ArrayList<>();
        String fct = "o";
        for (NodeImpl node : nodeList) {
            String cct = getConcatType(sentence, node.loc, node.length);
            if (fct.equals("o")) {
                if (shouldConcat(cct)) {
                    tmpNodeList.add(node);
                    fct = cct;
                } else {
                    newNodeList.add(node);
                }
            } else {
                if (fct.equals(cct)) {
                    tmpNodeList.add(node);
                } else {
                    newNodeList.add(concat(tmpNodeList));
                    tmpNodeList.clear();
                    fct = "o";
                    if (shouldConcat(cct)) {
                        tmpNodeList.add(node);
                        fct = cct;
                    } else {
                        newNodeList.add(node);
                    }
                }
            }
        }
        if (!tmpNodeList.isEmpty()) {
            newNodeList.add(concat(tmpNodeList));
        }
        return newNodeList;
    }

    public String getConcatType(byte[] sentence, int pos, int length) {
        int fsz = ByteUtils.chsize(sentence[pos]);
        String fct = getCharType(ByteUtils.toString(sentence, pos, fsz).charAt(0));
        if (!shouldConcat(fct)) {
            return "o";
        }
        String surface = ByteUtils.toString(sentence, pos + fsz, length - fsz);
        for (int i = 0; i < surface.length(); ++i) {
            String cct = getCharType(surface.charAt(i));
            if (!fct.equals(cct)) {
                return "o";
            }
        }
        return fct;
    }

    public Boolean shouldConcat(String cct) {
        return concatSet.contains(cct);
    }

    public NodeImpl concat(List<NodeImpl> nodeList) {
        if (nodeList.size() == 1) {
            return nodeList.get(0);
        }
        int pos = nodeList.get(0).loc;
        int length = 0;
        for (NodeImpl node : nodeList) {
            length += node.length;
        }
        return unkNode(pos, length);
    }

    protected String getCharType(char ch) {
        String charType = "o";
        switch (ch) {
            case 0x3007: // zero 0
            case 0x4e00: // ichi 1
            case 0x4e8c: // ni 2
            case 0x4e09: // san 3
            case 0x56db: // shi 4
            case 0x4e94: // go 5
            case 0x516d: // roku 6
            case 0x4e03: // nana 7
            case 0x516b: // hachi 8
            case 0x4e5d: // ku 9
            case 0x5341: // ju 10
            case 0x767e: // hyaku 100
            case 0x5343: // sen 1000
            case 0x4e07: // man 10k
            case 0x5104: // oku
            case 0x5146: // cho
                charType = "N";
                break;
            default:
                if (ch >= '0' && ch <= '9') {
                    // Numerals.
                    charType = "n";
                } else if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')) {
                    // Alphabet.
                    charType = "a";
                } else if (ch >= 0x3040 && ch <= 0x309f) {
                    // Hiragana.
                    charType = "h";
                } else if (ch >= 0x30a0 && ch <= 0x30ff && ch != 0x30fb) {
                    // Katakana. Exclude middle dot (nakaguro).
                    charType = "k";
                } else if (ch >= 0x4e00 && ch <= 0x9fff) {
                    // Kanji (Chinese)
                    charType = "c";
                } else if (ch >= 0xff10 && ch <= 0xff19) {
                    // Numerals (full width)
                    charType = "n";
                } else if ((ch >= 0xff21 && ch <= 0xff3a) || (ch >= 0xff41 && ch <= 0xff5a)) {
                    // Alphabet (full width).
                    charType = "a";
                }
                break;
        }
        return charType;
    }

    // 素性文字列の生成
    public byte[][] getFeaturesByConfig(String surface, String reading) {
        if (!outputFeatures) {
            throw new IllegalStateException("Features are disabled.");
        }
        String ct = getCharType(surface.charAt(0));
        String val = unknownDump.get(ct);
        if (val == null) {
            val = unknownDump.get("o"); // default
        }
        return expandFeatures(val, surface, null);
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
