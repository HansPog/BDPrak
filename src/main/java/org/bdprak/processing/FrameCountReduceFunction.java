package org.bdprak.processing;

import org.apache.spark.api.java.function.Function2;

/**
 * sums up tupels (faceID,1) to (faceID, count)
 * used for frame count per face analysis
 */
public class FrameCountReduceFunction implements Function2<Integer, Integer, Integer> {

    @Override
    public Integer call(Integer i1, Integer i2) throws Exception {

        return i1 + i2;
    }
}
