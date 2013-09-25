package sma.gui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import javax.swing.border.Border;
import javax.swing.border.BevelBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JTextArea;
import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.PrintStream;

/**
 * <p><B>Title:</b> IA2-SMA</p>
 * <p><b>Description:</b> Practical exercise 2013-14. Recycle swarm.</p>
 * Class containing several tools used in different classes of the GUI.
 * I/O functions, constants, other static functions reused in different 
 * elements. 
 * <p><b>Copyright:</b> Copyright (c) 2013</p>
 * <p><b>Company:</b> Universitat Rovira i Virgili (<a
 * href="http://www.urv.cat">URV</a>)</p>
 * @author David Isern & Joan Albert López
 */
public class UtilsGUI {

  // directoris de les icones

  public static String IMAGE_DIR = "classes"+
      System.getProperty("file.separator")+"sma"+
      System.getProperty("file.separator")+"images"+
      System.getProperty("file.separator");


  public static String pathIconPartida = UtilsGUI.IMAGE_DIR + "meneame.gif";
  public static String pathIconLogs = UtilsGUI.IMAGE_DIR + "ico_mas.gif";
  public static String pathIconStatistics = UtilsGUI.IMAGE_DIR + "ico_mas.gif";
  public static String pathIconButton = UtilsGUI.IMAGE_DIR + "ico_bajar.gif";

  public static String pathIconButtonSave = UtilsGUI.IMAGE_DIR + "save.gif";

  // constants de les TextAreas
  public static Color fonsAreaTreball = new Color(250,250,250); //griset més clar
  public static Color colorText = Color.BLACK;

  // constants globals
  public static Font fontTextMessages = new java.awt.Font("Helvetica",0,11);
  public static Font fontText = new Font("Arial",0,11);
  public static Font fontTextSmall = new Font("Arial",0,10);

  //  public static Color fonsBoto = new Color(103,103,153);
  public static Color fonsBoto = new Color(240,210,160); //marró més fosquet
  public static Font fontBoto = new Font("Arial",0,11);
  public static Color colorTextBoto = Color.BLACK;


  static public Color colorBackgroundFrame = new Color(240,210,160); //marró més fosquet
  static public Color colorForegroundText = new Color(0,0,100); //blau fosc

  static public Font fontTitol = new Font("Arial Narrow",Font.PLAIN,15);

  static public Font fontTextItalic = new Font("Arial",Font.ITALIC,12);

  static public Font fontTextPersonalData = new Font("Arial",Font.PLAIN,12);
  static public Font fontTitols = new Font("Dialog",Font.PLAIN,12);

  /**
   * Do not use it
   */
  public UtilsGUI() {
  }


  /**
   * Create a button with a pre-defined look-and-feel
   * @param text String Text to put inside the button
   * @param icon String Image embedded into the button
   * @return JButton initialised
   */
  public static JButton createButton(String text, String icon) {
    ImageIcon iconRefresh = new ImageIcon(icon);
    JButton jButton = new JButton(iconRefresh);
    jButton.setBorderPainted(false);
    jButton.setBackground(fonsBoto);
    jButton.setForeground(colorTextBoto);
    jButton.setFont(fontBoto);
    jButton.setText(text);
    jButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
    jButton.setEnabled(true);
    return jButton;
  }

  /**
   * Default look and feel titled border used in all interfaces.
   * @param message String Title to put in the top of the border
   * @return Border tunned accurately
   */
  public static Border createBorder(String message) {
    return new TitledBorder(
      new EtchedBorder(), message, 1, 2, fontTextMessages);
  } //end of createBorder


  public static JTextArea createTextArea() {
    JTextArea result = new JTextArea();
    result.setBorder(new BevelBorder(1));
    result.setBackground(fonsAreaTreball);
    result.setForeground(colorText);
    result.setName("Information area");
    result.setEditable(false);
    result.setSelectedTextColor(Color.BLUE);
    result.setFont(UtilsGUI.fontTextMessages);
    return result;
  }


  /**
   * We write the string specified into a file.
   * @param content String to write
   * @param file Pathname of the file
   */
  public static void writeFile(String content, File file) throws IOException {
    StringBuffer sb = new StringBuffer(content);
    PrintStream outFile = new PrintStream(new FileOutputStream(file));
    for (int i = 0; i < content.length(); i++) {
      outFile.print(sb.charAt(i));
    }
    //    System.out.println(content.length()+" characters write");
  }

} //endof class UtilsGUI
