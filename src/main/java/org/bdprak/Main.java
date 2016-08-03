package org.bdprak;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.bdprak.input.FacePreprocessing;
import org.bdprak.processing.FaceRecBatchProcess;
import org.bdprak.training.ModelTrainer;
import org.json.JSONObject;
import org.opencv.core.Core;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * main for testing different program parts
 * (for gui use frontend.Frontend)
 */
public class Main {
    static String hdfsRootStr = "hdfs://localhost:54310/";
    static Path modelFile = new Path("hdfs://localhost:54310/trainingDataWillElton.xml");

    static File tainingFolderWill = new File("/home/bduser/IdeaProjects/bdprak/will");
    static File tainingFolderElton = new File("/home/bduser/IdeaProjects/bdprak/elton");

    static File tainingFolderWillRaw = new File("/home/bduser/IdeaProjects/bdprak/willRaw");
    static File tainingFolderEltonRaw = new File("/home/bduser/IdeaProjects/bdprak/eltonRaw");


    public static void preprocess(){



        FacePreprocessing fp = new FacePreprocessing(0.25,0.25,200,200);

        fp.processFolder(tainingFolderEltonRaw.getAbsolutePath(), tainingFolderElton.getAbsolutePath());
        fp.processFolder(tainingFolderWillRaw.getAbsolutePath(), tainingFolderWill.getAbsolutePath());

    }

    public static void learn() throws IOException, URISyntaxException {
        //learn model

        List< File > imgFolderPaths = new ArrayList<>();
        imgFolderPaths.add(tainingFolderWill);
        imgFolderPaths.add(tainingFolderElton);
        List<Integer> labels = new ArrayList<>();
        labels.add(1);
        labels.add(2);
        String algortihm = ModelTrainer.LBPH;

        ModelTrainer mt = new ModelTrainer(hdfsRootStr, modelFile, imgFolderPaths, labels,  algortihm, false);

    }

    public static void  distribute(){



        //save model to local disk on all workers

        SparkConf sparkConf = new SparkConf().setAppName("JavaSpark").setMaster("local");
        JavaSparkContext jsc = new JavaSparkContext(sparkConf);

        JavaRDD<String> files = jsc.textFile(modelFile.toString());
        JavaRDD<String> prt = files.repartition(1); //TODO: Anzahl worker hier eintragen ???
        prt.setName("test");
        prt.saveAsTextFile("file:///home/bduser/model.xml");

        jsc.stop();
    }

    public static void recognize() throws IOException, URISyntaxException {

        String localModelFile = "/home/bduser/model.xml/part-00000";
        System.out.println(localModelFile);

        //FaceRecBatchProcess frbp = new FaceRecBatchProcess(hdfsRootStr,  "hdfs://localhost:54310/testLoadFolder",localModelFile);
        FaceRecBatchProcess frbp = new FaceRecBatchProcess(hdfsRootStr,  "hdfs://localhost:54310/testLoadVideo",localModelFile);
        frbp.processFolder();
    }

    public static void main(String[] args) throws IOException, URISyntaxException {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        //preprocess();
        //learn();
        //distribute();

        //recognize();


    }
}
