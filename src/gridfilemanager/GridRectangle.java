/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gridfilemanager;

/**
 *
 * @author seanschlaefli
 */
public class GridRectangle {
    
    public float highX;
    public float lowX;
    public float highY;
    public float lowY;
    
    public GridRectangle(float highX, float lowX, float highY, float lowY) {
        this.highX = highX;
        this.lowX = lowX;
        this.highY = highY;
        this.lowY = lowY;
    }
    
    public GridRectangle(GridPoint pt1, GridPoint pt2) {
        if (pt1.x >= pt2.x) {
            highX = pt1.x;
            lowX = pt2.x;
        } else {
            highX = pt2.x;
            lowX = pt1.x;
        }
        
        if (pt1.y >= pt2.y) {
            highY = pt1.y;
            lowY = pt2.y;
        } else {
            highY = pt2.y;
            lowY = pt1.y;
        }
    }
    
    public boolean isInsideRect(GridPoint p) {
        int retLowX = Float.compare(p.x, lowX);
        int retHighX = Float.compare(p.x, highX);
        int retLowY = Float.compare(p.y, lowY);
        int retHighY = Float.compare(p.y, highY);
        return ( (retLowX >= 0) && (retHighX <= 0) &&
               (retLowY >= 0)   && (retHighY <= 0) );
    }
    
    
    public boolean isInsideX(float value) {
        int retLow = Float.compare(value, lowX);
        int retHigh = Float.compare(value, highX);
        return (retLow >= 0) && (retHigh <= 0);
    }
    
    
    public boolean isInsideY(float value) {
        int retLow = Float.compare(value, lowY);
        int retHigh = Float.compare(value, highY);
        return (retLow >= 0) && (retHigh <= 0);
    }
    
    
    public String toString() {
        return "(" + highX + "," + lowX + "," + highY + "," + lowY + ")";
    }
}
