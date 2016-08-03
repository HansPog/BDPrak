package org.bdprak.processing;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.spark.api.java.function.Function;
import org.json.JSONArray;
import org.json.JSONObject;
import org.opencv.core.*;
import org.opencv.face.Face;
import org.opencv.face.FaceRecognizer;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import scala.Tuple2;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;

import static org.opencv.objdetect.Objdetect.CASCADE_SCALE_IMAGE;

/**
 * map function which executes face recognition for each given hdfs path (maps to tuple of frameID and HashSet of faceIDs found)
 */
public class RecognitionMapWithResult implements Function<String, Tuple2<Integer, HashSet<Integer>>> {
    public static void showResult(Mat img) {

        //Imgproc.resize(img, img, new Size(640, 480));
        MatOfByte matOfByte = new MatOfByte();
        Imgcodecs.imencode(".jpg", img, matOfByte);
        byte[] byteArray = matOfByte.toArray();
        BufferedImage bufImage = null;
        try {
            InputStream in = new ByteArrayInputStream(byteArray);
            bufImage = ImageIO.read(in);
            JFrame frame = new JFrame();
            frame.getContentPane().add(new JLabel(new ImageIcon(bufImage)));
            frame.pack();
            frame.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public RecognitionMapWithResult(String modelPath, String hdfsRootPath){
        this.modelPath = modelPath;
        this.hdfsRootPath = hdfsRootPath;

        System.out.println(modelPath);
    }


    private String modelPath,hdfsRootPath;
    public int width = 200;
    public int height = 200;

    @Override
    public Tuple2<Integer, HashSet<Integer>> call(String hdfsFile) throws Exception {

        System.out.println(hdfsFile);

        //load image
        Path path = new Path(hdfsFile);
        FileSystem fs = FileSystem.get(new URI(hdfsRootPath),new Configuration());

        BufferedImage bufImage = null;
        FSDataInputStream in =  fs.open(path);
        ObjectInputStream ois = new ObjectInputStream(in);


        byte[] bytes = (byte[]) ois.readObject();//IOUtils.toByteArray(bis);

        Mat img = Imgcodecs.imdecode(new MatOfByte(bytes), Imgcodecs.CV_LOAD_IMAGE_UNCHANGED);
        //showResult(img);
        Mat greyMat = Imgcodecs.imdecode(new MatOfByte(bytes), 0/*Imgcodecs.CV_LOAD_IMAGE_UNCHANGED*/);
        //Imgproc.equalizeHist(greyMat,greyMat);
        //showResult(greyMat);
        //Mat greyMat = new Mat();
        //Imgproc.cvtColor(img, greyMat, Imgproc.COLOR_BGR2GRAY);

        //detect faces
        MatOfRect faceDetections = new MatOfRect();
        //CascadeClassifier faceDetector = new CascadeClassifier(SparkCVSerial.class.getResource(/*"/haarcascade_frontalface_default.xml"*/ "/lbpcascade_frontalface.xml").getPath());
        //faceDetector.detectMultiScale(img, faceDetections);
        CascadeClassifier faceDetector = new CascadeClassifier(CascadeClassifier.class.getResource("/haarcascade_frontalface_default.xml").getPath());
        faceDetector.detectMultiScale(img, faceDetections,1.3, 5, 0 | CASCADE_SCALE_IMAGE,new Size(100,100),new Size(10000,10000));
        //faceDetector.detectMultiScale(img, faceDetections);
        Rect[] faces = faceDetections.toArray();

        //load face recognizer from xml (local path)

        //FaceRecognizer fr = Face.createLBPHFaceRecognizer();
        FaceRecognizer fr = Face.createLBPHFaceRecognizer();
        fr.load(modelPath);

        //System.out.println("process frame " + faces.length);

        JSONArray frame = new JSONArray();
        HashSet<Integer> predictions = new HashSet<>();


        for(Rect faceRect: faces){

            JSONObject face = new JSONObject();

            face.put("x", faceRect.x);
            face.put("y", faceRect.y);
            face.put("height", faceRect.height);
            face.put("width", faceRect.width);

            Mat faceMat =  new Mat(greyMat, faceRect);


            Mat faceResized = new Mat();
            Imgproc.resize( faceMat, faceResized, new Size(width, height), 1.0, 1.0, Imgproc.INTER_CUBIC);

            //recognize faces
            int prediction = fr.predict(faceResized);



            //write output
            face.put("prediction", prediction);
            predictions.add(prediction);
            //System.out.println("Result: " + prediction);

            frame.put(face);
        }


        System.out.println(frame);

        writeOutput(hdfsFile, frame);

        return new Tuple2<>(getFrameNumber(hdfsFile), predictions);
    }


    /**
     * extracts frame number from hdfs file name
     * @param hdfsPath f.eg. hdfs://.../1.jpg
     * @return framenumber
     */
    protected Integer getFrameNumber(String hdfsPath){
        Integer result = null;

        String numString = hdfsPath.substring(hdfsPath.lastIndexOf("/") +1, hdfsPath.lastIndexOf("."));
        result = Integer.parseInt(numString);

        return result;
    }

    private void writeOutput(String hdfsFile, JSONArray json) throws URISyntaxException, IOException {

        String folder = hdfsFile.substring(0, hdfsFile.lastIndexOf("/"));
        String parentFolder = folder.substring(0, folder.lastIndexOf("/"));

        //output path: hdfs://any/path/recResult/originalFolderName/1.jpg.json
        String outputPath = parentFolder + "/recResult" + folder.substring(folder.lastIndexOf("/")) + hdfsFile.substring( hdfsFile.lastIndexOf("/")) + ".json";

        Path path = new Path(outputPath);
        FileSystem fs = FileSystem.get(new URI(hdfsRootPath),new Configuration());


        BufferedWriter br =new BufferedWriter(new  OutputStreamWriter(fs.create(path,true)));
        br.write(json.toString());
        br.close();

    }
}
