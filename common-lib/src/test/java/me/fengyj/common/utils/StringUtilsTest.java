package me.fengyj.common.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class StringUtilsTest {

    @Test
    public void test_get_data_size() {

        long size = 9;
        var exp = "9 Bytes";
        var act = StringUtils.getDataSize(size);

        Assertions.assertEquals(exp, act);

        size = 1025;
        exp = "1.00 KB";
        act = StringUtils.getDataSize(size);

        Assertions.assertEquals(exp, act);

        size = 0;
        exp = "0 Byte";
        act = StringUtils.getDataSize(size);

        Assertions.assertEquals(exp, act);

        size = 2345012;
        exp = "2.24 MB";
        act = StringUtils.getDataSize(size);

        Assertions.assertEquals(exp, act);

        size = 43242345012L;
        exp = "40.27 GB";
        act = StringUtils.getDataSize(size);

        Assertions.assertEquals(exp, act);

        size = 901243242345012L;
        exp = "819.68 TB";
        act = StringUtils.getDataSize(size);

        Assertions.assertEquals(exp, act);
    }
}
