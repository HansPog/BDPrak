package org.bdprak.frontend;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import scala.Tuple2;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * simple gui to show img and videos in hdfs
 */
public class TestFrontend extends JPanel implements ActionListener{

    /**
     * TODO: bei aufruf pfadliste aller bilder erstellen
     * laden der bilddaten erst bei anzeige (nicht alles im speicher behalten)
     *
     * anzeige eines frames, umschalten über <> tasten
     */

    private JLabel img;
    private JButton next, prev, play,stop,back;
    private FileSystem hdfs;
    private HashMap<Integer, Path> paths;
    private List<Integer> indices;
    private int currentIndexPosition; //current position in indices list
    private boolean isPlaying;
    private JFrame frame;

    /**
     *
     * @param hdfsFolderPath hdfs path to a folder containing Tuple2<int frameCount, byte[] frameImg>
     */
    public TestFrontend(String hdfsRootPath, String hdfsFolderPath) throws URISyntaxException, IOException, ClassNotFoundException {
        //set up gui
        super();

        isPlaying = false;

        img = new JLabel();
        this.add(img, BorderLayout.CENTER);

        back = new JButton("|<<");
        back.addActionListener(this);
        next = new JButton(">");
        next.addActionListener(this);
        prev = new JButton("<");
        prev.addActionListener(this);
        play = new JButton("play");
        play.addActionListener(this);
        stop = new JButton("stop");
        stop.addActionListener(this);


        Box btnBox = Box.createHorizontalBox();

        btnBox.add(back);
        btnBox.add(prev);
        btnBox.add(next);
        btnBox.add(play);
        btnBox.add(stop);

        this.add(btnBox, BorderLayout.SOUTH);

        frame = new JFrame();
        frame.setContentPane(this);

        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.setMinimumSize(new Dimension(800,600));


        //set up hdfs
        Configuration configuration = new Configuration();
        this.hdfs = FileSystem.get(new URI(hdfsRootPath), configuration);
        RemoteIterator<LocatedFileStatus> iter  = hdfs.listFiles(new Path(hdfsFolderPath), false);

        paths = new HashMap<>();
        indices = new ArrayList<>();
        while (iter.hasNext()) {

            LocatedFileStatus fStatus = iter.next();
            if(hdfs.isFile(fStatus.getPath())){

                int index = Integer.parseInt(fStatus.getPath().getName().substring(0,fStatus.getPath().getName().lastIndexOf(".")));

                paths.put(index, fStatus.getPath());
                indices.add(index);
                fStatus.getPath();
            }
        }

        Collections.sort(indices);
        currentIndexPosition = 0;

        showFrame(indices.get(currentIndexPosition));
    }

    private void showFrame(int i) throws IOException, ClassNotFoundException {

        BufferedImage bufImage = null;
        InputStream inSt = new ByteArrayInputStream(getFrame(paths.get(i))._2);
        bufImage = ImageIO.read(inSt);
        img.setIcon(new ImageIcon(bufImage));

        frame.setTitle("HDFS GUI    Frame: " + indices.get(currentIndexPosition));
        //this.getContentPane().revalidate();
        //this.getContentPane().repaint();
        this.invalidate();
        this.repaint();
    }

    @Override
    protected void paintComponent(Graphics g){
        super.paintComponent(g);

        try {
            Thread.sleep(20);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if(isPlaying){
            playNextFrame();
        }
    }

    private Tuple2<Integer, byte[]> getFrame(Path hdfsPath) throws IOException, ClassNotFoundException {
;
        //System.out.println(hdfs.exists(hdfsPath));
        //System.out.println(hdfs.isFile(hdfsPath));

        FSDataInputStream in =  hdfs.open(hdfsPath);
        ObjectInputStream ois = new ObjectInputStream(in);

        int index = Integer.parseInt(hdfsPath.getName().substring(0,hdfsPath.getName().lastIndexOf(".")));


        return new Tuple2<>(index, (byte[]) ois.readObject());
    }

    public static void main(String[] args) throws IOException, URISyntaxException, ClassNotFoundException {
        JPanel window = new TestFrontend("hdfs://localhost:54310/","hdfs://localhost:54310/testLoadVideo/");
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        if(ae.getSource() == next){
            //System.out.println("next");
            if(currentIndexPosition < indices.size() -1){
                currentIndexPosition++;
                try {
                    showFrame(indices.get(currentIndexPosition));
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }

        }
        if(ae.getSource() == prev){
            //System.out.println("prev");
            if(currentIndexPosition > 0){
                currentIndexPosition--;
                try {
                    showFrame(indices.get(currentIndexPosition));
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
        if(ae.getSource() == play){
            isPlaying = true;
            playNextFrame();

        }
        if(ae.getSource() == stop){
            isPlaying=false;
        }
        if(ae.getSource() == back){
            currentIndexPosition = 0;
            try {
                showFrame(indices.get(currentIndexPosition));
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private void playNextFrame(){

        if(currentIndexPosition < indices.size()-1) {
            currentIndexPosition++;
            try {
                showFrame(indices.get(currentIndexPosition));
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

            this.repaint();
            this.revalidate();
        }else{
            isPlaying = false;
        }
    }
}
