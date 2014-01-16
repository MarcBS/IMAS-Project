package sma.ontology;

import java.io.Serializable;


/**
 *  	- int Row
 *  	- int Column
 *  	- Cell with all information
 * @author Alex Pardo Fernadnez
 *
 */
public class AuxGarbage implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3236144689670845921L;
	private int row;
	private int column;
	private Cell info;
	
	public AuxGarbage(int r, int c, Cell info){
		this.row = r;
		this.column = c;
		this.info = info;
	}

	/**
	 * @return the row
	 */
	public int getRow() {
		return row;
	}

	/**
	 * @param row the row to set
	 */
	public void setRow(int row) {
		this.row = row;
	}

	/**
	 * @return the column
	 */
	public int getColumn() {
		return column;
	}

	/**
	 * @param column the column to set
	 */
	public void setColumn(int column) {
		this.column = column;
	}

	/**
	 * @return the info
	 */
	public Cell getInfo() {
		return info;
	}

	/**
	 * @param info the info to set
	 */
	public void setInfo(Cell info) {
		this.info = info;
	}
	
	public String getGarbageType(){
		return String.valueOf(this.info.getGarbageType());
	}
	
}
