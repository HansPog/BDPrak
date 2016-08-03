package org.bdprak.frontend;

import org.opencv.core.Core;
import javax.swing.*;
import java.awt.*;

/**
 * Created by Jakob Kusnick on 07.07.16.
 *
 * Main class to start the program with gui
 */
public class Frontend {

    //static strings for Buttons, etc
    public static String PREPROCESS_STRING = "Video Preprocessing";
    public static String TRAIN_STRING = "Training";
    public static String RECOGNITION_STRING = "Recognition";

    private RecognitionPanel recognitionPanel;
    private PicPane picpane;

    //all the textfields for the options
    private JTextField hdfsRootTextField;
    private JTextField hdfsTrainingModelTextField;
    private JTextField localTrainingModelField;
    private JTextField personMap;

    public static void main(String args[]){
        Frontend frontend = new Frontend();
    }


    /**
     * Creates a frame and tabbed pane with all the various panels inside
     */
    public Frontend() {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        JFrame frame = new JFrame();
        frame.setSize(860, 740);

        JPanel contentPane = new JPanel();
        contentPane.setLayout(new BorderLayout());

        JTabbedPane tabbedPane = new JTabbedPane();

        //Panel for the selection of videos
        VideoSelectPanel preprocessingPanel = new VideoSelectPanel(this);
        tabbedPane.addTab(PREPROCESS_STRING, preprocessingPanel);

        //Panel for the selection of persons and their pictures  for the training model
        FolderSelectPanel trainingPanel = new FolderSelectPanel(this);
        tabbedPane.addTab(TRAIN_STRING, trainingPanel);

        //Panel for the selection of a video in a HDFS for the recognition process
        recognitionPanel = new RecognitionPanel(this);
        tabbedPane.addTab(RECOGNITION_STRING, recognitionPanel);

        //Panel for the visual presentation
        picpane = new PicPane(this);
        tabbedPane.addTab("Result", picpane);

        //Panel for all the options
        JPanel optionsPane = new JPanel();
        optionsPane.setLayout(new GridLayout(0,2));
        JLabel hdfsRootLabel = new JLabel("HDFS ROOT PATH:");
        hdfsRootTextField = new JTextField("hdfs://localhost:54310/");
        JLabel hdfsTrainingModelLabel = new JLabel("HDFS PATH TO TRAINING MODEL:");
        JLabel localTrainingModelLabel = new JLabel("LOCAL PATH TO TRAINING MODEL:");
        JLabel personMapLabel = new JLabel("PATH TO PERSON MAP");
        hdfsTrainingModelTextField = new JTextField("hdfs://localhost:54310/training.xml");
        localTrainingModelField = new JTextField("/home/bduser/model.xml");
        personMap = new JTextField("personMap.json");

        //Add all the labels and their textfields
        optionsPane.add(hdfsRootLabel);
        optionsPane.add(hdfsRootTextField);

        optionsPane.add(hdfsTrainingModelLabel);
        optionsPane.add(hdfsTrainingModelTextField);

        optionsPane.add(localTrainingModelLabel);
        optionsPane.add(localTrainingModelField);

        optionsPane.add(personMapLabel);
        optionsPane.add(personMap);

        //Add the tabbed and the the option panel to the frame
        contentPane.add(tabbedPane, BorderLayout.CENTER);
        contentPane.add(optionsPane, BorderLayout.SOUTH);

        frame.setContentPane(contentPane);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    /**
     * Sets path textfield in the recognition and presentation panel the HDFS path to the video
     * @param path The path to the video folder in the HDFS
     */
    public void setRecognitionVideo(String path) {
        recognitionPanel.hdfsVideoFolderField.setText(path);
        picpane.setVideo(path);
    }

    /**
     * Returns the path to the personMap from the textfield as string
     * @return The local path to the personMap.
     */
    public String getPersonMapPath() {
        return personMap.getText();
}

    /**
     * Returns the path to the HDFS root from the textfield as a string.
     * @return The path to the HDFS root node.
     */
    public String getHDFSRootPath() {
        return hdfsRootTextField.getText();
    }

    /**
     * Returns the HDFS path to the training model from the textfield as a string.
     * @return The HDFS path to the training model.
     */
    public String getTrainingsModelPath() {
        return hdfsTrainingModelTextField.getText();
    }


    /**
     * Returns the local path to the training model from the textfield as a string.
     * @return The local path to the training model.
     */
    public String getLocalTrainingsModelPath() {
        return localTrainingModelField.getText();
    }
}
