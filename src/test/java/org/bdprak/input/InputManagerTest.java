package org.bdprak.input;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.bdprak.frontend.TestFrontend;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opencv.core.Core;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by bduser on 08.06.16.
 */
public class InputManagerTest {

    public static final String hdfsRootStr  = "hdfs://localhost:54310/";
    public static final String hdfsVideoFolderStr = "hdfs://localhost:54310/testLoadVideo/";
    public static final String hdfsImageFolderStr = "hdfs://localhost:54310/testLoadFolder/";

    @BeforeClass
    public static void setup(){
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    @After
    public void cleanup() throws IOException, URISyntaxException {

        Configuration configuration = new Configuration();
        FileSystem hdfs = FileSystem.get(new URI(hdfsRootStr), configuration);
        Path hdfsFolderPath =new Path(hdfsVideoFolderStr);

        if(hdfs.exists(hdfsFolderPath)){
            System.out.println("folder already exists");
            hdfs.delete(hdfsFolderPath, true);
        }
    }

    @Test
    public void testLoadVideo() throws URISyntaxException, IOException, ClassNotFoundException {

        InputManager im = new InputManager();


        Configuration configuration = new Configuration();
        FileSystem hdfs = FileSystem.get(new URI(hdfsRootStr), configuration);

        Path hdfsFolderPath =new Path(hdfsVideoFolderStr);

        if(hdfs.exists(hdfsFolderPath)){
            System.out.println("folder already exists");
            hdfs.delete(hdfsFolderPath, true);
        }else{
            System.out.println("folder did not exist");
        }

        im.loadVideo(hdfsRootStr,hdfsVideoFolderStr,new File("/home/bduser/IdeaProjects/bdprak/video/cut.mp4"));

        TestFrontend tf = new TestFrontend(hdfsRootStr, hdfsVideoFolderStr);

        System.out.println("GUI started");

        System.in.read();


    }


    @Test
    public void testLoadFolder() throws URISyntaxException, IOException, ClassNotFoundException {

        InputManager im = new InputManager();


        Configuration configuration = new Configuration();
        FileSystem hdfs = FileSystem.get(new URI(hdfsRootStr), configuration);

        Path hdfsFolderPath = new Path(hdfsImageFolderStr);

        if(hdfs.exists(hdfsFolderPath)){
            System.out.println("folder already exists");
            hdfs.delete(hdfsFolderPath, true);
        }else{
            System.out.println("folder did not exist");
        }

        im.loadImageFolder(hdfsRootStr, hdfsImageFolderStr, new File("/home/bduser/IdeaProjects/bdprak/img"));

        TestFrontend tf = new TestFrontend(hdfsRootStr, hdfsImageFolderStr);

        System.out.println("GUI started");

        System.in.read();


    }
}
