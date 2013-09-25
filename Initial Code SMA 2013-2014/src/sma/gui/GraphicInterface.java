package sma.gui;

import java.awt.*;
import javax.swing.*;
import java.awt.Font;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;
import javax.swing.*; // For JPanel, etc.
import java.awt.*;           // For Graphics, etc.
import java.awt.geom.*;      // For Ellipse2D, etc.

import sma.ontology.*;

/**
 * <p><B>Title:</b> IA2-SMA</p> *
 * <p><b>Description:</b> Practical exercise 2011-12. Recycle swarm.</p>
 * Main class of the graphical interface controlled by the Central Agent.
 * It offers several methods to show the changes of the agents within the
 * city., as well as an area with the logs and an area with the main statistical
 * results.
 * <p><b>Copyright:</b> Copyright (c) 2013</p>
 * <p><b>Company:</b> Universitat Rovira i Virgili (<a
 * href="http://www.urv.cat">URV</a>)</p>
 * @author David Isern & Joan Albert López
 * @version 2.0
 */
public class GraphicInterface extends JFrame {

  int inset = 50;

  private sma.gui.MapVisualizer jMapPanel;
  private sma.gui.LogPanel jLogPanel;
  private sma.gui.StatisticsPanel jStatisticsPanel;

  GridLayout gridMainLayout = new GridLayout();
  JTabbedPane jGameTabbedPane = new JTabbedPane();
  JPanel jGamePanel = new JPanel();

//  GridLayout gridLayoutPanelPartida = new GridLayout();

//  BorderLayout borderLayout1 = new BorderLayout();
//  JPanel jPanelStatistics = new JPanel();
//  JFileChooser jFileChooserLogFile = new JFileChooser();
//  JScrollPane jScrollPane1 = new JScrollPane();
//  JScrollPane jScrollPane2 = new JScrollPane();
//  JTextArea jTextArea1 = new JTextArea();
//  JTextArea jTextAreaStatistics = new JTextArea();
//  BorderLayout borderLayout2 = new BorderLayout();
//  JFileChooser jFileChooser1 = new JFileChooser();




//  private static JButton createButton(String text, String icon) {
//    ImageIcon iconRefresh = new ImageIcon(icon);
//    JButton jButton = new JButton(iconRefresh);
//    jButton.setBackground(colorBackgroundFrame);
//    jButton.setForeground(colorForegroundText);
//    jButton.setFont(fontText);
//    jButton.setText(text);
//    jButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
//    jButton.setEnabled(true);
//    return jButton;
//  }



  public GraphicInterface(InfoGame p) {
    try {
      jbInit();
      this.showGameMap(p.getInfo().getMap());
    }
    catch (Exception exception) {
      exception.printStackTrace();
    }
  }

  private void jbInit() throws Exception {

    try {
      UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
      //     UIManager.setLookAndFeel("com.sun.java.swing.plaf.motif.MotifLookAndFeel");
    } catch (Exception e) {
      System.err.println("-> We use the default L&F. Reason: "+e.toString());
    }

    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    setBounds( inset, inset, screenSize.width - inset * 2, screenSize.height - inset * 2 );
    //Quit this app when the big window closes.
    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        //System.exit(0);
        dispose();
      }
    });

    this.getContentPane().setLayout(gridMainLayout);
//    this.getContentPane().setBackground(Color.BLACK);
    this.setForeground(Color.WHITE);
//    this.setBackground(Color.BLACK);
    this.setTitle("Practical exercise AI2-MAS");

    gridMainLayout.setColumns(1);




    /****************************************************/

    Font fontLabels = new Font("Arial", Font.PLAIN, 12);
    Font fontTextArea = new Font("Arial", Font.PLAIN, 12);
    Font font = new Font("Arial", Font.PLAIN, 12);


    jGameTabbedPane.setTabPlacement(JTabbedPane.TOP);

    jGameTabbedPane.setBackground(UtilsGUI.colorBackgroundFrame);
    jGameTabbedPane.setForeground(UtilsGUI.colorForegroundText);
    jGameTabbedPane.setFont(new java.awt.Font("Arial", Font.PLAIN, 10));
    jGameTabbedPane.setBorder(BorderFactory.createEtchedBorder());
    jGameTabbedPane.setMinimumSize(new Dimension(640, 480));
    jGameTabbedPane.setPreferredSize(new Dimension(640, 480));


    /** Partida *****************************/
    jGamePanel = new JPanel(new GridLayout(1,1));
    jGamePanel.setBackground(UtilsGUI.colorBackgroundFrame);
    jGamePanel.setForeground(UtilsGUI.colorForegroundText);
    jGamePanel.setBorder(BorderFactory.createEtchedBorder());
    jGamePanel.setMinimumSize(new Dimension(640, 480));
    jGamePanel.setPreferredSize(new Dimension(640, 480));

    ImageIcon icon = new ImageIcon(UtilsGUI.pathIconPartida);
    jGameTabbedPane.addTab("Partida", icon, jGamePanel);


    /** Logs *****************************/
    this.jLogPanel = new LogPanel();
    icon = new ImageIcon(UtilsGUI.pathIconLogs);
    jGameTabbedPane.addTab("Logs", icon, this.jLogPanel);

    this.jLogPanel.showMessage("Initializing components ....\n");

    /** Statistics **********************/
    this.jStatisticsPanel = new StatisticsPanel();
    jGameTabbedPane.addTab("Statistics", icon, this.jStatisticsPanel);

    this.jStatisticsPanel.showMessage("All tabs initialized successfully!");

    /** TABBED PANE *********************/
    this.getContentPane().add(jGameTabbedPane);

  } //endof jbInit()


  /**
   * Repinta *tot* un tauler de caselles
   * @param t Casella[][] tauler a mostrar
   * @see sma.ontology.Cell
   */
  public void showGameMap(Cell[][] t) {
    this.jMapPanel = new MapVisualizer(t);
    this.jGamePanel.add(jMapPanel);
    this.jGamePanel.repaint();
  }

  /**
   * Mostra una cadena en el panell destinat a logs
   * @param msg String per mostrar
   */
  public void showLog(String msg) {
    this.jLogPanel.showMessage(msg);
  }


  /**
   * Mostra una cadena en el panell destinat a stadístiques
   * @param msg String per mostrar
   */
  public void showStatistics(String msg) {
    this.jStatisticsPanel.showMessage(msg);
  }



  public static void main(String[] args) {
    GraphicInterface graphicinterface = new GraphicInterface(null);
    graphicinterface.setVisible(true);
  }


}
