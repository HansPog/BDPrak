package org.bdprak.frontend;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

/**
 * Created by Jakob Kusnick on 07.07.16.
 *
 * A panel with an add button to add rows of separated panels to a list. The row panels are customizable and are defined by the class name.
 */
public class OptionsListPanel extends JPanel implements ActionListener {

    private JButton plus;
    private String class_name;
    private JPanel center_panel;
    private GridBagConstraints constraint;
    private ArrayList content_list;
    private JPanel southPanel;

    /**
     * Creates a new OptionsListPanel
     * @param class_name Defines the nested row panels by the class name.
     */
    public OptionsListPanel(String class_name) {

        this.class_name = class_name;

        setLayout(new BorderLayout());

        southPanel = new JPanel();

        plus = new JButton("+");
        plus.setPreferredSize(new Dimension(50,20));
        plus.addActionListener(this);

        southPanel.add(plus);

        add(southPanel, BorderLayout.SOUTH);

        center_panel = new JPanel(new GridBagLayout());
        constraint = new GridBagConstraints();
        constraint.gridx = 0;
        constraint.gridy = 0;
        constraint.insets = new Insets(10,5,10,5);

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.add(center_panel);
        add(scrollPane, BorderLayout.CENTER);

        //Add one default row
        addRow();

    }

    /**
     * Adds an additional row to the list
     */
    public void addRow() {

        Class<?> c = null;
        try {
            // Create a new instance of the give class and add it the the list
            c = Class.forName(class_name);
            Constructor<?> ctor = c.getConstructors()[0];
            Object object = ctor.newInstance(new Object[]{null});

            if(JPanel.class.isAssignableFrom(object.getClass())) {
                Component comp = (Component)object;
                center_panel.add(comp, constraint);
                constraint.gridx = 1;

                if(content_list == null) {
                    content_list = new ArrayList();
                }

                content_list.add(comp);

                //Add a delete button for the row, to delete the row from the list)
                JButton delete = new JButton("-");
                delete.setPreferredSize(new Dimension(40,20));
                delete.setMaximumSize(new Dimension(40,20));
                delete.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if(e.getSource() == delete) {
                            center_panel.remove(comp);
                            center_panel.remove(delete);
                            center_panel.updateUI();
                        }
                    }
                });

                center_panel.add(delete, constraint);
                constraint.gridx = 0;
                constraint.gridy++;
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        center_panel.updateUI();

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if(e.getSource() == plus) {
            addRow();
        }
    }

    public ArrayList getContentList() {
        return content_list;
    }

    public JPanel getSouthPanel() { return southPanel; }

}
