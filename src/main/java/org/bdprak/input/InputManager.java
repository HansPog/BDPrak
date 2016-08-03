package org.bdprak.input;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.util.Progressable;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;
import scala.Tuple2;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.opencv.imgproc.Imgproc.COLOR_BGR2GRAY;
import static org.opencv.imgproc.Imgproc.GaussianBlur;
import static org.opencv.imgproc.Imgproc.cvtColor;

/**
 * saves input files to hdfs
 * creates Tuple2<int frameCount, byte[] frameImg>
 */
public class InputManager {

    public InputManager(){
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }


    public void loadImage(String hdfsRootPath, String hdfsFolderPath, File imgFile, Integer index) throws IOException, URISyntaxException {

        byte[] img = Files.readAllBytes(Paths.get(imgFile.getAbsolutePath()));
        loadImage(hdfsRootPath,hdfsFolderPath,img,index);
    }

    /**
     * saves a given img file to a hdfs path
     *
     */
    public void loadImage(String hdfsRootPath, String hdfsFolderPath, byte[] img, Integer index) throws IOException, URISyntaxException {
        //1. Get the instance of COnfiguration
        Configuration configuration = new Configuration();
        //2. Create an InputStream to read the data from local filejava fi
        //InputStream inputStream = new FileInputStream(imgFile);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(img);
        oos.flush();
        oos.close();
        InputStream inputStream = new ByteArrayInputStream(baos.toByteArray());

        //3. Get the HDFS instance
        FileSystem hdfs = FileSystem.get(new URI(hdfsRootPath), configuration);
        //4. Open a OutputStream to write the data, this can be obtained from the FileSytem
        OutputStream outputStream = hdfs.create(new Path(hdfsFolderPath + index + ".jpg"),
                new Progressable() {
                    @Override
                    public void progress() {
                        //System.out.println("....");
                    }
                });
        try
        {
            IOUtils.copyBytes(inputStream, outputStream, 4096, false);
        }
        finally
        {
            IOUtils.closeStream(inputStream);
            IOUtils.closeStream(outputStream);
        }

    }

    /**
     * saves a given folder of img files to a hdfs path
     * @param imgFolder
     */
    public void loadImageFolder(String hdfsRootPath, String hdfsFolderPath, File imgFolder) throws IOException, URISyntaxException {

        File[] fileList = imgFolder.listFiles();

        int index = 0;
        for(File f: fileList){

            loadImage(hdfsRootPath, hdfsFolderPath, f, index);
            index++;
        }
    }


    /**
     * saves a given video file to a hdfs path. filename is the frame number
     * @param videoFile
     */
    public void loadVideo(String hdfsRootPath, String hdfsFolderPath, File videoFile) throws IOException, URISyntaxException {


        VideoCapture cap = new VideoCapture(videoFile.getAbsolutePath());

        if(!cap.isOpened())  // check if we succeeded
            return;

        int index = 0;
        Mat frame = new Mat();
        while(cap.read(frame))
        {

            MatOfByte matOfByte = new MatOfByte();
            Imgcodecs.imencode(".jpg", frame, matOfByte);
            byte[] byteArray = matOfByte.toArray();
            loadImage(hdfsRootPath, hdfsFolderPath, byteArray, index);
            index++;

       /*     File tmpFile = File.createTempFile("img", null, null);

            Imgcodecs.imwrite(tmpFile.getAbsolutePath(), frame);
            loadImage(hdfsRootPath, hdfsFolderPath, tmpFile, index);
            index++;
            tmpFile.delete();*/
        }


    }

    public static void main(String[] args) throws IOException, URISyntaxException {
        InputManager im = new InputManager();
       /* im.loadImage("hdfs://localhost:54310/",
                "hdfs://localhost:54310/testFile2/",
                new File("/home/bduser/IdeaProjects/bdprak/img/2.jpg"),
                5);*/

    }

}