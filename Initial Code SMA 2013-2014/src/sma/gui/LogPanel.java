package sma.gui;

import javax.swing.*;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.awt.Toolkit;

/**
 * <p><B>Title:</b> IA2-SMA</p>
 * <p><b>Description:</b> Practical exercise 2013-14. Recycle swarm.</p>
 * Class showing the area of logs. Initially it contains a textarea and
 * a button to save the logs in a file. 
 * <p><b>Copyright:</b> Copyright (c) 2013</p>
 * <p><b>Company:</b> Universitat Rovira i Virgili (<a
 * href="http://www.urv.cat">URV</a>)</p>
 * @author David Isern & Joan Albert López
 * @version 2.0
 */
public class LogPanel extends JPanel {

  private static JTextArea jTextArea;
  private JButton doSave;
  private JFileChooser fc = new JFileChooser();

  private int inset = 60;

  /**
   * Default constructor
   */
  public LogPanel() {
    super();
    this.initComponents();
    this.setVisible(true);
  }

  /**
   * Mostra una cadena en l'àrea de text (jTextArea) del panell
   * @param msg String a mostrar
   */
  public static void showMessage(String msg) {
    jTextArea.append(msg);
  }

  private void initComponents() {
    this.setLayout(new AbsoluteLayout()); 
    this.setBorder(UtilsGUI.createBorder("Logs"));
    this.setMinimumSize(new Dimension(640, 480));
    this.setPreferredSize(new Dimension(640, 480));

    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

    this.jTextArea = UtilsGUI.createTextArea();
    JScrollPane jScrollPane = new JScrollPane(this.jTextArea);

    this.add(jScrollPane,
             new AbsoluteConstraints( 15,
                                      20,
                                      screenSize.width-(3*inset),
                                      screenSize.height-(4*inset)));

    /** button doSave */
    doSave = UtilsGUI.createButton("Save log ...", UtilsGUI.pathIconButtonSave);
    doSave.setToolTipText("Save the information into a log file");
    doSave.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        doSaveActionPerformed(evt);
      }
    });
    this.add(doSave,
             new AbsoluteConstraints(15,
                                     screenSize.height-(inset*2)-80,
                                     120,
                                     20));
  }


  private void doSaveActionPerformed(ActionEvent evt) {
    System.out.println("Saving content ...\n");
    String fileToSave = "log" + System.currentTimeMillis() + ".log";
    String contentToSave = this.jTextArea.getText();
    File dummy = new File(fileToSave);
    fc.setSelectedFile(dummy);
    fc.setName(fileToSave);
    fc.setDialogTitle("Save logs in a file");
    fc.setDialogType(JFileChooser.SAVE_DIALOG);
    fc.setCurrentDirectory(dummy);
    int returnVal = fc.showSaveDialog(this);
    if (returnVal == JFileChooser.APPROVE_OPTION) {
      File fileDest = fc.getSelectedFile();
      try {
        UtilsGUI.writeFile(contentToSave, fileDest);
        System.out.println("OK, EVERYTHING IS CORRECT\n");
      }
      catch (IOException e) {
        System.err.println("IO ERROR" + e.toString() + "\n");
      }
    }
    else {
      System.err.println("Cancelled by the user ..\n");
    }
  } //endof doSaveActionPerformed


}
