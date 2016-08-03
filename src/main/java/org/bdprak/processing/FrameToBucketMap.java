package org.bdprak.processing;

import org.apache.spark.api.java.function.Function;
import scala.Tuple2;

import java.util.HashSet;

/**
 * maps tuple of frameID and frameResult to tupel of bucketID and frameResult
 * used for bucket/videopart analysis
 */
public class FrameToBucketMap implements Function<Tuple2<Integer, HashSet<Integer>>,Tuple2<Integer, HashSet<Integer>>> {

    private int numBuckets, numFrames, bucketSize;

    public FrameToBucketMap(int numBuckets, int numFrames){
        this.numBuckets = numBuckets;
        this.numFrames = numFrames;
        bucketSize = numFrames/numBuckets;
    }

    @Override
    public Tuple2<Integer, HashSet<Integer>> call(Tuple2<Integer, HashSet<Integer>> t) throws Exception {

        return new Tuple2<>(getBucketNum(t._1),t._2);
    }

    /**
     *
     * @param frameNum 1-N
     * @return bucketnum 0-M
     */
    private int getBucketNum(int frameNum){
        int result = (int) ((frameNum-1)/bucketSize);

        //leftover is added to last bucket
        if(result > numFrames-1){
            return numFrames-1;
        }
        return result;
    }
}
