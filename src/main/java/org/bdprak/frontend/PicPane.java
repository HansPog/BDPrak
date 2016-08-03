package org.bdprak.frontend;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;
import scala.Tuple2;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.font.FontRenderContext;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;

/**
 * Created by Jakob Kusnick on 12.07.16.
 *
 * A panel to view the results of the recognition.
 */
public class PicPane extends JPanel implements ActionListener {

    private JButton next, prev, play,stop,back, okButton;
    private boolean isPlaying, show;
    private FileSystem hdfs;
    private HashMap<Integer, Path> paths;
    private HashMap<Integer, Path> jsonPaths;
    private java.util.List<Integer> indices;
    private int currentIndexPosition; //current position in indices list
    private BufferedImage bufImage;
    private BufferedReader br;
    private int pred, x, y, width, height;
    private HashMap<Integer, String> personMap;
    private HashMap<Integer, Color> personColor;
    private Random rnd;
    private Boolean tracer = false, points = false;
    private HashMap<Integer, Polygon> personPolygon;
    private HashMap<Integer, ArrayList<Point>> personPoints;
    private JTextField videoField;
    private Frontend frontend;

    /**
     * Creates a panel with space for the video frames, a control panel for the video flow and a textfield for the HDFS path for the video frames.
     * @param frontend An instance of Frontend, to access the textfields from the option panel.
     */
    public PicPane(Frontend frontend) {

            //Set up the GUI
            this.frontend = frontend;
            isPlaying = false;
            rnd = new Random();

            this.setLayout(new BorderLayout());

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

            JPanel southPanel = new JPanel();
            southPanel.setLayout(new GridBagLayout());
            GridBagConstraints southConstraints = new GridBagConstraints();
            southConstraints.gridx = 0;
            southConstraints.gridy = 0;
            southPanel.add(back, southConstraints);
            southConstraints.gridx++;
            southPanel.add(prev, southConstraints);
            southConstraints.gridx++;
            southPanel.add(next, southConstraints);
            southConstraints.gridx++;
            southPanel.add(play, southConstraints);
            southConstraints.gridx++;
            southPanel.add(stop, southConstraints);

            southConstraints.gridx = 0;
            southConstraints.gridy++;
            southConstraints.gridwidth = 5;
            southConstraints.insets = new Insets(10,0,5,0);

            videoField = new JTextField();
            videoField.setPreferredSize(new Dimension(300,20));
            videoField.setMaximumSize(new Dimension(400,20));

            southPanel.add(videoField, southConstraints);

            southConstraints.gridx = 5;
            southConstraints.gridwidth = 1;

            okButton = new JButton("OK");
            okButton.setPreferredSize(new Dimension(100,20));
            okButton.setMaximumSize(new Dimension(100,20));
            okButton.addActionListener(this);

            southPanel.add(okButton, southConstraints);

            this.add(new VideoPanel(), BorderLayout.CENTER);
            this.add(southPanel, BorderLayout.SOUTH);
    }

