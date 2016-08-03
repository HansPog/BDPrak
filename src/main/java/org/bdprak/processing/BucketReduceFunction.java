package org.bdprak.processing;

import org.apache.spark.api.java.function.Function2;

import java.util.HashSet;

/**
 * takes tupels of bucketID and frameResults and  calculates union of frameResults (found faces set)
 * used for bucket/videopart analysis
 */
public class BucketReduceFunction implements Function2<HashSet<Integer>,HashSet<Integer>,HashSet<Integer>> {

    @Override
    public HashSet<Integer> call(HashSet<Integer> v1, HashSet<Integer> v2) throws Exception {

        HashSet<Integer> result = new HashSet<>();
        result.addAll(v1);
        result.addAll(v2);

        return result;
    }
}
