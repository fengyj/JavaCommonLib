package me.fengyj.common.utils;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class StringUtils {

    private StringUtils() {

    }

    public static boolean isNullOrEmpty(String val) {

        return val == null || val.isEmpty();
    }

    public static boolean isNullOrWhiteSpace(String val) {

        return val == null || val.trim().isEmpty();
    }

    public static String shorten(String val, int maxLength) {

        if (val == null || val.length() <= maxLength) return val;
        if (maxLength > 500)
            return val.substring(0, maxLength - 3) + "...";
        else
            return val.substring(0, maxLength);
    }

    public static String shorten(String val, int maxLength, boolean appendDots) {

        if (val == null || val.length() <= maxLength) return val;
        if (appendDots)
            return val.substring(0, maxLength - 3) + "...";
        else
            return val.substring(0, maxLength);
    }

    public static byte[] getBytes(String val) {

        return val.getBytes(StandardCharsets.UTF_8);
    }

    public static String fromBytes(byte[] bytes) {

        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static List<String> getLines(byte[] bytes) throws IOException {

        try (var is = new ByteArrayInputStream(bytes);
             var reader = new InputStreamReader(is);
             var br = new BufferedReader(reader)) {
            return br.lines().toList();
        }
    }

    public static String checkAndFixIllegalChars(String val) {

        if (val == null || val.length() == 0) return val;
        StringBuilder sb = null;
        char current;
        int startIdx = -1;
        for (int i = 0; i < val.length(); i++) {
            current = val.charAt(i);
            if (current != 0x9 && current != 0xA && current != 0xD
                    && (current < 0x20 || current > 0xD7FF)
                    && (current < 0xE000 || current > 0xFFFD)) {

                if (sb == null) {
                    sb = new StringBuilder();
                    startIdx = 0;
                }
                if (startIdx < i)
                    sb.append(val, startIdx, i);
                startIdx = i + 1;
//                if (logger.isDebugEnabled())
//                    logger.debug(String.format("Find illegal char 0x%04x", (int) current));
            }
        }
        if (startIdx >= 0 && startIdx < val.length())
            sb.append(val, startIdx, val.length());
        return sb == null ? val : sb.toString();
    }

    public static BigDecimal toBigDecimal(String val) {

        try {

            return isNullOrWhiteSpace(val) ? null : new BigDecimal(val);

        } catch (Exception ex) {

            return null;
        }
    }

    public static String fromBigDecimal(BigDecimal bigDecimalVal) {

        return bigDecimalVal == null ? null : bigDecimalVal.toString();
    }

    public static boolean toBoolean(String val) {

        Boolean b = toNullableBoolean(val);
        if (b == null) return false;
        return b;
    }

    public static Boolean toNullableBoolean(String val) {

        if (isNullOrWhiteSpace(val)) return null;
        return "TRUE".equalsIgnoreCase(val) || "T".equalsIgnoreCase(val)
                || "1".equalsIgnoreCase(val) || "Y".equalsIgnoreCase(val);
    }

    public static Integer toInteger(String val) {

        try {

            return isNullOrWhiteSpace(val) ? null : Integer.parseInt(val);

        } catch (Exception ex) {

            return null;
        }
    }

    public static String trim(String value) {
        return value == null ? null : value.trim();
    }

    public static String getDataSize(long size) {

        var exts = new String[] { "Byte", "Bytes", "KB", "MB", "GB", "TB" };
        var extIdx = size == 0 ? 0 : 1;
        var val = size * 1.0;

        while (size > 0 && val > 1024.0 && extIdx < exts.length - 1) {

            val = val / 1024.0;
            extIdx++;
        }

        return extIdx <= 1
                ? String.format("%.0f %s", val, exts[extIdx])
                : String.format("%.2f %s", val, exts[extIdx]);
    }
}
