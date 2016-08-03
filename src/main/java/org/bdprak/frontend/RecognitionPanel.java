package org.bdprak.frontend;

import org.bdprak.processing.FaceRecBatchProcess;
import org.bdprak.processing.FaceRecBatchProcessWithResult;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URISyntaxException;

/**
 * Created by Jakob Kusnick on 27.07.16.
 *
 * A panel with a textfield to select the video folder (HDFS) for the recognition of the trained faces.
 */
public class RecognitionPanel extends JPanel implements ActionListener {

    private Frontend frontend;
    protected JTextField hdfsVideoFolderField;
    private JButton start;

    /**
     *  A panel with a textfield.
     * @param frontend A instance of Frontend to access the option textfields
     */
    public RecognitionPanel(Frontend frontend) {

        this.frontend = frontend;

        //Set up the GUI
        this.setLayout(new BorderLayout());

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new GridBagLayout());
        GridBagConstraints constraint = new GridBagConstraints();
        constraint.insets = new Insets(5,10,5,10);

        hdfsVideoFolderField = new JTextField();
        hdfsVideoFolderField.setPreferredSize(new Dimension(350,20));

        centerPanel.add(new JLabel("HDFS Path to video folder:"), constraint);
        centerPanel.add(hdfsVideoFolderField, constraint);

        JPanel southPanel = new JPanel();

        start = new JButton("Start");
        start.setPreferredSize(new Dimension(100,20));
        start.addActionListener(this);

        southPanel.add(start);

        add(centerPanel, BorderLayout.CENTER);
        add(southPanel, BorderLayout.SOUTH);

    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {

        //Start the recognition with the given options and paths
        if(actionEvent.getSource() == start) {
            System.out.println(frontend.getHDFSRootPath());
            System.out.println(hdfsVideoFolderField.getText());
            System.out.println(frontend.getLocalTrainingsModelPath()+"/part-00000");
            FaceRecBatchProcess frbp = new FaceRecBatchProcess(frontend.getHDFSRootPath(), hdfsVideoFolderField.getText(), frontend.getLocalTrainingsModelPath()+"/part-00000");

            try {
                frbp.processFolder();
            } catch (URISyntaxException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }
}
