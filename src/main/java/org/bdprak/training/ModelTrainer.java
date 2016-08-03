package org.bdprak.training;


import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.opencv.core.*;
import org.opencv.face.*;
import org.opencv.imgcodecs.Imgcodecs;
import scala.Tuple2;

import static org.opencv.face.Face.createFisherFaceRecognizer;
import static org.opencv.imgcodecs.Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE;

/**
 * creates model for face recognition
 */
public class ModelTrainer {


    public static final String EIGEN = "EIGEN";
    public static final String FISHER = "FISHER";
    public static final String LBPH = "LBPH";


    /**
     *
     * @param modelPath io path for model (xml save)
     * @param algortihm algorithm to use, see finals
     * @param update use update instead of train (keep old state), only works with LBPH
     */
    public ModelTrainer(String hdfsRootPath, Path modelPath, List<File> imgFolderPaths, List<Integer> labels, String algortihm, boolean update) throws URISyntaxException, IOException {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);


        Configuration configuration = new Configuration();
        FileSystem hdfs = FileSystem.get(new URI(hdfsRootPath), configuration);

        FaceRecognizer model  = null;
        if(!algortihm.equals(LBPH) || !update){

            //create new empty model
            switch (algortihm){
                case EIGEN:
                    model = Face.createEigenFaceRecognizer();
                    break;
                case FISHER:
                    model = Face.createFisherFaceRecognizer();
                    break;
                case LBPH:
                    model = Face.createLBPHFaceRecognizer(); //TODO check if parameters needed
                    break;
                default:
                    return;
            }


            //train
            for(int i = 0; i < imgFolderPaths.size(); i++){
                System.out.println(imgFolderPaths.get(i));
                System.out.println(labels.get(i));
            }
            Tuple2<List<Mat>,Mat> trainingLists = getTrainingLists(imgFolderPaths,labels);

            model.train(trainingLists._1, trainingLists._2);

        }else{

            //load LBPH model from xml

            model = Face.createLBPHFaceRecognizer();

            File temp = File.createTempFile("oldModel", ".xml");
            hdfs.copyToLocalFile(false, modelPath, new Path(temp.getAbsolutePath()), true);

            model.load(temp.getAbsolutePath());

            temp.delete();

            //update
            Tuple2<List<Mat>,Mat> trainingLists = getTrainingLists(imgFolderPaths,labels);
            model.update(trainingLists._1, trainingLists._2);
        }

        //save new model
        File temp = File.createTempFile("newModel", ".xml");
        model.save(temp.getAbsolutePath());
        hdfs.copyFromLocalFile(true, true, new Path(temp.getAbsolutePath()), modelPath);

    }

    /**
     *
     * @param imgFolderFiles list of folders
     * @param labels list of labels for folders
     * @return list of Mat of images and mat of labels for Mats
     */
    private Tuple2<List<Mat>,Mat> getTrainingLists(List<File> imgFolderFiles, List<Integer> labels){
        List<Mat> images = new ArrayList<>();
        List<Integer> imgLabels = new ArrayList<>();

        for(int i = 0; i < imgFolderFiles.size(); i++){
            File[] imgFiles = imgFolderFiles.get(i).listFiles();
            for(File imgFile: imgFiles){

                images.add(Imgcodecs.imread(imgFile.getAbsolutePath(), CV_LOAD_IMAGE_GRAYSCALE));
                imgLabels.add(labels.get(i));
            }
        }

        MatOfInt mat = new MatOfInt();
        mat.fromList(imgLabels);

        return new Tuple2<>(images,mat);
    }
}
