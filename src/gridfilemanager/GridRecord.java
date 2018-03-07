/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gridfilemanager;

 /*
 * Sean Schlaefli
 * CS157B
 * Pollet
 * HW2
 * GridRecord.java
 */

public class GridRecord {
    public String label;
    public GridPoint point;

    public GridRecord(String label, GridPoint point) {
	this.label = label;
	this.point = point;       
    }

    public String toString() {
	return label + point.toString();
    }
}
