package org.bdprak.frontend;

import org.bdprak.input.InputManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

/**
 * Created by Jakob Kusnick on 12.07.16.
 *
 * The panel for the selection and HDFS upload of the videos.
 * Extends OptionsListPanel, so the separate options rows are instances of the nested class VideoSelectRow.
 */
public class VideoSelectPanel extends OptionsListPanel{

    private JButton start;
    private Frontend frontend;

    /**
     * The panel for the selection and HDFS upload of the videos.
     * @param frontend The frontend to get the paths from the options panel.
     */
    public VideoSelectPanel(Frontend frontend) {

        super("org.bdprak.frontend.VideoSelectPanel$VideoSelectRow");

        this.frontend = frontend;

        // Start button to read out the videos, cut them into separated frames and load them to the HDFS.
        start = new JButton("Start");
        start.setPreferredSize(new Dimension(100,20));
        start.addActionListener(this);

        getSouthPanel().add(start);

        start.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                InputManager im = new InputManager();

                for (Object row : getContentList()) {
                    try {
                        VideoSelectRow videoSelectRow = (VideoSelectRow) row;
                        String inputPath = videoSelectRow.path_field.getText();
                        String hdfsTargetPath = videoSelectRow.hdfs_folder_path_field.getText();
                        if (!hdfsTargetPath.endsWith("/")) {
                            hdfsTargetPath += "/";
                        }
                        String hdfsRootPath = frontend.getHDFSRootPath();

                        //Load the video, cut it into separated frames and load it up to the given HDFS path
                        im.loadVideo(hdfsRootPath, hdfsTargetPath, new File(inputPath));

                        // Set the text of the recognition and picpane textfield to the path of the video.
                        frontend.setRecognitionVideo(hdfsTargetPath);

                    } catch (Exception ea) {
                        ea.printStackTrace();
                    }
                }
            }
        });
    }

    /**
     * A separated panel for each options row in the parent OptionsListPanel
     */
    public class VideoSelectRow extends JPanel implements ActionListener {

        private JTextField path_field;
        private JButton choose_folder;
        private JLabel hdfs_folder_label;
        private JTextField hdfs_folder_path_field;

        public VideoSelectRow() {

            //Set up the GUI, ...
            setLayout(new GridBagLayout());
            GridBagConstraints constraint = new GridBagConstraints();
            constraint.gridx = 0;
            constraint.gridy = 0;
            constraint.insets = new Insets(3,5,3,5);

            //... all labels, buttons and textfields
            JLabel preprocess_label = new JLabel("Video File:");
            hdfs_folder_label = new JLabel("HDFS Folder:");

            path_field = new JTextField();
            path_field.setPreferredSize(new Dimension(280, 20));
            path_field.setMaximumSize(new Dimension(400,20));

            //Button for the selection of a video file
            choose_folder = new JButton("Select File");
            choose_folder.setPreferredSize(new Dimension(180, 20));
            choose_folder.setMaximumSize(new Dimension(400,20));
            choose_folder.addActionListener(this);

            hdfs_folder_path_field = new JTextField();

            hdfs_folder_path_field.setMaximumSize(new Dimension(400,20));
            hdfs_folder_path_field.setPreferredSize(new Dimension(280, 20));

            add(preprocess_label, constraint);
            constraint.gridx++;
            add(path_field, constraint);
            constraint.gridx++;
            add(choose_folder, constraint);

            constraint.gridy++;
            constraint.gridx = 0;
            add(hdfs_folder_label, constraint);
            constraint.gridx++;
            add(hdfs_folder_path_field, constraint);
        }


        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == choose_folder) {
                JFileChooser fc = new JFileChooser();
                int returnVal = fc.showOpenDialog(this);

                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = fc.getSelectedFile();
                    path_field.setText(file.getAbsolutePath());
                }
            }
        }
    }
}
