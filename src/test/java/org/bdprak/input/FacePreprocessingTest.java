package org.bdprak.input;

import org.junit.Test;

/**
 * Created by bduser on 22.06.16.
 */
public class FacePreprocessingTest {

    @Test
    public void testFolderProc(){
        FacePreprocessing fp = new FacePreprocessing(0.25,0.25,100,100);

        fp.processFolder("/home/bduser/IdeaProjects/bdprak/prepTest/", "/home/bduser/IdeaProjects/bdprak/prepTestOut/");
    }
}
