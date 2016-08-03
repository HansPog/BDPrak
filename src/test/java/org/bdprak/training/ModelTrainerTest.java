package org.bdprak.training;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by bduser on 28.06.16.
 */
public class ModelTrainerTest {

    @Test
    public void testModelTrainer() throws IOException, URISyntaxException {

        String hdfsRootStr = "hdfs://localhost:54310/";
        Path file = new Path("hdfs://localhost:54310/trainingDataWill/");

        File folder = new File("/home/bduser/IdeaProjects/bdprak/will");

        List< File > imgFolderPaths = new ArrayList<>();
        imgFolderPaths.add(folder);
        List<Integer> labels = new ArrayList<>();
        labels.add(0);
        String algortihm = ModelTrainer.LBPH;

        ModelTrainer mt = new ModelTrainer(hdfsRootStr, file, imgFolderPaths, labels,  algortihm, false);

        Configuration configuration = new Configuration();
        FileSystem hdfs = FileSystem.get(new URI(hdfsRootStr), configuration);

        FSDataInputStream stream = hdfs.open(file);


        int bytesRead;
        ByteBuffer buffer = ByteBuffer.allocate(99999999);

        while ((bytesRead = stream.read(buffer)) > 0) {
            System.out.write(buffer.array(), 0, bytesRead);
        }
    }
}
