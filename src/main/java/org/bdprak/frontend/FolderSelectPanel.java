package org.bdprak.frontend;

import org.apache.commons.io.FileUtils;
import org.apache.hadoop.fs.Path;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.bdprak.input.FacePreprocessing;
import org.bdprak.training.ModelTrainer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;

/**
 * Created by Jakob Kusnick on 07.07.16.
 *
 * Panel for the selection of all persons and their picture folder.
 * Extends OptionsListPanel, so each option row is an instance of the nested class FolderSelectRow.
 */
public class FolderSelectPanel extends OptionsListPanel {

    private Frontend frontend;
    private JButton start;

    /**
     *
     * @param frontend The instance of Frontend, to get the paths from the options panel.
     */
    public FolderSelectPanel(Frontend frontend) {
        super("org.bdprak.frontend.FolderSelectPanel$FolderSelectRow");

        this.frontend = frontend;

        // Create the start button
        start = new JButton("Start");
        start.setPreferredSize(new Dimension(100,20));

        getSouthPanel().add(start);

        /** THe start button (re)creates the local personMap file,
        * adds a xml entry for every person,
        * writes it to the xml file,
        * preprocess the faces,
        * creates the model
        * and distribute it to the HDFS
         **/
        start.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {

                //(Re)Create the XML file for the personMap
                File faceMapFile = new File(frontend.getPersonMapPath());
                if (faceMapFile.exists()) {
                    faceMapFile.delete();
                }

                try {
                    faceMapFile.createNewFile();

                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                //Create the JSONArray for all persons: a person have a personInt and a personName
                JSONArray personMap = new JSONArray();

                JSONObject person = new JSONObject();
                Integer faceCount = 0;

                ArrayList<File> trainingFolders = new ArrayList<>();
                ArrayList<File> outputFolders = new ArrayList<>();

                for (Object row : getContentList()) {

                    FolderSelectRow folderSelectRow = (FolderSelectRow) row;
                    try {
                        person = new JSONObject();
                        person.put("personInt", faceCount.toString());
                        person.put("personName", folderSelectRow.name_field.getText());
                        personMap.put(person);
                        faceCount++;
                    } catch (JSONException e1) {
                        e1.printStackTrace();
                    }
                    trainingFolders.add(new File(folderSelectRow.path_field.getText()));
                    outputFolders.add(new File(folderSelectRow.output_folder_field.getText()));
                }

                // Write the personMap to the XML file
                BufferedWriter br = null;
                try {
                    br = new BufferedWriter(new FileWriter(faceMapFile.getAbsoluteFile()));
                    br.write(personMap.toString());
                    br.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }


                //preprocess faces
                ArrayList<Integer> labels = new ArrayList<>();
                FacePreprocessing fp = new FacePreprocessing(0.2, 0.2, 200, 200);

                for (int i = 0; i < trainingFolders.size(); i++) {

                    fp.processFolder(trainingFolders.get(i).getAbsolutePath(), outputFolders.get(i).getAbsolutePath());
                    labels.add(i);
                }

                JOptionPane.showMessageDialog(null, "Finished preprocessing. Please check output folders and press \"Ok\" to create model");

                //create model
                String algortihm = ModelTrainer.LBPH;
                //String algortihm = ModelTrainer.FISHER;
                try {
                    ModelTrainer mt = new ModelTrainer(frontend.getHDFSRootPath(), new Path(frontend.getTrainingsModelPath()), outputFolders, labels, algortihm, false);
                } catch (URISyntaxException e1) {
                    e1.printStackTrace();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                System.out.println("created model");

                File modelFile = new File(frontend.getLocalTrainingsModelPath());
                if(modelFile.exists()){
                    FileUtils.deleteQuietly(modelFile);
                }

                //distribute model
                SparkConf sparkConf = new SparkConf().setAppName("JavaSpark").setMaster("local");
                JavaSparkContext jsc = new JavaSparkContext(sparkConf);

                JavaRDD<String> files = jsc.textFile(frontend.getTrainingsModelPath());
                JavaRDD<String> prt = files.repartition(1); //TODO: Anzahl worker hier eintragen ???
                prt.setName("test");
                prt.saveAsTextFile("file://" + frontend.getLocalTrainingsModelPath());

                jsc.stop();
            }
        });

    }

    /**
     * An options row to select a folder of pictures and tag them with a person name
     */
    public class FolderSelectRow extends JPanel implements ActionListener {

        private JTextField path_field;
        private JTextField output_folder_field;
        private JButton choose_folder;
        private JButton choose_output_folder;
        private JTextField name_field;
        private JLabel output_folder_label;
        private JLabel folder_label;

        /**
         * Sets up the GUI of the options row
         */
        public FolderSelectRow() {

            setLayout(new GridBagLayout());
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.insets = new Insets(3,5,3,5);

            JLabel preprocess_label = new JLabel(Frontend.PREPROCESS_STRING);

            folder_label = new JLabel("Folder:");
            output_folder_label = new JLabel("Output Folder:");

            name_field = new JTextField("PersonName");
            name_field.setPreferredSize(new Dimension(150, 20));
            name_field.setMaximumSize(new Dimension(400,20));
            path_field = new JTextField();
            path_field.setPreferredSize(new Dimension(280, 20));
            path_field.setMaximumSize(new Dimension(400,20));
            output_folder_field = new JTextField();
            output_folder_field.setPreferredSize(new Dimension(280, 20));
            output_folder_field.setMaximumSize(new Dimension(400,20));
            choose_folder = new JButton("Select Folder");
            choose_folder.setPreferredSize(new Dimension(200, 20));
            choose_folder.setMaximumSize(new Dimension(400,20));
            choose_folder.addActionListener(this);
            choose_output_folder = new JButton("Select Output Folder");
            choose_output_folder.setPreferredSize(new Dimension(200, 20));
            choose_output_folder.setMaximumSize(new Dimension(400,20));
            choose_output_folder.addActionListener(this);

            add(name_field, constraints);
            constraints.gridx++;
            add(folder_label, constraints);
            constraints.gridx++;
            add(path_field, constraints);
            constraints.gridx++;
            add(choose_folder, constraints);

            constraints.gridx = 1;
            constraints.gridy++;
            add(output_folder_label, constraints);
            constraints.gridx++;
            add(output_folder_field, constraints);
            constraints.gridx++;
            add(choose_output_folder, constraints);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if(e.getSource() == choose_folder) {
                JFileChooser fc = new JFileChooser();
                fc.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY);
                int returnVal = fc.showOpenDialog(this);

                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = fc.getSelectedFile();
                    path_field.setText(file.getAbsolutePath());
                }
            }
            if(e.getSource() == choose_output_folder) {
                JFileChooser fc = new JFileChooser();
                fc.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY);
                int returnVal = fc.showOpenDialog(this);

                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = fc.getSelectedFile();
                    output_folder_field.setText(file.getAbsolutePath());
                }
            }
        }
    }
}
