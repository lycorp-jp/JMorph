// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DictionaryImplTest {

    private DictionaryImpl target;

    private DictionaryImpl createDictionary(boolean concat, boolean outputFeatures) throws IOException {
        String modelPath = this.getClass().getClassLoader().getResource("mecab-small-dic/final").getPath();
        MorphDic morphDic = new MorphDicMecab(modelPath, outputFeatures);
        List<MorphDic> morphDicList = new ArrayList<>();
        morphDicList.add(morphDic);
        CostMatrix costMatrix = new CostMatrixMecab(FileUtils.resolve(modelPath, "matrix.bin"));
        ConcatMode concatMode = concat ? ConcatMode.ALL : ConcatMode.NONE;
        UnknownDicBasicConfig unknownDicConfig = new UnknownDicBasicConfig(null, concatMode, " ");
        String dumpUnknownLocation = FileUtils.resolve(modelPath, "dump.unknown");
        UnknownDic unknownDic = new UnknownDicBasic(
                FileUtils.resolve(modelPath, "unk.csv"), dumpUnknownLocation, outputFeatures, unknownDicConfig);
        UserDicInRequestProvider userDicInRequestProvider = new UserDicInRequestProviderBasic(
                FileUtils.resolve(modelPath, "dump.userdic"), dumpUnknownLocation, outputFeatures, " ",
                costMatrix.getUserDicEntriesIndex(), morphDic, unknownDic);
        return new DictionaryImpl(morphDicList, costMatrix, unknownDic, userDicInRequestProvider, outputFeatures);
    }

    @Test
    public void testGetCostMatrix() throws IOException {
        // setup
        target = createDictionary(false, false);
        int rc = 2;
        int lc = 4;
        int expected = 417;

        // exec
        CostMatrix actual = target.getCostMatrix();

        // verify
        assertEquals(expected, actual.getCost(rc, lc));
    }

    @Test
    public void testLookup() throws IOException {
        // setup
        target = createDictionary(false, false);
        byte[] sentence = "花子は太郎が好きだ".getBytes(StandardCharsets.UTF_8);
        byte[] word1 = "花子".getBytes(StandardCharsets.UTF_8);

        // exec
        List<NodeImpl> actual = target.lookup(sentence, 0);
        List<NodeImpl> word1Lookup = target.lookup(word1, 0);

        // verify
        assertEquals(word1Lookup.size(), actual.size());
        assertEquals(0, actual.get(0).getDictId());
        assertEquals(0, actual.get(0).getLoc());
        assertEquals(word1Lookup.get(0).getLength(), actual.get(0).getLength());
        assertEquals(word1Lookup.get(0).getToken().getFeatureSetId(), actual.get(0).getToken().getFeatureSetId());
    }

    @Test
    public void testConcatFalse() throws IOException {
        // setup
        byte[] sentence = "alphabet".getBytes(StandardCharsets.UTF_8);
        target = createDictionary(false, false);
        Lattice lattice = new Lattice(target.getCostMatrix(), sentence);
        for (int i = 0; i < sentence.length; ++i) {
            if (lattice.get(i) != null) {
                lattice.connect(target.lookup(sentence, i));
            }
        }
        lattice.finish();
        List<NodeImpl> nodeList = lattice.getBestPath(1);

        // exec
        nodeList = target.concat(lattice.getSentence(), nodeList);

        // verify
        assertEquals(8, nodeList.size());

    }

    @Test
    public void testConcatTrue() throws IOException {
        // setup
        byte[] sentence = "alphabet".getBytes(StandardCharsets.UTF_8);
        target = createDictionary(true, false);
        Lattice lattice = new Lattice(target.getCostMatrix(), sentence);
        for (int i = 0; i < sentence.length; ++i) {
            if (lattice.get(i) != null) {
                lattice.connect(target.lookup(sentence, i));
            }
        }
        lattice.finish();
        List<NodeImpl> nodeList = lattice.getBestPath(1);

        // exec
        nodeList = target.concat(lattice.getSentence(), nodeList);

        // verify
        assertEquals(1, nodeList.size());
    }

    @Test
    public void testGetAllFeatures() throws IOException {
        // setup
        target = createDictionary(false, false);

        byte[] sentence = "花子は太郎が好きだ".getBytes(StandardCharsets.UTF_8);
        Lattice lattice = new Lattice(target.getCostMatrix(), sentence);
        for (int i = 0; i < sentence.length; ++i) {
            if (lattice.get(i) != null) {
                lattice.connect(target.lookup(sentence, i));
            }
        }
        lattice.finish();
        List<NodeImpl> nodeList = lattice.getBestPath(1);
        List<TokenInfo> resultList = new LinkedList<>();

        // exec
        for (NodeImpl node : nodeList) {
            for (byte[][] allFeatures : target.getAllFeatures(lattice.getSentence(), node)) {
                resultList.add(new TokenInfoImpl(allFeatures));
            }
        }

        // verify
        assertEquals("花子", new String(resultList.get(0).getBytes()[0], StandardCharsets.UTF_8));
        assertEquals("は", new String(resultList.get(1).getBytes()[0], StandardCharsets.UTF_8));
        assertEquals("太郎", new String(resultList.get(2).getBytes()[0], StandardCharsets.UTF_8));
        assertEquals("が", new String(resultList.get(3).getBytes()[0], StandardCharsets.UTF_8));
        assertEquals("好き", new String(resultList.get(4).getBytes()[0], StandardCharsets.UTF_8));
        assertEquals("だ", new String(resultList.get(5).getBytes()[0], StandardCharsets.UTF_8));
    }

    @Test
    public void testGetAllFeaturesWithDump() throws IOException {
        // setup
        target = createDictionary(false, true);

        byte[] sentence = "花子は太郎が好きだ".getBytes(StandardCharsets.UTF_8);
        Lattice lattice = new Lattice(target.getCostMatrix(), sentence);
        for (int i = 0; i < sentence.length; ++i) {
            if (lattice.get(i) != null) {
                lattice.connect(target.lookup(sentence, i));
            }
        }
        lattice.finish();
        List<NodeImpl> nodeList = lattice.getBestPath(1);
        List<TokenInfo> resultList = new LinkedList<>();

        // exec
        for (NodeImpl node : nodeList) {
            for (byte[][] allFeatures : target.getAllFeatures(lattice.getSentence(), node)) {
                resultList.add(new TokenInfoImpl(allFeatures));
            }
        }

        // verify
        assertEquals("花子", new String(resultList.get(0).getBytes()[0], StandardCharsets.UTF_8));
        assertEquals("品詞1", new String(resultList.get(0).getBytes()[1], StandardCharsets.UTF_8));
        assertEquals("は", new String(resultList.get(1).getBytes()[0], StandardCharsets.UTF_8));
        assertEquals("品詞2", new String(resultList.get(1).getBytes()[1], StandardCharsets.UTF_8));
        assertEquals("太郎", new String(resultList.get(2).getBytes()[0], StandardCharsets.UTF_8));
        assertEquals("品詞1", new String(resultList.get(2).getBytes()[1], StandardCharsets.UTF_8));
        assertEquals("が", new String(resultList.get(3).getBytes()[0], StandardCharsets.UTF_8));
        assertEquals("品詞2", new String(resultList.get(3).getBytes()[1], StandardCharsets.UTF_8));
        assertEquals("好き", new String(resultList.get(4).getBytes()[0], StandardCharsets.UTF_8));
        assertEquals("品詞1", new String(resultList.get(4).getBytes()[1], StandardCharsets.UTF_8));
        assertEquals("だ", new String(resultList.get(5).getBytes()[0], StandardCharsets.UTF_8));
        assertEquals("品詞3", new String(resultList.get(5).getBytes()[1], StandardCharsets.UTF_8));
    }

    @Test
    public void testGetEos() throws IOException {
        // setup
        target = createDictionary(false, false);

        byte[] sentence = "花子は太郎が好きだ".getBytes(StandardCharsets.UTF_8);
        Lattice lattice = new Lattice(target.getCostMatrix(), sentence);
        for (int i = 0; i < sentence.length; ++i) {
            if (lattice.get(i) != null) {
                lattice.connect(target.lookup(sentence, i));
            }
        }
        lattice.finish();

        // exec
        TokenInfoImpl actual = new TokenInfoImpl(target.getEos(lattice.getEos()));

        // verify
        assertEquals("EOS", new String(actual.getBytes()[0], StandardCharsets.UTF_8));
    }

    // TODO: リクエスト時ユーザー辞書の確認テスト
}
