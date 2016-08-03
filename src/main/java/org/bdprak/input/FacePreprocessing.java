package org.bdprak.input;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import scala.Tuple2;

import java.io.File;

import static org.opencv.objdetect.Objdetect.CASCADE_SCALE_IMAGE;

/**
 * processes (rotate, resize, crop) images for training use
 * imgs have to contain exactly 1 face (with 2 eyes)
 */
public class FacePreprocessing {

    private double offset_pctw, offset_pcth;
    private int width, heigth;

    public FacePreprocessing(){
        this(0.2,0.2,70,70);
    }
    public FacePreprocessing(double ofset_pctw, double offset_pcth, int width, int heigth){
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        this.offset_pctw = ofset_pctw;
        this.offset_pcth = offset_pcth;
        this.width = width;
        this.heigth = heigth;
    }

    public void processFolder(String inFolder, String outFolder){

        File folder = new File(inFolder);
        if(!folder.exists() || !folder.isDirectory()){
            return;
        }

        File outFile = new File(outFolder);
        if(!outFile.exists()){
            outFile.mkdir();
        }

        for(File f: folder.listFiles()){
            processFile(f, outFolder);
            freeMemory();
        }
    }

    private void freeMemory(){
        System.gc();
        System.runFinalization();
    }

    public void processFile(File inFile, String outFolder){

        if(!inFile.exists() || inFile.isDirectory()){
            return;
        }

        System.out.println("reading " + inFile.getAbsolutePath());
        Mat img = Imgcodecs.imread(inFile.getAbsolutePath());
        System.out.println("done");
        Tuple2<Mat,double[]> cropImg = cropFace(img);
        if(cropImg == null){
            return;
        }

        System.out.println("getEyePos");
        Tuple2<double[],double[]> eyePositions = getEyePositions(cropImg._1);
        System.out.println("done");

        if(eyePositions == null){
            return;
        }
        //add crop offset
        eyePositions._1[0] += cropImg._2[0];
        eyePositions._1[1] += cropImg._2[1];
        eyePositions._2[0] += cropImg._2[0];
        eyePositions._2[1] += cropImg._2[1];

        System.out.println("Eye positions: " +eyePositions._1[0]+" "+eyePositions._1[1]+" "+eyePositions._2[0]+" "+eyePositions._2[1]);

        Mat croppedFace = processFace(eyePositions._1,eyePositions._2,img);

        if(croppedFace == null){
            return;
        }

        Mat greyMat = new Mat();
        Imgproc.cvtColor(croppedFace, greyMat, Imgproc.COLOR_BGR2GRAY);
        //Imgproc.equalizeHist(greyMat,greyMat);


        System.out.println("writing " +outFolder + "/crop_" + inFile.getName());
        Imgcodecs.imwrite(outFolder + "/crop_" + inFile.getName(), greyMat);
        System.out.println("done");

        cropImg._1.release();
        greyMat.release();
        img.release();
        croppedFace.release();

    }

    /**
     *
     * @param img
     * @return img cropped to face and offset
     */
    private Tuple2<Mat,double[]> cropFace(Mat img){
        MatOfRect faceDetections = new MatOfRect();
        //CascadeClassifier faceDetector = new CascadeClassifier(SparkCVSerial.class.getResource("/lbpcascade_frontalface.xml").getPath());
        CascadeClassifier faceDetector = new CascadeClassifier(CascadeClassifier.class.getResource("/haarcascade_frontalface_default.xml").getPath());

        //faceDetector.detectMultiScale(img, faceDetections);
        faceDetector.detectMultiScale(img, faceDetections,1.3, 5, 0 | CASCADE_SCALE_IMAGE,new Size(300,300),new Size(10000,10000));
        Rect[] faces = faceDetections.toArray();

        if(faces.length != 1){
            System.out.println("wrong number of faces: "+faces.length);
            return null;
        }

        double[] offset = new double[2];

        offset[0] = faces[0].x;
        offset[1] = faces[0].y;

        return new Tuple2<>(new Mat(img, faces[0]), offset);
    }

