package sma.ontology;

import java.awt.Point;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * @author Marc Bola�os Sol�
 * 
 * Representation of a delimiting zone for a ScoutAgent:
 * 
 * UL -------->
 * | 
 * |
 * |
 * |
 * v 		 BR
 * 
 * @param UL Point represents the upper left corner (column, row)
 * @param BR Point represents the bottom right corner  (column, row) 
 * @param buildings ArrayList<Point> stores all the buildings' positions contained in this delimiting zone.
 * Always with respect to the point (0,0) on the original map (upper left).
 */
public class DelimitingZone implements Serializable{
	
	private Point UL;
	private Point BR;
	private Point UR;
	private Point BL;
	// Buildings contained into this delimiting zone
	private ArrayList<Point> buildings;
	
	/**
	 * Constructor of the Delimiting Zone, where ul = upper left corner 
	 * and br = bottom right corner.
	 * 
	 * @param ulRow
	 * @param ulCol
	 * @param brRow
	 * @param brCol
	 */
	public DelimitingZone(int ulRow, int ulCol, int brRow, int brCol){
		UL = new Point(ulCol, ulRow);
		BR = new Point(brCol, brRow);
		BL = new Point(ulCol, brRow);
		UR = new Point(brCol, ulRow);
		buildings = null;
	}
	
	
	/**
	 * Returns all the positions of the buildings contained into this delimiting zone.
	 * 
	 * @return ArrayList<Point> with columns = x and rows = y.
	 */
	public ArrayList<Point> getBuildingsPositions(){
		return buildings;
	}
	
	
	public Point getBR(){
		return BR;
	}
	
	public Point getUL(){
		return UL;
	}
	public Point getBL(){
		return BL;
	}
	
	public Point getUR(){
		return UR;
	}
	
	
	/**
	 * Initializes the buildings list with the original map (this must only be executed when 
	 * the delimiting zone is the complete map).
	 * 
	 * @param auxInfo AuxInfo of the original map.
	 */
	public void setBuildings(AuxInfo auxInfo) throws Exception {
		if(buildings == null){
			buildings = new ArrayList<Point>();
			Cell[][] map = auxInfo.getMap();
			for(int i = 0; i < auxInfo.getMapColumns(); i++){
				for(int j = 0; j < auxInfo.getMapRows(); j++){
					
					// If one of the cells is a building we store it into the buildings ArrayList.
					Cell c = map[i][j];
					if(c.getCellType() == Cell.BUILDING){
						buildings.add(new Point(i, j));
					}
					
				}
			}
		} else {
			throw new Exception("Illegal buildings initialization, they were already set.");
		}
	}
	
	
	/**
	 * Initializes the buildings list for the current delimiting zone only 
	 * in case this delimiting zone is a sub-divided zone.
	 * 
	 * @param buildingsList ArrayList with all the buildings that might 
	 * 		be contained in the current zone.
	 */
	private void setBuildings(ArrayList<Point> buildingsList){
		Iterator<Point> iter = buildingsList.iterator();
		this.buildings = new ArrayList<Point>();
		while(iter.hasNext()){
			Point p = (Point) iter.next();
			if(p.x >= UL.x && p.x <= BR.x && p.y <= BR.y && p.y >= UL.y){
				this.buildings.add(new Point(p.x, p.y));
			}
		}
	}
	
	
	/**
	 * Divides the current delimiting zone into two equally sized zones.
	 * 
	 * @return DelimitingZone[] with length = 2 and with the containing buildings stored in them.
	 */
	public DelimitingZone[] divideZone(){
		DelimitingZone[] dz = new DelimitingZone[2];
		
		int width = BR.x - UL.x + 1;
		int height = BR.y - UL.y + 1;
		if(width > height){
			// divide by width
			int br_col_0 = UL.x + (int) Math.floor(width/2);
			int ul_col_1 = UL.x + (int) Math.ceil(width/2);
			if(br_col_0 == ul_col_1){
				br_col_0--;
			}
			dz[0] = new DelimitingZone(UL.y, UL.x, BR.y, br_col_0);
			dz[1] = new DelimitingZone(UL.y, ul_col_1, BR.y, BR.x);
		} else {
			// divide by height
			int br_row_0 = UL.y + (int) Math.floor(height/2);
			int ul_row_1 = UL.y + (int) Math.ceil(height/2);
			if(br_row_0 == ul_row_1){
				br_row_0--;
			}
			dz[0] = new DelimitingZone(UL.y, UL.x, br_row_0, BR.x);
			dz[1] = new DelimitingZone(ul_row_1, UL.x, BR.y, BR.x);
		}
		
		dz[0].setBuildings(this.buildings);
		dz[1].setBuildings(this.buildings);
		
		return dz;
	}

}
