// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MorphDicMecab implements MorphDic {

    private DartsMecab darts;
    private ByteBuffer tokenBuffer;
    private ByteBuffer featureBuffer;

    public static final int TOKEN_BYTES = 16;
    private static final int CHARSET_SIZE = 32;
    private static final int DICTIONARY_MAGIC_ID = 0xef718f77;
    private static final int DIC_VERSION = 102;

    public MorphDicMecab(String path, boolean outputFeatures) throws RuntimeException, IOException {
        this(path, outputFeatures, null);
    }

    public MorphDicMecab(String path, boolean outputFeatures, CostMatrix costMatrix)
            throws RuntimeException, IOException {
        readDictionaryFile(FileUtils.resolve(path, "sys.dic"), outputFeatures, costMatrix);

    }

    public MorphDicMecab(List<? extends UserDicEntry> userDicEntries) {
        // do nothing. (for UserDicInRequest constructor)
    }

    private void readDictionaryFile(String filename, Boolean outputFeatures, CostMatrix costMatrix)
            throws IOException {
        try {
            ByteBuffer buffer = FileUtils.openBuffer(filename);
            long fileSize = buffer.limit();
            if (fileSize < 100) {
                throw new IOException("Dictionary file is too small: " + filename);
            }
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            // header
            int magic = buffer.getInt();
            if ((magic ^ DICTIONARY_MAGIC_ID) != fileSize) {
                throw new IOException("Dictionary file is broken: invalid magic number: " + filename);
            }

            int version = buffer.getInt();
            if (version != DIC_VERSION) {
                throw new IOException("Incompatible dictionary version: " + version);
            }

            int type = buffer.getInt();
            int lexSize = buffer.getInt();
            int lSize = buffer.getInt();
            int rSize = buffer.getInt();
            int dSize = buffer.getInt();
            int tSize = buffer.getInt();
            int fSize = buffer.getInt();
            int dummy = buffer.getInt();

            // charset
            buffer.position(buffer.position() + CHARSET_SIZE);
            validateSectionSizes(filename, buffer.remaining(), dSize, tSize, fSize);

            // darts
            byte[] daBytes = new byte[dSize];
            buffer.get(daBytes);
            darts = new DartsMecab();
            darts.setArray(daBytes);

            // tokens
            byte[] tokenBytes = new byte[tSize];
            buffer.get(tokenBytes);
            tokenBuffer = ByteBuffer.wrap(tokenBytes).order(ByteOrder.LITTLE_ENDIAN);
            if (costMatrix != null) {
                validateConnectionIds(filename, costMatrix);
            }

            // features
            if (outputFeatures) {
                byte[] featureBytes = new byte[fSize];
                buffer.get(featureBytes);
                featureBuffer = ByteBuffer.wrap(featureBytes).order(ByteOrder.LITTLE_ENDIAN);
            }
        } catch (IOException e) {
            throw new IOException("Failed to read dictionary file", e);
        }
    }

    private static void validateSectionSizes(
            String filename, int remainingBytes, int dSize, int tSize, int fSize) throws IOException {
        if (dSize < 0 || tSize < 0 || fSize < 0) {
            throw new IOException("Dictionary file is broken: negative section size: " + filename);
        }
        if (dSize % DartsMecab.Unit.BYTE_SIZE != 0) {
            throw new IOException("Dictionary file is broken: invalid Darts section size: " + filename);
        }
        if (tSize % TOKEN_BYTES != 0) {
            throw new IOException("Dictionary file is broken: invalid token section size: " + filename);
        }

        long declaredBytes = (long) dSize + tSize + fSize;
        if (declaredBytes != remainingBytes) {
            throw new IOException("Dictionary file is broken: section sizes do not match file size: " + filename);
        }
    }

    private void validateConnectionIds(String filename, CostMatrix costMatrix) throws IOException {
        int tokenCount = tokenBuffer.capacity() / TOKEN_BYTES;
        for (int tokenIndex = 0; tokenIndex < tokenCount; tokenIndex++) {
            int offset = tokenIndex * TOKEN_BYTES;
            int lc = tokenBuffer.getShort(offset);
            int rc = tokenBuffer.getShort(offset + Short.BYTES);
            if (!costMatrix.isValidRc(rc) || !costMatrix.isValidLc(lc)) {
                throw new IOException("Dictionary connection ID is out of CostMatrix range: "
                        + "file=" + filename + ", tokenIndex=" + tokenIndex
                        + ", rc=" + rc + ", lc=" + lc);
            }
        }
    }

    public List<NodeImpl> lookup(byte[] sentence, int pos, int dictId) {
        List<NodeImpl> nodeList = new ArrayList<>();

        List<DartsMecab.Result> results = darts.commonPrefixSearch(sentence, pos, 64);

        for (DartsMecab.Result result : results) {
            int value = result.value;
            int length = result.length;
            for (Token token : getTokens(value)) {
                nodeList.add(new NodeImpl(token, dictId, pos, length));
            }
        }
        return nodeList;
    }

    public List<NodeImpl> lookup(byte[] sentence, int pos, int dictId, TimeoutGuard timeout) {
        return lookup(sentence, pos, dictId);
    }

    private TokenMecab getToken(int index) {
        int offset = index * TOKEN_BYTES;
        short lc = tokenBuffer.getShort(offset);
        short rc = tokenBuffer.getShort(offset + Short.BYTES);
        short posid = tokenBuffer.getShort(offset + Short.BYTES * 2);
        short cost = tokenBuffer.getShort(offset + Short.BYTES * 3);
        int feature = tokenBuffer.getInt(offset + Short.BYTES * 4);
        int compound = tokenBuffer.getInt(offset + Short.BYTES * 4 + Integer.BYTES);
        return new TokenMecab(lc, rc, cost, feature);
    }

    private List<TokenMecab> getTokens(int value) {
        List<TokenMecab> tokens = new ArrayList<>();
        int tokenIndex = getTokenIndex(value);
        int tokenSize = getTokenSize(value);

        for (int j = 0; j < tokenSize; j++) {
            int currentTokenIndex = tokenIndex + j;
            if (currentTokenIndex * TOKEN_BYTES < tokenBuffer.capacity()) {
                tokens.add(getToken(currentTokenIndex));
            } else {
                throw new IllegalArgumentException(
                        "Token index " + currentTokenIndex + " is out of bounds (capacity: " +
                                (tokenBuffer.capacity() / TOKEN_BYTES) + ")");
            }
        }
        return tokens;
    }

    private int getTokenIndex(int value) {
        return value >> 8;
    }

    private int getTokenSize(int value) {
        return value & 0xFF;
    }

    private String getFeaturesFromOffset(int offset) {
        if (featureBuffer == null) {
            throw new IllegalStateException("Feature Buffer has not been initialized");
        }
        int endPosition = offset;
        while (endPosition < featureBuffer.limit() && featureBuffer.get(endPosition) != 0) {
            endPosition++;
        }

        byte[] features = new byte[endPosition - offset];
        for (int i = 0; i < features.length; i++) {
            features[i] = featureBuffer.get(offset + i);
        }

        return new String(features, StandardCharsets.UTF_8);
    }

    public byte[][] getFeatures(NodeImpl node) {
        String featureString = getFeaturesFromOffset(node.getToken().getFeatureSetId());
        List<String> features = CsvParser.parse(featureString);
        List<byte[]> featureBytes = new ArrayList<>();
        for (String feature : features) {
            featureBytes.add(ByteUtils.toBytes(feature));
        }
        return featureBytes.toArray(new byte[featureBytes.size()][]);
    }

    public boolean isPhrase(NodeImpl node) {
        return false;
    }

    public List<NodeImpl> getPhrase(NodeImpl node) {
        return null;
    }
}