    private Tuple2<double[],double[]> getEyePositions(Mat img){
        MatOfRect eyeDetections = new MatOfRect();
        CascadeClassifier eyeDetector = new CascadeClassifier(CascadeClassifier.class.getResource("/haarcascade_eye.xml").getPath());

        eyeDetector.detectMultiScale(img, eyeDetections,1.3, 5, 0 | CASCADE_SCALE_IMAGE,new Size(30,30),new Size(350,350));
        if(eyeDetections.toArray().length > 2){
            eyeDetections = new MatOfRect();
            eyeDetector.detectMultiScale(img, eyeDetections,1.5, 8, 0 | CASCADE_SCALE_IMAGE,new Size(30,30),new Size(350,350));
        }else if(eyeDetections.toArray().length < 2){
            eyeDetections = new MatOfRect();
            eyeDetector.detectMultiScale(img, eyeDetections,1.1, 3, 0 | CASCADE_SCALE_IMAGE,new Size(30,30),new Size(350,350));
        }
        //eyeDetector.detectMultiScale(img, eyeDetections,1.3, 2, 0 | CASCADE_SCALE_IMAGE,new Size(30,30),new Size(350,350));
        //eyeDetector.detectMultiScale2(img, eyeDetections, );

        Rect[] detectedEyeRects = eyeDetections.toArray();

        if(detectedEyeRects.length != 2){
            System.out.println("Wrong number of eyes: " +detectedEyeRects.length);
            return null;
        }

        double[][] eyeCenters = new double[2][2];

        for (int i = 0; i < detectedEyeRects.length; i++) {
            eyeCenters[i][0] = detectedEyeRects[i].x + detectedEyeRects[i].width *0.5;
            eyeCenters[i][1] = detectedEyeRects[i].y + detectedEyeRects[i].height *0.5;
        }

        //make shure eye0 is left eye
        if(eyeCenters[1][0] < eyeCenters[0][0]){
            double tempX = eyeCenters[1][0];
            double tempY = eyeCenters[1][1];

            eyeCenters[1][0] = eyeCenters[0][0];
            eyeCenters[1][1] = eyeCenters[0][1];
            eyeCenters[0][0] = tempX;
            eyeCenters[0][1] = tempY;
        }

        return new Tuple2<>(eyeCenters[0],eyeCenters[1]);
    }


    private Mat processFace(double[] eye_left, double[] eye_right, Mat img) {

        int[] dest_sz = {width,heigth};
        double offset_h = Math.floor(offset_pcth * dest_sz[0]);

        double offset_v = Math.floor(offset_pctw * dest_sz[1]);

        double[] eye_direction = {eye_right[0] - eye_left[0], eye_right[1] - eye_left[1]};
        double rotation = Math.atan2(eye_direction[1], eye_direction[0]);
        double dist = Math.sqrt(Math.pow(eye_direction[0],2) + Math.pow(eye_direction[1],2));
        double reference = dest_sz[0] - 2.0 * offset_h;
        double scale = dist / reference;



	/*image angle center*/

        System.out.println("rotation: "+ rotation+" leftEye: "+eye_left);
        Mat rotatedImg = scaleRotateTranslate(img, rotation, eye_left);



        double crop_xy[] = {eye_left[0] - scale * offset_h, eye_left[1] - scale * offset_v};



        double crop_size[] = {dest_sz[0] * scale, dest_sz[1] * scale};

        Rect cropRect = new Rect((int)crop_xy[0], (int)crop_xy[1], (int)( crop_size[0]), (int)(crop_size[1]));

        System.out.println(img.size().width + " "+img.size().height);
        System.out.println(rotatedImg.size().width + " "+rotatedImg.size().height);
        System.out.println(cropRect);

        if(cropRect.x+cropRect.width > rotatedImg.width() ||
                cropRect.y+cropRect.height > rotatedImg.height()){
            System.out.println("crop bigger than original");
            return null;
        }
        Mat croppedImg = new Mat(rotatedImg, cropRect);

        Mat resizdImg = new Mat();
        Size sz = new Size(dest_sz[0],dest_sz[1]);
        Imgproc.resize( croppedImg, resizdImg, sz );

        return resizdImg;

    }


    private Mat scaleRotateTranslate(Mat img, double rotAngle, double[] leftEyeCoords){

        Mat outMat = new Mat(img.width(),img.height(),img.type());

        Mat rotationMatrix = Imgproc.getRotationMatrix2D(new Point(leftEyeCoords[0], leftEyeCoords[1]), Math.toDegrees(rotAngle), 1);
        Imgproc.warpAffine(img, outMat, rotationMatrix, outMat.size());

        return outMat;
    }
}
