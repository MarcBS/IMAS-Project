package sma.ontology;

import java.io.*;
import java.util.StringTokenizer;

import sma.gui.UtilsGUI;
import java.util.*;
/**
 * <p><B>Title:</b> IA2-SMA</p>
 * <p><b>Description:</b> Practical exercise 2013-14. Recycle swarm.</p>
 * Information about the current game. This object is initialized from a file.
 * <p><b>Copyright:</b> Copyright (c) 2013</p>
 * <p><b>Company:</b> Universitat Rovira i Virgili (<a
 * href="http://www.urv.cat">URV</a>)</p>
 * @author David Isern & Joan Albert L���pez
 * @see sma.CoordinatorAgent
 * @see sma.CentralAgent
 */
public class InfoGame implements java.io.Serializable {

  private AuxInfo info;  //Object sent to the CoordinatorAgent during the initialization
  private List<Cell> buildingsGarbage; //List of the undiscovered buildings containg garbage. 
  									   //It cannot be sent to the CoordinatorAgent.
  // a list containing all the buildings
  
  private int turn=0;
  private int gameDuration;
  private long timeout;
  private float probGarbage;
 
  static private boolean DEBUG = false;

  public InfoGame() {
	  info=new AuxInfo();
	  buildingsGarbage=new java.util.ArrayList<Cell>();
  }

  public AuxInfo getInfo() {
	return info;
  }

  public void setInfo(AuxInfo info) {
	this.info = info;
  }

  private void showMessage(String s) {
    if(this.DEBUG)
      System.out.println(s);
  }
  
  public int getTurn() { return this.turn; }
  public void incrTurn() { this.turn++; }
  public int getGameDuration() {return this.gameDuration;}
  public void setGameDuration(int d) {this.gameDuration = d;}
  public long getTimeout() {return this.timeout;}
  public void setTimeout(long n) {this.timeout = n;}	
  public float getProbGarbage() {return probGarbage;}
  public void setProbGarbage(float probGarbage) {this.probGarbage = probGarbage;}
  public boolean isEndGame() { return (this.turn>=this.getGameDuration()); }

//  /**
//   * We write the string specified into a file.
//   * @param content String to write
//   * @param file Pathname of the file
//   * @return Nothing
//   */
//  private void writeFile(String content, File file) throws IOException {
//    StringBuffer sb = new StringBuffer(content);
//    PrintStream outFile = new PrintStream(new FileOutputStream(file));
//    for (int i = 0; i < content.length(); i++) {
//      outFile.print(sb.charAt(i));
//    }
//    //    System.out.println(content.length()+" characters write");
//  }
  public void writeGameResult(String fileOutput, Cell[][] t) throws IOException, Exception {
    File file= new File(fileOutput);
    String content = "" + this.getGameDuration()+"\n"+this.getTimeout()+"\n";
    for(int r=0; r<t.length; r++) {
      for(int c=0; c<t[0].length; c++) {
        Cell ca = t[r][c];
        content = content + Cell.getCellType(ca.getCellType());
        if(ca.getCellType()==Cell.BUILDING)
          content = content + ca.getGarbageUnits();
        content+="\t";
      }
      content+="\n";
    }
    UtilsGUI.writeFile(content,file);
    showMessage("File written");
  }


