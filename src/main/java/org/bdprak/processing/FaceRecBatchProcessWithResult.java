package org.bdprak.processing;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import scala.Tuple2;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * face recognition spark process with spark result calculations
 */
public class FaceRecBatchProcessWithResult {

    private String folderPath, hdfsRootPath, modelPath;

    public FaceRecBatchProcessWithResult(String hdfsRootPath, String folderPath, String modelPath){
        this.folderPath = folderPath;
        this.hdfsRootPath = hdfsRootPath;
        this.modelPath = modelPath;
    }

    public void processFolder() throws URISyntaxException, IOException {

        SparkConf sparkConf = new SparkConf().setAppName("JavaSparkFaceReccc").setMaster("local");
        JavaSparkContext jsc = new JavaSparkContext(sparkConf);

        Configuration configuration = new Configuration();
        FileSystem hdfs = FileSystem.get(new URI(hdfsRootPath), configuration);


        List<String> files = new ArrayList<>();
        RemoteIterator<LocatedFileStatus> ritr = hdfs.listFiles(new Path(folderPath), true);
        while(ritr.hasNext()){
            files.add(ritr.next().getPath().toString());
        }

        JavaRDD<Tuple2<Integer, HashSet<Integer>>> dataSet = jsc
                .parallelize(files).map(new RecognitionMapWithResult(modelPath,hdfsRootPath));

        //keep dataset for multiple result calculations
        dataSet.cache();


        // analysis: split video to 100 parts (buckets) and show how many/which faces are present in each %-bucket
        // Tuple2<frameID, framePredictions>  => Tuple2<bucketID, framePredictions> => PairRDD
        JavaPairRDD<Integer, HashSet<Integer>> buckets = JavaPairRDD.fromJavaRDD(dataSet.map(new FrameToBucketMap(100, files.size())));

        // => <bucketID, bucketPredictions> => local java map
        Map<Integer, HashSet<Integer>> bucketResults = buckets.reduceByKey(new BucketReduceFunction()).collectAsMap();

        String bucketsString = "";
        for(int i = 0; i < 100; i++){
            bucketsString += bucketResults.get(i).size();
        }
        System.out.println("Number of recognized faces per video-%");
        System.out.println(bucketsString);

        System.out.println("----");

        // analysis: how many frames show a person
        // => <faceID, 1> => <faceID, count> => map
        Map<Integer, Integer> frameCounts = JavaPairRDD.fromJavaRDD(dataSet.flatMap(new FrameToFaceCountFlatMap())).reduceByKey(new FrameCountReduceFunction()).collectAsMap();
        System.out.println("Frame count per face:");
        for(Map.Entry<Integer, Integer> e: frameCounts.entrySet()){
            System.out.println(e.getKey() + ": " + e.getValue());
        }
        System.out.println("----");

        //dataSet.count();
        jsc.stop();
    }
}
