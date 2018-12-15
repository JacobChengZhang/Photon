package org.jacobzhang.photon.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CommonUtilTest {

    @Test
    public void clampTest() {
        assertEquals(0.5, CommonUtil.clamp(0.5, 0.1, 0.7), 0.000001);
        assertEquals(0.6, CommonUtil.clamp(0.5, 0.6, 0.7), 0.000001);
        assertEquals(0.7, CommonUtil.clamp(0.9, 0.6, 0.7), 0.000001);

        try {
            CommonUtil.clamp(0.5, 0.6, 0.5);
        } catch (AssertionError ae) {
            return;
        }
        fail();
    }
}
