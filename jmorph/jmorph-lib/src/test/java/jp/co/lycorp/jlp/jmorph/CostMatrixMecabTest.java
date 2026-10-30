// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class CostMatrixMecabTest {

    @Test
    public void testDefaultConstructor() {
        // setup
        CostMatrixMecab target = new CostMatrixMecab();

        // verify
        assertEquals(0, target.getLsize());
        assertEquals(0, target.getRsize());
        assertEquals(Short.MAX_VALUE, CostMatrixMecab.userDicEntriesIndex);
        assertEquals(10, target.getCost(CostMatrixMecab.userDicEntriesIndex, 0));
        assertEquals(10, target.getCost(0, CostMatrixMecab.userDicEntriesIndex));
    }

    @Test
    public void testConstructor() throws IOException {
        // setup
        String matrixPath = this.getClass().getClassLoader()
                .getResource(Paths.get("mecab-small-dic", "final", "matrix.bin").toString())
                .getPath();

        // exec
        CostMatrixMecab target = new CostMatrixMecab(matrixPath);

        // verify
        assertEquals(11, target.getLsize());
        assertEquals(11, target.getRsize());
        assertEquals(417, target.getCost(2, 4));
    }

    @Test
    public void testConnectionIdRange() throws IOException {
        String matrixPath = this.getClass().getClassLoader()
                .getResource(Paths.get("mecab-small-dic", "final", "matrix.bin").toString())
                .getPath();
        CostMatrixMecab target = new CostMatrixMecab(matrixPath);

        assertTrue(target.isValidRc(0));
        assertTrue(target.isValidRc(10));
        assertFalse(target.isValidRc(-1));
        assertFalse(target.isValidRc(11));
        assertTrue(target.isValidRc(CostMatrixMecab.userDicEntriesIndex));

        assertTrue(target.isValidLc(0));
        assertTrue(target.isValidLc(10));
        assertFalse(target.isValidLc(-1));
        assertFalse(target.isValidLc(11));
        assertTrue(target.isValidLc(CostMatrixMecab.userDicEntriesIndex));
    }

    @Test
    public void testConstructorInvalidHeader(@TempDir Path tempDir) throws IOException {
        // setup
        Path matrixPath = tempDir.resolve("matrix.bin");
        Files.write(matrixPath, new byte[]{1, 2, 3});

        // exec & verify
        assertThrows(IOException.class, () -> new CostMatrixMecab(matrixPath.toString()));
    }
}
