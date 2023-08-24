package me.fengyj.common.utils;

import me.fengyj.common.exceptions.ErrorSeverity;
import me.fengyj.common.exceptions.GeneralException;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IOUtils {

    private static final String SYSTEM_TEMP_DIR_PROP_NAME = "java.io.tmpdir";

    private static final String TEMP_DIR = System.getProperty(SYSTEM_TEMP_DIR_PROP_NAME)
            .endsWith(File.separator)
            ? System.getProperty(SYSTEM_TEMP_DIR_PROP_NAME)
            : System.getProperty(SYSTEM_TEMP_DIR_PROP_NAME) + File.separator;

    private IOUtils() {
        // empty
    }

    public static byte[] readBufferAsBytes(ByteBuffer buffer) {

        buffer.rewind();
        var bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return bytes;
    }

    public static byte[] readStreamAsBytes(InputStream inputStream) throws IOException {

        try (var result = new ByteArrayOutputStream()) {

            var buffer = new byte[1024 * 32];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                result.write(buffer, 0, length);
                result.flush();
            }
            result.flush();
            return result.toByteArray();
        }
    }

    public static byte[] readStreamAsBytes(InputStream inputStream, int maxLengthToRead) throws IOException {

        try (var result = new ByteArrayOutputStream()) {

            var buffer = new byte[1024 * 32];

            int lengthHasRead = 0;
            while (true) {
                int lengthToRead = Math.min(buffer.length, maxLengthToRead - lengthHasRead);
                if (lengthToRead == 0) break;
                int length = inputStream.read(buffer, 0, lengthToRead);
                if (length == -1) break;
                result.write(buffer, 0, length);
                result.flush();
                lengthHasRead += length;
            }
            result.flush();
            return result.toByteArray();
        }
    }

    public static String readStreamAsText(InputStream inputStream) throws IOException {

        var bytes = readStreamAsBytes(inputStream);
        return StringUtils.fromBytes(bytes);
    }

    public static byte[] readResourceAsBytes(String fileName) throws IOException {

        try (var is = ClassLoader.getSystemResource(fileName).openStream()) {
            return readStreamAsBytes(is);
        }
    }

    public static String readResourceAsText(String fileName) throws IOException {

        try (var is = ClassLoader.getSystemResource(fileName).openStream()) {
            return readStreamAsText(is);
        }
    }

    public static List<String> readResourceAsLines(String fileName) throws IOException {

        try (var is = ClassLoader.getSystemResource(fileName).openStream();
             var reader = new InputStreamReader(is);
             var br = new BufferedReader(reader)) {
            return br.lines().toList();
        }
    }

    public static void readResourceAsLines(String fileName, Consumer<Stream<String>> linesConsumer) throws IOException {

        try (var is = ClassLoader.getSystemResource(fileName).openStream();
             var reader = new InputStreamReader(is);
             var br = new BufferedReader(reader)) {
            linesConsumer.accept(br.lines());
        }
    }

    public static byte[] readFileAsBytes(Path filePath) throws IOException {

        try (var is = new FileInputStream(filePath.toFile())) {
            return readStreamAsBytes(is);
        }
    }

    public static String readFileAsText(Path filePath) throws IOException {

        try (var is = new FileInputStream(filePath.toFile())) {
            return readStreamAsText(is);
        }
    }

    public static List<String> readFileAsLines(Path filePath) throws IOException {

        try (var is = new FileInputStream(filePath.toFile());
             var reader = new InputStreamReader(is);
             var br = new BufferedReader(reader)) {
            return br.lines().toList();
        }
    }

    public static void readFileAsLines(Path filePath, Consumer<Stream<String>> linesConsumer) throws IOException {

        try (var is = new FileInputStream(filePath.toFile());
             var reader = new InputStreamReader(is);
             var br = new BufferedReader(reader)) {
            linesConsumer.accept(br.lines());
        }
    }

    public static void writeFile(Path filePath, byte[] bytes) throws IOException {

        try (var os = new FileOutputStream(filePath.toFile())) {
            os.write(bytes);
            os.flush();
        }
    }

    public static void writeFile(Path filePath, String text) throws IOException {

        try (var os = new FileOutputStream(filePath.toFile())) {
            os.write(StringUtils.getBytes(text));
            os.flush();
        }
    }

    /**
     * Delete file or directory
     *
     * @param filePath the path of the file or directory
     */
    public static void deleteFile(Path filePath) {

        var f = filePath.toFile();
        if (!f.exists()) return;

        if (f.isDirectory()) {
            var items = f.listFiles();
            if (items != null) {
                for (File c : items)
                    deleteFile(c.toPath());
            }
        }
        if (!f.delete()) {
            throw GeneralException.create(
                    ErrorSeverity.Error,
                    String.format("Delete %s failed.", f.getPath()),
                    null);
        }
    }

    public static List<Path> listFiles(Path folder, int maxDeep) {

        try (var stream = Files.walk(folder, maxDeep)) {
            return stream.sorted(Comparator.comparing(Path::toString)).collect(Collectors.toList());
        } catch (IOException ex) {
            throw new GeneralException(ErrorSeverity.Error, String.format("Cannot list files in %s", folder.toString()), ex);
        }
    }

    public static String getSystemTempFolder() {

        return TEMP_DIR;
    }

    public static String createTmpFolder() {

        var folder = String.format("%s%s%s", IOUtils.getSystemTempFolder(), UUID.randomUUID(), File.separator);
        var theDir = new File(folder);
        if (!theDir.exists()) {
            theDir.mkdirs();
        }
        return folder;
    }

    public static String createTmpFile(String ext) {

        return String.format("%s%s.%s", IOUtils.getSystemTempFolder(), UUID.randomUUID(), ext);
    }

    public static String createTmpFile() {

        return createTmpFile("tmp");
    }

    public static String createTmpFileInTmpFolder(String folder) {

        return createTmpFileInTmpFolder(folder, "tmp");
    }

    public static String createTmpFileInTmpFolder(String folder, String ext) {

        return String.format("%s%s.%s", folder, UUID.randomUUID(), ext);
    }
}
