// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class UserDicInRequestProviderBasicTest {

    private UserDicInRequestImpl createdDic;

    @BeforeEach
    public void setUp() throws Exception {
        UserDicEntry entry0 = new UserDicEntry("架空ランドマーク");
        UserDicEntry entry1 = new UserDicEntry("架空冒険譚", "かくうぼうけんたん");
        UserDicEntry entry2 = new UserDicEntry("例示姓", "れいじせい");
        UserDicEntry entry3 = new UserDicEntry("例示名", "れいじめい");
        UserDicEntry entry4 = new UserDicEntry("架空固有名詞", "かくうこゆうめいし");
        List<UserDicEntry> userDicEntries = new ArrayList<>();
        userDicEntries.add(entry0);
        userDicEntries.add(entry1);
        userDicEntries.add(entry2);
        userDicEntries.add(entry3);
        userDicEntries.add(entry4);

        String modelPath = this.getClass().getClassLoader().getResource("mecab-small-dic/final").getPath();
        MorphDic morphDic = new MorphDicMecab(modelPath, true);
        CostMatrix costMatrix = new CostMatrixMecab(FileUtils.resolve(modelPath, "matrix.bin"));
        UnknownDicBasicConfig unknownDicConfig = new UnknownDicBasicConfig(null, ConcatMode.NONE, " ");
        String dumpUnknownLocation = FileUtils.resolve(modelPath, "dump.unknown");
        UnknownDic unknownDic = new UnknownDicBasic(
                FileUtils.resolve(modelPath, "unk.csv"), dumpUnknownLocation, true, unknownDicConfig);
        UserDicInRequestProvider userDicInRequestProvider = new UserDicInRequestProviderBasic(
                FileUtils.resolve(modelPath, "dump.userdic"), dumpUnknownLocation, true, " ",
                costMatrix.getUserDicEntriesIndex(), morphDic, unknownDic);
        MorphDic createdDicByProvider = userDicInRequestProvider.create(userDicEntries);
        assertTrue(createdDicByProvider instanceof UserDicInRequestImpl);
        createdDic = (UserDicInRequestImpl) createdDicByProvider;
    }

    @Test
    public void testLookup() {
        // setup
        byte[] sentence = ByteUtils.toBytes("架空ランドマークで架空冒険譚（作者：例示姓例示名）を見た");
        int pos = ByteUtils.toBytes("架空ランドマークで架空冒険譚（作者：").length;
        int dictId = 0;

        // exec
        List<NodeImpl> actual = createdDic.lookup(sentence, pos, dictId);

        // verify
        assertEquals(1, actual.size());
        assertEquals(ByteUtils.toBytes("架空ランドマークで架空冒険譚（作者：").length, actual.get(0).loc);
        assertEquals(ByteUtils.toBytes("例示姓").length, actual.get(0).length);
        assertEquals(0, actual.get(0).token.getCost());
        assertEquals(-1, actual.get(0).token.getFeatureSetId());
        assertEquals(2, actual.get(0).token.getTokenId());
    }

    @Test
    public void testLookupStopsWhenTimeoutIsExceeded() {
        List<UserDicEntry> entries = new ArrayList<>();
        for (int i = 0; i < 1024; i++) {
            entries.add(new UserDicEntry("a"));
        }
        UserDicInRequestImpl dictionary = new UserDicInRequestImpl(
                entries, 0, null, null, false, (byte) ' ', null, null);
        AtomicLong nanoTime = new AtomicLong(0);
        TimeoutGuard timeout = TimeoutGuard.start(1, nanoTime::get);
        nanoTime.set(1_000_000L);

        assertThrows(TimeoutExceededException.class,
                () -> dictionary.lookup(ByteUtils.toBytes("b"), 0, 0, timeout));
    }

    @Test
    public void testProviderCreationChecksTimeout() {
        AtomicLong nanoTime = new AtomicLong(0);
        TimeoutGuard timeout = TimeoutGuard.start(1, nanoTime::get);
        UserDicInRequestProvider provider = new UserDicInRequestProvider() {
            public MorphDic create(List<? extends UserDicEntry> entries) {
                return createdDic;
            }

            public MorphDic create(List<? extends UserDicEntry> entries, TimeoutGuard guard) {
                guard.check();
                nanoTime.set(1_000_000L);
                guard.check();
                return createdDic;
            }
        };
        DictionaryImpl dictionary = new DictionaryImpl(
                new ArrayList<>(), null, null, provider, false);

        assertThrows(TimeoutExceededException.class,
                () -> dictionary.addUserDicInRequest(
                        List.of(new UserDicEntry("a")), timeout));
        assertDoesNotThrow(() -> dictionary.addUserDicInRequest(
                List.of(new UserDicEntry("a")), TimeoutGuard.disabled()));
    }

    @Test
    public void testGetFeatures() {
        // setup
        TokenMecab token = new TokenMecab((short) 8, (short) 9, (short) 10, 11, 0);
        NodeImpl node = new NodeImpl(token, 0, 0, 14);

        // exec
        byte[][] actual = createdDic.getFeatures(node);

        // verify
        assertNotEquals(null, actual); // not empty check
    }

    @Test
    public void testGetFeaturesWithReading() {
        // setup
        TokenMecab token = new TokenMecab((short) 8, (short) 9, (short) 10, 11, 1);
        NodeImpl node = new NodeImpl(token, 0, 0, 12);

        // exec
        byte[][] actual = createdDic.getFeatures(node);

        // verify
        assertEquals("品詞1", ByteUtils.toString(actual[0]));
        assertEquals("詳細品詞1", ByteUtils.toString(actual[1]));
        assertEquals("かくうぼうけんたん", ByteUtils.toString(actual[2]));
    }
}