    /**
     * Prepare for the replay of the video.
     * Connect to the HDFS, save all frame indicies and read out the personMap
     *
     * @param hdfsFolderPath The HDFS path to the folder with all video frames.
     * @param jsonFolderPath The HDFS path to the folder with all result JSONs.
     * @param personMapPath The path to the local personMap (for every person an int and a name).
     */
    private void prepare(String hdfsFolderPath, String jsonFolderPath, String personMapPath) throws URISyntaxException, IOException, JSONException {
        //set up HDFS
        String hdfsRootPath = frontend.getHDFSRootPath();

        if(hdfsRootPath.equals("")) {

        }
        else {
            show = true;
            Configuration configuration = new Configuration();
            this.hdfs = FileSystem.get(new URI(hdfsRootPath), configuration);
            //An iterator for the paths of the video frames
            RemoteIterator<LocatedFileStatus> iter = hdfs.listFiles(new Path(hdfsFolderPath), false);
            //An iterator for the paths of the result JSONs
            RemoteIterator<LocatedFileStatus> jsonIter = hdfs.listFiles(new Path(jsonFolderPath), false);

            paths = new HashMap<>();
            jsonPaths = new HashMap<>();
            indices = new ArrayList<>();

            //Iterate through all frame and collect them into a HashMap.
            while (iter.hasNext()) {

                LocatedFileStatus fStatus = iter.next();
                if (hdfs.isFile(fStatus.getPath())) {

                    int index = Integer.parseInt(fStatus.getPath().getName().substring(0, fStatus.getPath().getName().lastIndexOf(".")));

                    paths.put(index, fStatus.getPath());

                    //Collect the indices of the frames in a separated ArrayList.
                    indices.add(index);
                }
            }

            //Iterate through all JSONs and collect them into a HashMap.
            while (jsonIter.hasNext()) {

                LocatedFileStatus fStatus = jsonIter.next();
                if (hdfs.isFile(fStatus.getPath())) {

                    int index = Integer.parseInt(fStatus.getPath().getName().substring(0, fStatus.getPath().getName().indexOf(".")));
                    jsonPaths.put(index, fStatus.getPath());
                }
            }

            //Sort the index-ArrayList to enable the possibility to iterate through the video frames in the right order (using the indices in the ArrayList).
            Collections.sort(indices);

            //Set the current index to zero to begin with the frame with the lowest index.
            currentIndexPosition = 0;

            //Read out the personMap
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(personMapPath))));
            String personContent = "";
            String readerLine;

            while ((readerLine = reader.readLine()) != null) {
                personContent += readerLine;
            }

            //Create a JSONArray for all persons
            JSONArray jsonArray = null;
            jsonArray = new JSONArray(personContent);

            //HashMap for all person names
            personMap = new HashMap<>();

            //HashMap for the color of a persons result rectangle
            personColor = new HashMap<>();

            //HashMap for all points of the movement polygon for each person
            personPolygon = new HashMap<>();

            //HashMap for all movement points of every person
            personPoints = new HashMap<>();

            //Iterate through all person in the personMap
            for (int i = 0; i < jsonArray.length(); i++) {

                JSONObject obj = null;
                try{

                    obj = jsonArray.getJSONObject(i);
                }catch(JSONException e){
                    e.printStackTrace();

                }
                
                //Read out the person-Int and name, ...
                int personInt = obj.getInt("personInt");
                String personName = obj.getString("personName");

                //... put them into the HashMap
                personMap.put(personInt, personName);

                //Also put the color, the Polygon and the ArrayList for the person into their maps
                //the color is random generated in a defined window (not too dark "+50")
                Color col = new Color(rnd.nextInt(200) + 50, rnd.nextInt(200) + 50, rnd.nextInt(200) + 50);
                personColor.put(personInt, col);

                personPolygon.put(personInt, new Polygon());
                personPoints.put(personInt, new ArrayList<>());

            }
        }
    }

    /**
     * Print a single video frame to the video panel. The frame number is given by the currentIndexPosition.
     * @param g The graphic context of the parent panel.
     */
    private void showFrame(Graphics g) {

        Graphics2D g2d = (Graphics2D) g;

        bufImage = null;
        String jsonContent = "";
        String line;
        try {
            // Read out the JSON file for the actual frame index
            FileSystem fs = FileSystem.get(new URI(frontend.getHDFSRootPath()), new Configuration());
            FSDataInputStream in =  fs.open(jsonPaths.get(indices.get(currentIndexPosition)));
            br = new BufferedReader(new InputStreamReader(in));

            while((line = br.readLine()) != null) {
               jsonContent += line;
            }

            //Read out the person array
            JSONArray array = new JSONArray(jsonContent);

            //Read out the frame picture for the actual frame index
            InputStream inSt = null;

            inSt = new ByteArrayInputStream(getFrame(paths.get(indices.get(currentIndexPosition)))._2);
            bufImage = ImageIO.read(inSt);

            //show the image and the informations
            g2d.drawImage(bufImage, 0, 0, null);

            //draw the rectangle, name string and polygon and/or the movement points
            for(int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);

                //read out the informations from the json array
                pred = obj.getInt("prediction"); //prediciton int (which person was detected?)
                height = obj.getInt("height");
                width = obj.getInt("width");
                x = obj.getInt("x");
                y = obj.getInt("y");

                //get the name of the person
                String name = personMap.get(pred);

                //paint the person name
                int fontSize = 18;
                Font font = new Font("Arial", Font.PLAIN, fontSize);
                int nameWidth = (int) font.getStringBounds(name, new FontRenderContext(font.getTransform(), false, false)).getBounds().getWidth();
                g2d.setPaint(new Color(255,255,255, 150));
                g2d.fillRect(x, y + height, nameWidth + 2, fontSize + 2);

                g2d.setPaint(new Color(0,0,0));
                g2d.setFont(font);
                g2d. drawString(name, x + 1 , y + height + fontSize );

                //draw the persons face rectangle
                g2d.setPaint(personColor.get(pred));
                float thickness = 2;
                Stroke oldStroke = g2d.getStroke();
                g2d.setStroke(new BasicStroke(thickness));
                g2d.drawRect(x, y, width+1, height+1);
                g2d.setStroke(oldStroke);

                //add the face position to the persons point or polygon HashMap
                personPolygon.get(pred).addPoint(x+width/2, y + (height/2));
                personPoints.get(pred).add(new Point(x+width/2, y + (height/2)));
            }

            //draw the movement polygon or movement points for each person
            if(tracer || points) {
                for(int i = 0; i < personPolygon.size(); i++) {
                    g2d.setPaint(personColor.get(i));
                    if(tracer) {
                        g2d.drawPolygon(personPolygon.get(i));
                    }
                    else {
                        for(Point point : personPoints.get(i)) {
                            Stroke old = g2d.getStroke();
                            g2d.setStroke(new BasicStroke(2));
                            g2d.drawLine(point.x, point.y, point.x, point.y);
                            g2d.setStroke(old);
                        }
                    }
                }
            }

            this.invalidate();
            this.repaint();

        } catch (IOException | JSONException | URISyntaxException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            if(br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        // Increase the frame index
        if(ae.getSource() == next){
            if(currentIndexPosition < indices.size() -1){
                currentIndexPosition++;
            }
        }
        // Decrease the frame index
        if(ae.getSource() == prev){
            if(currentIndexPosition > 0){
                currentIndexPosition--;
            }
        }
        // Play the video frames
        if(ae.getSource() == play){
            isPlaying = true;
            playNextFrame();

        }
        //Stop the video replay
        if(ae.getSource() == stop){
            isPlaying=false;
        }
        //Skip back to the first frame
        if(ae.getSource() == back){
            currentIndexPosition = 0;
            for(int i = 0; i < personPolygon.size(); i++) {
                personPolygon.put(i, new Polygon());
                personPoints.put(i, new ArrayList<>());
            }
        }
        // Submit the HDFS path to the video folder and read/show the results
        if(ae.getSource() == okButton) {
            try {

                String folder = videoField.getText(); //hdfs://localhost:54310/testLoadVideo/
                if(folder.endsWith("/")){
                    folder = folder.substring(0,folder.length()-1); //hdfs://localhost:54310/testLoadVideo
                }
                String parentFolder = folder.substring(0, folder.lastIndexOf("/")); //hdfs://localhost:54310

                //output path: hdfs://localhost:54310/recResult/testLoadVideo
                String outputPath = parentFolder + "/recResult/" + folder.substring(folder.lastIndexOf("/")+1);

                System.out.println(outputPath);
                //String outputPath = parentFolder + "/recResult" + folder.substring(folder.lastIndexOf("/"));
                
                prepare(folder, outputPath, frontend.getPersonMapPath());
                repaint();
            } catch (JSONException | URISyntaxException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Shows the next video frame
     */
    private void playNextFrame(){

        if(currentIndexPosition < indices.size()-1) {
            currentIndexPosition++;

            this.repaint();
            this.revalidate();
        }else{
            isPlaying = false;
        }
    }

    /**
     * Gets the index and the byte array of a frame by the given HDFS path to the frame
     * @param hdfsPath
     * @return
     */
    private Tuple2<Integer, byte[]> getFrame(Path hdfsPath) throws IOException, ClassNotFoundException {

        FSDataInputStream in =  hdfs.open(hdfsPath);
        ObjectInputStream ois = new ObjectInputStream(in);

        int index = Integer.parseInt(hdfsPath.getName().substring(0,hdfsPath.getName().lastIndexOf(".")));

        return new Tuple2<>(index, (byte[]) ois.readObject());
    }

    /**
     * Creates a empty panel with a graphics context to draw the images on it.
     */
    private class VideoPanel extends JPanel {
        public VideoPanel() {
            super();
            setMinimumSize(new Dimension(30,30));
        }

        @Override
        public void paintComponent(Graphics g) {

            //show the separated frames
            super.paintComponent(g);

            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if(isPlaying){
                playNextFrame();
            }
            if(show == true) {
                showFrame(g);
            }
        }
    }

    /**
     * Sets the video path textfield to the given path string
     * @param path The HDFS path to the video folder as a String
     */
    public void setVideo(String path) {
        videoField.setText(path);
    }

}
