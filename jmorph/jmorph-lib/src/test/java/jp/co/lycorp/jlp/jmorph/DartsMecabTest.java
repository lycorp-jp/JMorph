// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class DartsMecabTest {

    private AutoCloseable closeable;

    @BeforeEach
    public void setUp() throws Exception {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    public void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    public void testCommonPrefixSearchWithNullArray() {
        // setup
        DartsMecab newDarts = new DartsMecab();
        byte[] key = ByteUtils.toBytes("test");

        // verify
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> newDarts.commonPrefixSearch(key, 10));

        assertTrue(exception.getMessage().contains("Darts array has not been initialized"));
    }

    @Test
    public void testCommonPrefixSearch() {
        // setup
        DartsMecab darts = new DartsMecab();
        darts.setArray(readDartsArray());

        byte[] key = ByteUtils.toBytes("太郎");
        // execute
        List<DartsMecab.Result> results = darts.commonPrefixSearch(key, 64);

        // verify
        assertEquals(2, results.size());
        assertEquals(1025, results.get(0).value);
        assertEquals(ByteUtils.toBytes("太").length, results.get(0).length);
        assertEquals(1281, results.get(1).value);
        assertEquals(key.length, results.get(1).length);
    }

    @Test
    public void testCommonPrefixSearchWithOffset() {
        // setup
        DartsMecab darts = new DartsMecab();
        darts.setArray(readDartsArray());

        byte[] prefix = ByteUtils.toBytes("彼は");
        byte[] word = ByteUtils.toBytes("太郎");
        byte[] key = new byte[prefix.length + word.length];
        System.arraycopy(prefix, 0, key, 0, prefix.length);
        System.arraycopy(word, 0, key, prefix.length, word.length);

        // execute
        List<DartsMecab.Result> results = darts.commonPrefixSearch(key, prefix.length, 64);

        // verify
        assertEquals(2, results.size());
        assertEquals(1025, results.get(0).value);
        assertEquals(ByteUtils.toBytes("太").length, results.get(0).length);
        assertEquals(1281, results.get(1).value);
        assertEquals(word.length, results.get(1).length);
    }

    private byte[] readDartsArray() {
        String dictionaryPath = this.getClass().getClassLoader().getResource(Paths.get("mecab-small-dic", "final", "sys.dic").toString()).getPath();
        try (FileChannel fileChannel = new FileInputStream(dictionaryPath).getChannel()) {
            long fileSize = fileChannel.size();
            MappedByteBuffer buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileSize);
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            // header
            int magic = buffer.getInt();
            int version = buffer.getInt();
            int type = buffer.getInt();
            int lexSize = buffer.getInt();
            int lSize = buffer.getInt();
            int rSize = buffer.getInt();
            int dSize = buffer.getInt();
            int tSize = buffer.getInt();
            int fSize = buffer.getInt();
            int dummy = buffer.getInt();

            // charset
            buffer.position(buffer.position() + 32);

            // darts
            byte[] daBytes = new byte[dSize];
            buffer.get(daBytes);
            return daBytes;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
