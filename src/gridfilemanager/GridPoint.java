/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gridfilemanager;


public class GridPoint {
    public float x;
    public float y;

    public GridPoint(float x, float y) {
	this.x = x;
	this.y = y;
    }

    public String toString() {
	return "(" + x + "," + y + ")";       
    }
}
