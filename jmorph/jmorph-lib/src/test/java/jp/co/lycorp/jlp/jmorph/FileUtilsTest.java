// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class FileUtilsTest {
    @TempDir
    Path tempDir;

    @Test
    public void testExistsFalse() throws IOException {
        // exec
        boolean actual = FileUtils.exists("not_exists.txt");

        // verify
        assertFalse(actual);
    }

    @Test
    public void testExistsTrue() throws IOException {
        // setup
        String filePath = this.getClass().getClassLoader().getResource("init.json").getPath();

        // exec
        boolean actual = FileUtils.exists(filePath);

        // verify
        assertTrue(actual);
    }

    @Test
    public void testResolvePathLocation() {
        assertEquals(tempDir.resolve("sample.txt").toString(),
                FileUtils.resolve(tempDir.toString(), "sample.txt"));
    }

    @Test
    public void testResolveFileLocation() {
        assertEquals(tempDir.resolve("sample.txt").toUri().toString(),
                FileUtils.resolve(tempDir.toUri().toString(), "sample.txt"));
    }

    @Test
    public void testResolveClasspathLocation() {
        assertEquals("classpath:dict/sample.txt", FileUtils.resolve("classpath:dict", "sample.txt"));
        assertEquals("classpath:dict/sample.txt", FileUtils.resolve("classpath:dict/", "sample.txt"));
    }

    @Test
    public void testResolveJarLocation() {
        String jarLocation = "jar:file:///tmp/model.jar!/dict";
        assertEquals(jarLocation + "/sample.txt", FileUtils.resolve(jarLocation, "sample.txt"));
        assertEquals(jarLocation + "/sample.txt", FileUtils.resolve(jarLocation + "/", "sample.txt"));
    }

    @Test
    public void testRemoveExtension() {
        // setup
        String filePath = this.getClass().getClassLoader().getResource("init.json").getPath();
        String expected = filePath.substring(0, filePath.length() - ".json".length());

        // exec
        String actual = FileUtils.removeExtension(filePath);

        // verify
        assertEquals(expected, actual);
    }

    @Test
    public void testReadAllLines() throws IOException {
        // setup
        String filePath = this.getClass().getClassLoader().getResource("init.json").getPath();

        // exec
        List<String> actual = FileUtils.readAllLines(filePath);

        // verify
        assertEquals("{", actual.get(0));
        assertEquals("  \"description\": \"sample JMorph config\",", actual.get(1));
        assertEquals("  \"analyzer\": {", actual.get(3));
        assertEquals("}", actual.get(22));
        assertEquals(23, actual.size());
    }

    @Test
    public void testOpenPath() throws IOException {
        // setup
        Path filePath = tempDir.resolve("sample.txt");
        Files.writeString(filePath, "hello", StandardCharsets.UTF_8);

        // exec
        String actual;
        try (InputStream in = FileUtils.open(filePath.toString())) {
            actual = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }

        // verify
        assertEquals("hello", actual);
    }

    @Test
    public void testOpenFileLocation() throws IOException {
        // setup
        Path filePath = tempDir.resolve("sample.txt");
        Files.writeString(filePath, "hello", StandardCharsets.UTF_8);

        // exec
        String actual;
        try (InputStream in = FileUtils.open(filePath.toUri().toString())) {
            actual = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }

        // verify
        assertEquals("hello", actual);
    }

    @Test
    public void testOpenFileLocationWithEscapedCharacters() throws IOException {
        // setup
        Path filePath = tempDir.resolve("sample file.txt");
        Files.writeString(filePath, "hello", StandardCharsets.UTF_8);

        // exec
        String actual;
        try (InputStream in = FileUtils.open(filePath.toUri().toString())) {
            actual = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }

        // verify
        assertEquals("hello", actual);
    }

    @Test
    public void testOpenFileLocationRequiresAbsolutePath() {
        // exec & verify
        assertThrows(IllegalArgumentException.class, () -> FileUtils.open("file:relative.txt"));
    }

    @Test
    public void testOpenClasspathLocation() throws IOException {
        // exec
        List<String> actual = FileUtils.readAllLines("classpath:init.json");

        // verify
        assertEquals("{", actual.get(0));
        assertEquals(23, actual.size());
    }

    @Test
    public void testOpenClasspathLocationWithLeadingSlash() throws IOException {
        // exec
        List<String> actual = FileUtils.readAllLines("classpath:/init.json");

        // verify
        assertEquals("{", actual.get(0));
        assertEquals(23, actual.size());
    }

    @Test
    public void testOpenClasspathLocationNotFound() {
        // exec & verify
        assertThrows(IOException.class, () -> FileUtils.open("classpath:not-found.txt"));
    }

    @Test
    public void testOpenJarLocation() throws IOException {
        // setup
        Path jarPath = createJar("dict/sample.txt", "jar-value");

        // exec
        String actual;
        try (InputStream in = FileUtils.open("jar:" + jarPath.toUri() + "!/dict/sample.txt")) {
            actual = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }

        // verify
        assertEquals("jar-value", actual);
    }

    @Test
    public void testOpenJarLocationNotFound() throws IOException {
        // setup
        Path jarPath = createJar("dict/sample.txt", "jar-value");

        // exec & verify
        assertThrows(IOException.class, () -> FileUtils.open("jar:" + jarPath.toUri() + "!/dict/not-found.txt"));
    }

    @Test
    public void testOpenJarLocationRequiresFileUri() throws IOException {
        // setup
        Path jarPath = createJar("dict/sample.txt", "jar-value");

        // exec & verify
        assertThrows(IllegalArgumentException.class, () -> FileUtils.open("jar:" + jarPath + "!/dict/sample.txt"));
    }

    @Test
    public void testExistsClasspathAndJarLocations() throws IOException {
        // setup
        Path jarPath = createJar("dict/sample.txt", "jar-value");

        // exec & verify
        assertTrue(FileUtils.exists("classpath:init.json"));
        assertFalse(FileUtils.exists("classpath:not-found.txt"));
        assertTrue(FileUtils.exists("jar:" + jarPath.toUri() + "!/dict/sample.txt"));
        assertFalse(FileUtils.exists("jar:" + jarPath.toUri() + "!/dict/not-found.txt"));
        assertFalse(FileUtils.exists("jar:" + jarPath + "!/dict/sample.txt"));
    }

    @Test
    public void testReadAllLinesWithCharset() throws IOException {
        // setup
        Path filePath = tempDir.resolve("sample.txt");
        Files.write(filePath, "あ\nい".getBytes(StandardCharsets.UTF_8));

        // exec
        List<String> actual = FileUtils.readAllLines(filePath.toString(), StandardCharsets.UTF_8);

        // verify
        assertEquals(List.of("あ", "い"), actual);
    }

    @Test
    public void testGetFileSize() throws Exception {
        // setup
        Path filePath = tempDir.resolve("sample.txt");
        Files.writeString(filePath, "hello", StandardCharsets.UTF_8);
        Path jarPath = createJar("dict/sample.txt", "jar-value");
        Path classpathPath = Path.of(this.getClass().getClassLoader().getResource("init.json").toURI());

        // exec & verify
        assertEquals(5, FileUtils.getFileSize(filePath.toString()));
        assertEquals(5, FileUtils.getFileSize(filePath.toUri().toString()));
        assertEquals(9, FileUtils.getFileSize("jar:" + jarPath.toUri() + "!/dict/sample.txt"));
        assertEquals(Files.size(classpathPath), FileUtils.getFileSize("classpath:init.json"));
    }

    @Test
    public void testGetFileSizeForClasspathJarLocation() throws IOException {
        // setup
        Path jarPath = createJar("dict/sample.txt", "jar-value");
        ClassLoader original = Thread.currentThread().getContextClassLoader();

        // exec & verify
        try (URLClassLoader classLoader = new URLClassLoader(new URL[] {jarPath.toUri().toURL()}, null)) {
            Thread.currentThread().setContextClassLoader(classLoader);
            assertEquals(9, FileUtils.getFileSize("classpath:dict/sample.txt"));
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
    }

    @Test
    public void testGetFileSizeForClasspathFallbackLocation() throws IOException {
        // setup
        byte[] contents = "fallback".getBytes(StandardCharsets.UTF_8);
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        ClassLoader classLoader = new ClassLoader(null) {
            @Override
            public URL getResource(String name) {
                if (!"virtual.bin".equals(name)) {
                    return null;
                }
                try {
                    return new URL(null, "memory:virtual.bin", new URLStreamHandler() {
                        @Override
                        protected URLConnection openConnection(URL url) {
                            return new URLConnection(url) {
                                @Override
                                public void connect() {
                                }

                                @Override
                                public InputStream getInputStream() {
                                    return new ByteArrayInputStream(contents);
                                }
                            };
                        }
                    });
                } catch (IOException e) {
                    throw new AssertionError(e);
                }
            }

            @Override
            public InputStream getResourceAsStream(String name) {
                if (!"virtual.bin".equals(name)) {
                    return null;
                }
                return new ByteArrayInputStream(contents);
            }
        };

        // exec & verify
        try {
            Thread.currentThread().setContextClassLoader(classLoader);
            assertEquals(contents.length, FileUtils.getFileSize("classpath:virtual.bin"));
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
    }

    @Test
    public void testOpenBufferForPathUsesLittleEndianOrder() throws IOException {
        // setup
        Path filePath = tempDir.resolve("buffer.bin");
        Files.write(filePath, new byte[] {1, 2, 3, 4});

        // exec
        ByteBuffer actual = FileUtils.openBuffer(filePath.toString());

        // verify
        assertEquals(ByteOrder.LITTLE_ENDIAN, actual.order());
        assertEquals(0x04030201, actual.getInt(0));
    }

    @Test
    public void testOpenBufferForClasspathLocationUsesLittleEndianOrder() throws IOException {
        // exec
        ByteBuffer actual = FileUtils.openBuffer("classpath:init.json");

        // verify
        assertEquals(ByteOrder.LITTLE_ENDIAN, actual.order());
        assertEquals((byte) '{', actual.get(0));
    }

    @Test
    public void testOpenBufferForJarLocationUsesLittleEndianOrder() throws IOException {
        // setup
        Path jarPath = createJar("dict/buffer.bin", new byte[] {1, 2, 3, 4});

        // exec
        ByteBuffer actual = FileUtils.openBuffer("jar:" + jarPath.toUri() + "!/dict/buffer.bin");

        // verify
        assertEquals(ByteOrder.LITTLE_ENDIAN, actual.order());
        assertEquals(0x04030201, actual.getInt(0));
    }

    private Path createJar(String entryName, String contents) throws IOException {
        return createJar(entryName, contents.getBytes(StandardCharsets.UTF_8));
    }

    private Path createJar(String entryName, byte[] contents) throws IOException {
        Path jarPath = tempDir.resolve("test.jar");
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                JarOutputStream jar = new JarOutputStream(buffer)) {
            jar.putNextEntry(new JarEntry(entryName));
            jar.write(contents);
            jar.closeEntry();
            jar.finish();
            Files.write(jarPath, buffer.toByteArray());
        }
        return jarPath;
    }
}
