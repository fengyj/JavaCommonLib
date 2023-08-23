package me.fengyj.utils;

import me.fengyj.exception.ErrorSeverity;
import me.fengyj.exception.GeneralException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IOUtils {

    private static final String SYSTEM_TEMP_DIR_PROP_NAME = "java.io.tmpdir";

    private static final String TEMP_DIR = System.getProperty(SYSTEM_TEMP_DIR_PROP_NAME)
            .endsWith(File.separator)
            ? System.getProperty(SYSTEM_TEMP_DIR_PROP_NAME)
            : System.getProperty(SYSTEM_TEMP_DIR_PROP_NAME) + File.separator;

    private IOUtils() {

    }

    public static byte[] toBytes(ByteBuffer buffer) {

        buffer.rewind();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return bytes;
    }

    public static byte[] toBytes(InputStream inputStream) throws IOException {

        try (ByteArrayOutputStream result = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024 * 32];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                result.write(buffer, 0, length);
                result.flush();
            }
            result.flush();
            return result.toByteArray();
        }
    }

    public static byte[] toBytes(InputStream inputStream, int maxLengthToRead) throws IOException {

        try (ByteArrayOutputStream result = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024 * 32];

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

    public static List<Path> listFiles(Path folder, int maxDeep) {

        try(Stream<Path> stream = Files.walk(folder, maxDeep)) {
            return stream.sorted(Comparator.comparing(Path::toString)).collect(Collectors.toList());
        }
        catch (IOException ex) {
            throw new GeneralException(ErrorSeverity.Error, String.format("Cannot list files in %s", folder.toString()), ex);
        }
    }

    public static String getSystemTempFolder() {

        return TEMP_DIR;
    }

    public static String createTmpFolder() {

        String folder = String.format("%s%s%s", IOUtils.getSystemTempFolder(), UUID.randomUUID(), File.separator);
        File theDir = new File(folder);
        if (!theDir.exists()){
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

    public static void delete(File f) {

        if (!f.exists()) return;

        if (f.isDirectory()) {
            File[] items = f.listFiles();
            if (items != null) {
                for (File c : items)
                    delete(c);
            }
        }
        if (!f.delete()) {
            throw GeneralException.create(
                ErrorSeverity.Error,
                String.format("Delete %s failed.", f.getPath()),
                null);
        }
    }
}
