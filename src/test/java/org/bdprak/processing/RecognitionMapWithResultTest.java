package org.bdprak.processing;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Created by bduser on 25.07.16.
 */
public class RecognitionMapWithResultTest {

    @Test
    public void testGetFrameNumber(){
        RecognitionMapWithResult rm = new RecognitionMapWithResult("", "");

        assertEquals(Integer.valueOf(123), rm.getFrameNumber("hdfs://localhost:534234/dkjsgk2132/13223fj/123.jpg"));
    }
}
