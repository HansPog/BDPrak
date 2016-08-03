package org.bdprak.processing;

import org.apache.spark.api.java.function.FlatMapFunction;
import scala.Tuple2;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * maps tupels of frameID and set of faces to tupels:  (faceID,1)
 * used for frame count per face analysis
 */
public class FrameToFaceCountFlatMap implements FlatMapFunction<Tuple2<Integer, HashSet<Integer>>,Tuple2<Integer, Integer>> {

    @Override
    public Iterable<Tuple2<Integer, Integer>> call(Tuple2<Integer, HashSet<Integer>> t) throws Exception {
        ArrayList<Tuple2<Integer, Integer>> result = new ArrayList<>();

        for(Integer i: t._2){
            result.add(new Tuple2<>(i,1));
        }

        return result;
    }
}