  public void readGameFile (String file) throws IOException,Exception {
	FileReader fis = new FileReader(file);
    BufferedReader dis = new BufferedReader(fis);
    int NROWS = 0, NCOLS = 0;
    
	String dades = dis.readLine(); StringTokenizer st = new StringTokenizer(dades, " ");
	this.setProbGarbage(Float.parseFloat(st.nextToken()));
	dades = dis.readLine(); st = new StringTokenizer(dades, " ");
	this.setGameDuration(Integer.parseInt(st.nextToken()));
	dades = dis.readLine(); st = new StringTokenizer(dades, " ");
	this.setTimeout(Long.parseLong(st.nextToken()));
	dades = dis.readLine(); st = new StringTokenizer(dades, " ");
	NROWS = Integer.parseInt(st.nextToken());
	info.setMapRows(NROWS); //Save into AuxInfo the number of the rows in the map
	dades = dis.readLine(); st = new StringTokenizer(dades, " ");
	NCOLS = Integer.parseInt(st.nextToken());
	info.setMapColumns(NROWS); //Save into AuxInfo the number of the columns in the map
	this.info.map = new Cell[NROWS][NCOLS];
	dades = dis.readLine(); st = new StringTokenizer(dades, " ");
	this.info.setNumScouts(Integer.parseInt(st.nextToken()));
	dades = dis.readLine(); st = new StringTokenizer(dades, " ");
	this.info.setNumHarvesters(Integer.parseInt(st.nextToken()));
	dades = dis.readLine(); st = new StringTokenizer(dades, " ");
	dades = st.nextToken(); st = new StringTokenizer(dades, ",");
	this.info.setTypeHarvesters(new String[this.info.getNumHarvesters()]);
	this.info.setCapacityHarvesters(new int[this.info.getNumHarvesters()]);
	for (int i=0; i<this.info.getNumHarvesters(); i++) {
		String str = st.nextToken();
		StringTokenizer st2 = new StringTokenizer(str, "-");
		this.info.getTypeHarvesters()[i] = st2.nextToken();
		this.info.getCapacityHarvesters()[i] = Integer.parseInt(st2.nextToken());
	}
	int col=0, row=0;
	//Llegim mapa
	while ((dades = dis.readLine()) != null) {
		col=0;
		st = new StringTokenizer(dades, " ");
		while (st.hasMoreTokens()){
			String str = st.nextToken();
			if(str.equals("s")) this.info.map[row][col]= new Cell(Cell.STREET);
			else{
				if(str.charAt(0)=='b') {
					this.info.map[row][col]= new Cell(Cell.BUILDING);
					
					// adds the building into the list
					this.info.addBuilding(this.info.map[row][col]);
					//buildings.add(this.info.map[row][col]);
					
					
					if (str.length()>1){
						String type = str.substring(str.length()-2, str.length()-1);
						int units = Integer.parseInt(str.substring(2, str.length()-2));
						//We create a cell with the information about garbage that is stored
						//in the private list buildingsGarbage. The public map does not contain
						//the information about garbage, until it is discovered.
						Cell garbage = new Cell(Cell.BUILDING);
						garbage.setRow(row); garbage.setColumn(col);
						garbage.setGarbageUnits(units);
						garbage.setGarbageType(type.charAt(0));
						buildingsGarbage.add(garbage); 
						//this.info.map[row][col].setGarbageUnits(units);
						//this.info.map[row][col].setGarbageType(type.charAt(0));
					}					
				}else{
					this.info.map[row][col]= new Cell(Cell.RECYCLING_CENTER);
					if (str.length()>1){
						str = str.substring(2, str.length()-1);
						StringTokenizer st2 = new StringTokenizer(str, ",");
						int[] points = new int[4];
						points[0] = Integer.parseInt(st2.nextToken());
						points[1] = Integer.parseInt(st2.nextToken());
						points[2] = Integer.parseInt(st2.nextToken());
						points[3] = Integer.parseInt(st2.nextToken());
						this.info.map[row][col].setGarbagePoints(points);
						this.info.addRecyclingCenter(this.info.map[row][col]);
					}					
				}
			}
			this.info.map[row][col].setRow(row);
			this.info.map[row][col].setColumn(col);
			showMessage(((Cell)info.map[row][col]).toString() );
			col++;
		}
		row++;		
	}
	
  }

public List<Cell> getBuildingsGarbage() {
	return buildingsGarbage;
}

public void setBuildingsGarbage(List<Cell> buildingsGarbage) {
	this.buildingsGarbage = buildingsGarbage;
}

/**
 * Returns a list with all the buildings
 * @return
 */
public List<Cell> getAllBuildings(){
	return this.info.getAllBuildings();
}

} //endof class InfoPartida
