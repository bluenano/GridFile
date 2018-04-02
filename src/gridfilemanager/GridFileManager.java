package gridfilemanager;

import java.sql.*;
import java.io.*;
import java.util.*;

public class GridFileManager {
    
    public static final String C = "c";
    public static final String I = "i";
    public static final String L = "l";
    public static final String SQLITE = "jdbc:sqlite:";
    public static final String PATH = "/Users/seanschlaefli/NetbeansProjects/GridFileManager/src/gridfilemanager";
    
    public Connection conn; 
    
    
    public GridFileManager(String databaseName) {
        conn = establishSqliteConnection(databaseName);
        String gridFile = "CREATE TABLE IF NOT EXISTS GRID_FILE (\nID INTEGER PRIMARY KEY,\n" 
                        + "NAME VARCHAR(64),\nNUM_BUCKETS INTEGER);";
        String gridX = "CREATE TABLE IF NOT EXISTS GRIDX (\nGRID_FILE_ID INTEGER PRIMARY KEY,\n"
                     + "LOW_VALUE INTEGER,\nHIGH_VALUE INTEGER,\nNUM_LINES INTEGER);";
        String gridY = "CREATE TABLE IF NOT EXISTS GRIDY (\nGRID_FILE_ID INTEGER PRIMARY KEY,\n"
                     + "LOW_VALUE INTEGER,\nHIGH_VALUE INTEGER,\nNUM_LINES INTEGER);";
        String gridFileRow = "CREATE TABLE IF NOT EXISTS GRID_FILE_ROW (\nGRID_FILE_ID INTEGER,\n"
                           + "BUCKET_ID INTEGER,\nX REAL,\nY REAL,\nLABEL CHAR(16),\n"
                           + "PRIMARY KEY (GRID_FILE_ID, BUCKET_ID, LABEL));";
        createTable(gridFile);
        createTable(gridX);
        createTable(gridY);
        createTable(gridFileRow);
    }
    
    
    private Connection establishSqliteConnection(String db) {
        try {
            return DriverManager.getConnection(SQLITE + PATH + db);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }
    
    
    public void createTable(String sql) {
        try {
            Statement createTable = conn.createStatement();
            createTable.execute(sql);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
    
    
    public boolean createGridFile(String fileName, int lowX, int highX,
                                         int numLinesX, int lowY, int highY,
                                         int numLinesY, int numBuckets) {
        try {
            Statement insert = conn.createStatement();         
            insert.execute(createGridFileInsert(fileName, numBuckets));   
            insert.execute(createGridXInsert(lowX, highX, numLinesX)); 
            insert.execute(createGridYInsert(lowY, highY, numLinesY));
            return true;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return false;
    }
    
    
    public boolean add(String fileName, GridRecord record) {
        if (record != null) {
            try {
                Statement stmt = conn.createStatement();
                ResultSet r = stmt.executeQuery(createGridFileSelect(fileName));                
                int id = r.getInt("ID");
                int buckets = r.getInt("NUM_BUCKETS");
                float x = findGridLine(id, record.point.x, "GRIDX");
                float y = findGridLine(id, record.point.y, "GRIDY");
                int bucket = hash(Float.floatToIntBits(x), Float.floatToIntBits(y), buckets);
                return stmt.execute(createGridRowInsert(id, bucket, record));                
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        }
        return false;
    }
    
    
    public GridRecord[] lookup(String fileName, GridPoint pt1, GridPoint pt2,
                               int limit_offset, int limit_count) {
        try {
            GridRectangle rect = new GridRectangle(pt1, pt2);
            Statement stmt = conn.createStatement();
            ResultSet r = stmt.executeQuery(createGridFileSelect(fileName));
            int id = r.getInt("ID");
            float[] xGridLines = findGridLines(id, rect.lowX, rect.highX, "GRIDX"); 
            float[] yGridLines = findGridLines(id, rect.lowY, rect.highY, "GRIDY");
            r = stmt.executeQuery(createLookupSelect(xGridLines, yGridLines));
            ArrayList<GridRecord> records = new ArrayList<GridRecord>();
            while (r.next()) {
                String label = r.getString("LABEL");
                float x = r.getFloat("X");
                float y = r.getFloat("Y");
                GridPoint p = new GridPoint(x,y);
                if (rect.isInsideRect(p)) {
                    GridRecord rec = new GridRecord(label, p);
                    records.add(rec);
                }
            }
            return validateGridRecords(records, limit_offset, limit_count);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }
    
    private int hash(int x, int y, int buckets) {
        int xHash = hash(x, buckets);
        int yHash = hash(y, buckets);
        return hash(xHash+yHash, buckets);
    }
    
    
    // golden ratio hash function
    private int hash(int value, int m) {
        double C = Math.pow(5.0, (1.0/2.0));
        return (int) (Math.floor(m * ((value * C)- Math.floor(value * C)) ));
    }
    
    
    private float findGridLine(int id, float value, String table) {
        try {
            Statement stmt = conn.createStatement();
            ResultSet r = stmt.executeQuery(createGridDimensionSelect(id, table));
            int low = r.getInt("LOW_VALUE");
            int high = r.getInt("HIGH_VALUE");
            int numLines = r.getInt("NUM_LINES");
            return searchForGridLine(value, low, high, numLines);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return 0.0f;
    }
    
    
    private float[] findGridLines(int id, float rectLow, float rectHigh, String table) {
        try {
            Statement stmt = conn.createStatement();
            ResultSet r = stmt.executeQuery(createGridDimensionSelect(id, table));
            int high = r.getInt("HIGH_VALUE");
            int low = r.getInt("LOW_VALUE");
            int numLines = r.getInt("NUM_LINES");
            return searchForGridLines(high, low, numLines, rectLow, rectHigh);            
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }
    
    
    
    private float[] searchForGridLines(int high, int low, int numLines, float rectLow, float rectHigh) {
        int d = high-low;
        float increment = (float) d / (numLines+1);
        float min = (float) low;
        float max = (float) high;
        for (int i = 0; i < numLines+2; i++) {
            float current = low + i*increment;
            int retLow = Float.compare(current, rectLow);
            if (retLow <= 0) {
                min = current;
            } else {
                float retHigh = Float.compare(current, rectHigh);
                if (retHigh >= 0) {
                    max = current;
                    float[] lines = new float[2];
                    lines[0] = min;
                    lines[1] = max;
                    return lines;
                }
            }
        }
        return null;
    }
    
    
    // could implement binary search instead of linear search
    private float searchForGridLine(float value, int low, int high, int numLines) {
        int d = high-low;
        float increment = (float) d / (numLines+1);
        for (int i = 0; i < numLines+1; i++) {
            float start = low + i*increment;
            float end = low + (i+1)*increment;
            float retLow = Float.compare(value, start);
            float retHigh = Float.compare(value, end);
            if (retLow >= 0 && retHigh < 0) return start;
        }
        return (float) high;
    }
        
    
    
    private ArrayList<Float> calculateGridLines(float start, float end, float increment) {
        ArrayList<Float> lines = new ArrayList<Float>();
        float current = start;
        int ret;
        while ((ret = Float.compare(current, end)) <= 0) {
            lines.add(current);
            current += increment;
        }
        return lines;
    }
    
    
    private GridRecord[] validateGridRecords(ArrayList<GridRecord> records, 
                                        int limit_offset, int limit_count) {
        int leftOver = records.size() - (limit_offset-1);
        int size = (leftOver > limit_count) ? limit_count : leftOver;
        GridRecord[] returnedRecords = new GridRecord[size];
        for (int i = 0; i < size; i++) {
            returnedRecords[i] = records.get(i+limit_offset-1);
        }
        return returnedRecords;
    }
    
    
    private String createGridFileInsert(String fileName, int numBuckets) {
        return "INSERT INTO GRID_FILE (NAME, NUM_BUCKETS)\n" 
             + "VALUES ('" + fileName 
             + "', " + Integer.toString(numBuckets) + ");";
    }
    
    
    private String createGridXInsert(int lowX, int highX, int numLines) {
        return "INSERT INTO GRIDX (LOW_VALUE, HIGH_VALUE, NUM_LINES)\n"
             + "VALUES ("+ Integer.toString(lowX)
             + ", " + Integer.toString(highX) + ", " + Integer.toString(numLines) + ");";
    }
    
    
    private String createGridYInsert(int lowY, int highY, int numLines) {
        return "INSERT INTO GRIDY (LOW_VALUE, HIGH_VALUE, NUM_LINES)\n"
             + "VALUES (" + Integer.toString(lowY)
             + ", " + Integer.toString(highY) + ", " + Integer.toString(numLines) + ");";
    }
    
    
    private String createGridFileSelect(String fileName) {
        return "SELECT ID, NUM_BUCKETS\nFROM GRID_FILE\nWHERE NAME = '" + fileName + "';";
    }
    
    
    private String createGridRowInsert(int id, int bucket, GridRecord record) {
        return "INSERT INTO GRID_FILE_ROW (GRID_FILE_ID, BUCKET_ID, X, Y, LABEL)\n"
             + "VALUES (" + Integer.toString(id) + ", " + Integer.toString(bucket)
             + ", " + Float.toString(record.point.x) 
             + ", " + Float.toString(record.point.y) + ", '" + record.label
             + "');";
    }
    
    
    private String createGridDimensionSelect(int id, String table) {
        return "SELECT LOW_VALUE, HIGH_VALUE, NUM_LINES\nFROM " + table
             + "\nWHERE GRID_FILE_ID = " + Integer.toString(id) + ";";
    }
    
    
    private String createIdSelect(String fileName) {
        return "SELECT ID\nFROM GRID_FILE\nWHERE NAME=" + "'" + fileName + "';";
    }
    
    
    private String createLookupSelect(float[] x, float[] y) {
        return "SELECT *\nFROM GRID_FILE_ROW\nWHERE x >= " 
               + Float.toString(x[0]) + " AND x <= " 
               + Float.toString(x[1]) + " AND y >= "
               + Float.toString(y[0]) + " AND y <= "
               + Float.toString(y[1]) + ";";
    }
    
    
    public void executeInstruction(String[] cmd) {
        if (cmd.length > 0) {
            
            if (cmd[0].equals(C) && cmd.length == 9) {
                    createGridFile(cmd[1], Integer.parseInt(cmd[2]),
                                   Integer.parseInt(cmd[3]),
                                   Integer.parseInt(cmd[4]),
                                   Integer.parseInt(cmd[5]),
                                   Integer.parseInt(cmd[6]),
                                   Integer.parseInt(cmd[7]),
                                   Integer.parseInt(cmd[8]));
                    
            } else if (cmd[0].equals(I) && cmd.length == 5) {
                float x = Float.parseFloat(cmd[3]);
                float y = Float.parseFloat(cmd[4]);
                add(cmd[1], new GridRecord(cmd[2], new GridPoint(x,y)));
                
            } else if (cmd[0].equals(L) && cmd.length == 8) {
                
                int limitOffset = Integer.parseInt(cmd[6]);
                int limitCount = Integer.parseInt(cmd[7]);
                float x1 = Float.parseFloat(cmd[2]);
                float y1 = Float.parseFloat(cmd[3]);
                float x2 = Float.parseFloat(cmd[4]);
                float y2 = Float.parseFloat(cmd[5]);
                GridRecord[] records = lookup(cmd[1], new GridPoint(x1,y1), 
                                              new GridPoint(x2,y2), limitOffset, 
                                              limitCount);               
            }
        }
    }
    
    
    public static void main(String[] args) {
        if (args.length == 2) {
            String db = args[0];
            String instructions = args[1];
            GridFileManager manager = new GridFileManager(db);
            
            File file = new File(instructions);
            try {
                Scanner in = new Scanner(file);
                while (in.hasNextLine()) {
                    String instruction = in.nextLine();
                    String[] tokens = instruction.split(" ");
                    manager.executeInstruction(tokens);
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }
    
}
