package org.bdprak.processing;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.opencv.core.Mat;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * face recognition spark process without spark result calculations
 */
public class FaceRecBatchProcess {

    private String folderPath, hdfsRootPath, modelPath;

    public  FaceRecBatchProcess(String hdfsRootPath, String folderPath, String modelPath){
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

        JavaRDD<String> dataSet = jsc
                .parallelize(files).map(new RecognitionMap(modelPath,hdfsRootPath));
        dataSet.count();
        jsc.stop();
    }
}
