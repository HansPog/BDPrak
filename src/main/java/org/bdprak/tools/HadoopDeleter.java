package org.bdprak.tools;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * deletes recursively given hdfs path
 */
public class HadoopDeleter {

    public static void delete(String hdfsRootPath, String deletePath) throws URISyntaxException, IOException {

        Configuration configuration = new Configuration();
        FileSystem hdfs = FileSystem.get(new URI(hdfsRootPath), configuration);

        RemoteIterator<LocatedFileStatus> ri = hdfs.listFiles(new Path("hdfs://localhost:54310/"), true);
        while (ri.hasNext()){
            hdfs.delete(ri.next().getPath(), true);
        }
    }

    public static void main(String[] args) throws IOException, URISyntaxException {
        delete("hdfs://localhost:54310/", "hdfs://localhost:54310/testLoadVideo");
    }
}
