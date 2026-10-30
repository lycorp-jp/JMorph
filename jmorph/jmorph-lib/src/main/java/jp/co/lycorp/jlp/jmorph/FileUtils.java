// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public final class FileUtils {
    private static final String CLASSPATH_PREFIX = "classpath:";
    private static final String JAR_PREFIX = "jar:";
    private static final String FILE_PREFIX = "file:";

    private enum LocationType {
        CLASSPATH,
        JAR,
        FILE,
        PATH
    }

    private static final class JarLocation {
        private final Path jarPath;
        private final String entryPath;

        private JarLocation(Path jarPath, String entryPath) {
            this.jarPath = jarPath;
            this.entryPath = entryPath;
        }
    }

    private FileUtils() {
    }

    public static ByteBuffer openBuffer(String location) throws IOException {
        Objects.requireNonNull(location, "location");

        LocationType type = detectType(location);
        if (type == LocationType.CLASSPATH || type == LocationType.JAR) {
            try (InputStream in = open(location)) {
                return littleEndian(ByteBuffer.wrap(in.readAllBytes()));
            }
        }

        Path path = (type == LocationType.FILE)
                ? parseAbsoluteFilePath(location, true)
                : Paths.get(location);

        return mapReadOnly(path);
    }

    public static InputStream open(String location) throws IOException {
        Objects.requireNonNull(location, "location");

        LocationType type = detectType(location);
        if (type == LocationType.CLASSPATH) {
            return openClasspath(location);
        }

        if (type == LocationType.JAR) {
            return openJar(location);
        }

        if (type == LocationType.FILE) {
            return openFile(location);
        }

        return openPath(location);
    }

    private static InputStream openClasspath(String location) throws IOException {
        String path = classpathPath(location);
        ClassLoader cl = contextClassLoader();
        InputStream in = cl.getResourceAsStream(path);
        if (in == null) {
            throw new FileNotFoundException("Classpath resource not found: " + location);
        }
        return in;
    }

    private static InputStream openJar(String location) throws IOException {
        JarLocation jarLocation = parseJarLocation(location, true);
        JarFile jarFile = new JarFile(jarLocation.jarPath.toFile());
        JarEntry entry = jarFile.getJarEntry(jarLocation.entryPath);
        if (entry == null) {
            jarFile.close();
            throw new FileNotFoundException("Jar entry not found: " + location);
        }

        InputStream in = jarFile.getInputStream(entry);
        return new FilterInputStream(in) {
            @Override
            public void close() throws IOException {
                super.close();
                jarFile.close();
            }
        };
    }

    private static InputStream openFile(String location) throws IOException {
        Path path = parseAbsoluteFilePath(location, true);
        return Files.newInputStream(path);
    }

    private static InputStream openPath(String location) throws IOException {
        return openPath(Paths.get(location));
    }

    private static InputStream openPath(Path path) throws IOException {
        return Files.newInputStream(path);
    }

    public static boolean exists(String location) {
        Objects.requireNonNull(location, "location");

        LocationType type = detectType(location);
        if (type == LocationType.CLASSPATH) {
            return existsClasspath(location);
        }

        if (type == LocationType.JAR) {
            return existsJar(location);
        }

        if (type == LocationType.FILE) {
            return existsFile(location);
        }

        return Files.exists(Paths.get(location));
    }

    public static String resolve(String baseLocation, String fileName) {
        Objects.requireNonNull(baseLocation, "baseLocation");
        Objects.requireNonNull(fileName, "fileName");

        LocationType type = detectType(baseLocation);
        if (type == LocationType.CLASSPATH || type == LocationType.JAR) {
            String separator = baseLocation.endsWith("/") ? "" : "/";
            return baseLocation + separator + fileName;
        }

        if (type == LocationType.FILE) {
            Path path = parseAbsoluteFilePath(baseLocation, true);
            return path.resolve(fileName).toUri().toString();
        }

        return Paths.get(baseLocation).resolve(fileName).toString();
    }

    private static boolean existsClasspath(String location) {
        String path = classpathPath(location);
        ClassLoader cl = contextClassLoader();
        return cl.getResource(path) != null;
    }

    private static boolean existsJar(String location) {
        JarLocation jarLocation = parseJarLocation(location, false);
        if (jarLocation == null || !Files.exists(jarLocation.jarPath)) {
            return false;
        }

        try (JarFile jar = new JarFile(jarLocation.jarPath.toFile())) {
            return jar.getJarEntry(jarLocation.entryPath) != null;
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean existsFile(String location) {
        Path path = parseAbsoluteFilePath(location, false);
        if (path == null) {
            return false;
        }
        return Files.exists(path);
    }

    public static String removeExtension(String location) {
        if (location == null) {
            return null;
        }

        int slash = Math.max(
                location.lastIndexOf('/'),
                location.lastIndexOf('\\'));

        int dot = location.lastIndexOf('.');

        if (dot == -1 || dot < slash) {
            return location;
        }

        return location.substring(0, dot);
    }

    public static List<String> readAllLines(String location) throws IOException {
        return readAllLines(location, StandardCharsets.UTF_8);
    }

    public static List<String> readAllLines(String location, Charset charset) throws IOException {
        try (BufferedReader reader = newBufferedReader(location, charset)) {
            return reader.lines().collect(Collectors.toList());
        }
    }

    public static BufferedReader newBufferedReader(String location) throws IOException {
        return newBufferedReader(location, StandardCharsets.UTF_8);
    }

    public static BufferedReader newBufferedReader(String location, Charset charset) throws IOException {
        Objects.requireNonNull(charset, "charset");
        return new BufferedReader(new InputStreamReader(open(location), charset));
    }

    public static long getFileSize(String location) throws IOException {
        Objects.requireNonNull(location, "location");

        LocationType type = detectType(location);
        if (type == LocationType.CLASSPATH) {
            return getClasspathFileSize(location);
        }

        if (type == LocationType.JAR) {
            return getJarFileSize(location);
        }

        if (type == LocationType.FILE) {
            return getFileFileSize(location);
        }

        return Files.size(Paths.get(location));
    }

    private static long getClasspathFileSize(String location) throws IOException {
        String path = classpathPath(location);
        URL url = contextClassLoader().getResource(path);
        if (url == null) {
            throw new FileNotFoundException("Classpath resource not found: " + location);
        }

        if ("file".equals(url.getProtocol())) {
            try {
                return Files.size(Paths.get(url.toURI()));
            } catch (IllegalArgumentException | URISyntaxException e) {
                return measureClasspathFileSize(location);
            }
        }

        if ("jar".equals(url.getProtocol())) {
            JarURLConnection connection = (JarURLConnection) url.openConnection();
            JarEntry entry = connection.getJarEntry();
            if (entry == null) {
                throw new FileNotFoundException(location);
            }
            return entry.getSize();
        }

        return measureClasspathFileSize(location);
    }

    private static long measureClasspathFileSize(String location) throws IOException {
        try (InputStream in = openClasspath(location)) {
            // NOTE: サイズ取得の正確性を優先し、ストリーム終端まで全て読み込んで計測する。
            byte[] buffer = new byte[8192];
            long total = 0;
            int read;
            while ((read = in.read(buffer)) != -1) {
                total += read;
            }
            return total;
        }
    }

    private static long getJarFileSize(String location) throws IOException {
        JarLocation jarLocation = parseJarLocation(location, true);
        try (JarFile jar = new JarFile(jarLocation.jarPath.toFile())) {
            JarEntry entry = jar.getJarEntry(jarLocation.entryPath);
            if (entry == null) {
                throw new FileNotFoundException(location);
            }
            return entry.getSize();
        }
    }

    private static long getFileFileSize(String location) throws IOException {
        Path path = parseAbsoluteFilePath(location, true);
        return Files.size(path);
    }

    private static LocationType detectType(String location) {
        if (location.startsWith(CLASSPATH_PREFIX)) {
            return LocationType.CLASSPATH;
        }
        if (location.startsWith(JAR_PREFIX)) {
            return LocationType.JAR;
        }
        if (location.startsWith(FILE_PREFIX)) {
            return LocationType.FILE;
        }
        return LocationType.PATH;
    }

    private static String classpathPath(String location) {
        String path = location.substring(CLASSPATH_PREFIX.length());
        if (path.startsWith("/")) {
            return path.substring(1);
        }
        return path;
    }

    private static Path parseAbsoluteFilePath(String location, boolean strict) {
        try {
            Path path = Paths.get(URI.create(location));
            if (!path.isAbsolute()) {
                if (strict) {
                    throw new IllegalArgumentException("file: requires absolute URI: " + location);
                }
                return null;
            }
            return path;
        } catch (IllegalArgumentException e) {
            if (strict) {
                throw new IllegalArgumentException("Invalid file URI: " + location, e);
            }
            return null;
        }
    }

    private static JarLocation parseJarLocation(String location, boolean strict) {
        String spec = location.substring(JAR_PREFIX.length());
        int sep = spec.indexOf("!/");
        if (sep < 0) {
            if (strict) {
                throw new IllegalArgumentException("Invalid jar location: " + location);
            }
            return null;
        }
        String jarPathSpec = spec.substring(0, sep);
        if (!jarPathSpec.startsWith(FILE_PREFIX)) {
            if (strict) {
                throw new IllegalArgumentException("jar: requires file URI: " + location);
            }
            return null;
        }
        Path jarPath = Paths.get(URI.create(jarPathSpec));
        String entryPath = spec.substring(sep + 2);
        return new JarLocation(jarPath, entryPath);
    }

    private static ClassLoader contextClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    private static ByteBuffer mapReadOnly(Path path) throws IOException {
        try (FileChannel ch = FileChannel.open(path, StandardOpenOption.READ)) {
            return littleEndian(ch.map(FileChannel.MapMode.READ_ONLY, 0, ch.size()));
        }
    }

    private static ByteBuffer littleEndian(ByteBuffer buffer) {
        return buffer.order(ByteOrder.LITTLE_ENDIAN);
    }
}
