/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package eu.hydrologis.jgrass.libs.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.zip.DataFormatException;

import eu.hydrologis.jgrass.libs.map.attribute.AttributeTable;
import eu.hydrologis.jgrass.libs.map.color.ColorMapBuffer;
import eu.hydrologis.jgrass.libs.map.color.ColorRule;
import eu.hydrologis.jgrass.libs.map.color.ColorTable;
import eu.hydrologis.jgrass.libs.map.color.GrassColorTable;
import eu.hydrologis.jgrass.libs.messages.Messages;
import eu.hydrologis.jgrass.libs.object.GDataObject;
import eu.hydrologis.jgrass.libs.region.JGrassRegion;
import eu.hydrologis.jgrass.libs.utils.Format;
import eu.hydrologis.jgrass.libs.utils.JGrassConstants;
import eu.hydrologis.jgrass.libs.utils.monitor.IProgressMonitorJGrass;

/**
 * This reads any native Raster map format. It supports integer, float and double and the
 * transformation of any of those into int, float and double matrixes, as well as in the ByteBuffers
 * of the same tipes.
 */
public class GrassAsciiRasterReader extends MapReader {

    private String LASTFOLDER = JGrassConstants.GRASSASCIIRASTER;
    /*
     * 0 for 1-byte integer, 1 for 2-byte integer and so on, -1 for float, -2 for double
     */
    private int rasterMapType = -9999;
    private int numberOfBytesPerValue = -9999;

    private Object novalue = new Double(Double.NaN);

    /**
     * Comment for <code>matrixType</code> this defines the tipe of matrix to return: 0 = normal
     * type[][] matrix 1 = type[][] matrix indexed from [1][1] 2 = transposed of the original
     * type[][] matrix 3 = transposed of the original type[][] matrix and indexed by [1][1]
     */
    private int matrixType = 0;

    /**
     * Comment for <code>theFilePath</code> this is the complete path including the filename
     */
    private String theFilePath = null;
    // private String theNullFilePath = null;
    private String filename = null;
    private ByteBuffer rasterByteBuffer = null;

    private boolean moreData = false;
    private boolean hasChanged = true;

    // private ByteBuffer rowCache = null;
    private int rowCacheRow = -1;
    private int firstDataRow = -1;

    private final double[] range = new double[]{1000000.0, -1000000.0}; // min,
    // max

    private final double[] dataRange = new double[]{Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY};

    private double[][] datamatrix = null;

    /* Storage for cell category descriptive information */
    private AttributeTable attTable = null;
    private AttributeTable legendAttribTable = null;

    private ByteBuffer cmapBuffer = ByteBuffer.allocate(1);

    private String mapsetPath;

    /** Creates a new instance of GrassRasterReader */
    public GrassAsciiRasterReader() {
        super(MapReader.RASTER_READER);
    }

    /*
     * (non-Javadoc)
     * 
     * @see eu.hydrologis.jgrass.libs.io.MapReader#open(java.lang.String)
     */
    public boolean open( String mapPath ) {
        File mapFile = new File(mapPath);
        File dummyMapsetFile = mapFile.getParentFile();
        File dummyLocationFile = dummyMapsetFile.getParentFile();

        LASTFOLDER = "";

        return open(mapFile.getName(), dummyLocationFile.getAbsolutePath(), dummyMapsetFile
                .getName());
    }

    /*
     * (non-Javadoc)
     * 
     * @see jgrass.io.MapReader#open(java.lang.String, jgrass.map.Mapset) this opens checks for the
     *      file existence, sets the active and map regions, defines the maptype, opens the map and
     *      extracts the header to extract the rowaddresse and check if everything is alright
     */
    public boolean open( String fileName, String locationPath, String mapsetName ) {
        filename = fileName;
        this.mapsetPath = locationPath + File.separator + mapsetName;
        theFilePath = mapsetPath + File.separator + LASTFOLDER + File.separator + filename;

        if (hasChanged) {
            /*
             * Read the header of the map
             */
            if (!getRasterMapTypesAndData()) {
                return false;
            }

            /* Ok. Get ready to read data */
            moreData = true;
        }

        hasChanged(false);

        return true;
    }

    public void close() {
        hasChanged(true);
        moreData = true;
    }

    /**
     * 
     */
    private ColorTable loadColorTable() {
        /* Read the color map */
        try {
            colorTable = null;
            colorTable = new GrassColorTable(mapsetPath, filename, dataRange);
            return colorTable;
        } catch (IOException e) {
        }
        return null;
    }

    /**
     * 
     */
    public AttributeTable loadAttributeTable( String themapsetPath, String thefilename ) {
        /*
         * ascii files have no category file, therefore just instantiate an empty attribute table
         */
        attTable = new AttributeTable();
        return attTable;
    }

    /*
     * (non-Javadoc)
     * 
     * @see jgrass.io.MapReader#hasMoreData() checks if there are more data and if there are not, it
     *      stops
     */
    public boolean hasMoreData( IProgressMonitorJGrass monitor ) {
        if (dataWindow != null && moreData == true) {
            /*
             * If the parameter 'donotloadcolortable' has not been set then load the color table
             * here and when reading the data row by row we calculate the color map values and store
             * it in the color map byte buffer. If there is no color table then we create a default
             * one after we have read all the data and know what the data range is. This is done at
             * the end of this function.
             */
            if (cmapBuffer != null) {
                cmapBuffer = null;
                cmapBuffer = ByteBuffer.allocate(dataWindow.getRows() * dataWindow.getCols() * 4);
                if (colorTable == null)
                    colorTable = loadColorTable();
                if (colorTable.isEmpty())
                    colorTable = null;
            }
            /* Allocate the space for the map data. */
            int bufferSize = dataWindow.getRows() * dataWindow.getCols() * numberOfBytesPerValue;
            rasterByteBuffer = null;
            // rasterByteBuffer = ByteBuffer.allocate(bufferSize*
            // numberOfBytesPerValue);

            // Moovida: I never remember why here we multiply per
            // numberOfBytesPerValue
            // once more??????
            rasterByteBuffer = new GDataObject.GridData(bufferSize * 4
            // * numberOfBytesPerValue
                    , GDataObject.USE_BACKING_FILE).getReadBuffer();
            rasterByteBuffer.rewind();

            try {
                /* Byte array that will hold a complete null row */
                /* The rowDataArray holds the unpacked row data */
                byte[] rowDataCache = new byte[dataWindow.getCols() * numberOfBytesPerValue];
                /* The rowColorDataArray holds the unpacked row color data */
                byte[] rowColorDataCache = new byte[dataWindow.getCols() * 4];

                rowCacheRow = -1;
                firstDataRow = -1;
                int rowindex = -1;
                /* Get a local reference to speed things up */
                int filerows = fileWindow.getRows();
                double filenorth = fileWindow.getNorth();
                double filensres = fileWindow.getNSResolution();
                double datansres = dataWindow.getNSResolution();
                double datanorth = dataWindow.getNorth();
                /* Iterate through all the rows of the data window. */
                // int lastpos = 0;
                monitor.beginTask(Messages.getString("GrassRasterReader.readingraster"), dataWindow //$NON-NLS-1$
                        .getRows());
                int stepRowsForPercentage = dataWindow.getRows() / 100;
                for( double row = 0; row < dataWindow.getRows(); row++ ) {

                    if (row % stepRowsForPercentage == 0.0)
                        monitor.worked(stepRowsForPercentage);
                    /*
                     * Calculate the map file row for the current data window row. The row value is
                     * adjusted by half the datawindow resolution to tale the center of the cell.
                     */
                    int filerow = (int) ((filenorth - (datanorth - (row * datansres))) / filensres);
                    // int filerow = (int) ((filenorth - (datanorth - (row *
                    // datansres + datansres2))) / filensres);
                    if (filerow < 0 || filerow >= filerows) {
                        /*
                         * If no data has been read yet, then increment first data row counter
                         */
                        if (firstDataRow == -1)
                            rowindex++;
                        /*
                         * Write a null row to the raster buffer. To speed things up the first time
                         * this is called it instantiates the buffer and fills it with null values
                         * that are reused the other times.
                         */
                        // if (nullRow == null) nullRow = initNullRow();
                        for( int i = 0; i < dataWindow.getCols(); i++ ) {
                            rasterByteBuffer.putDouble(Double.NaN);
                        }
                        // rasterByteBuffer.put(nullRow);
                    } else {
                        if (firstDataRow == -1)
                            firstDataRow = rowindex + 1;
                        /* Read row and put in raster buffer */
                        if (filerow == rowCacheRow) {
                            rasterByteBuffer.put(rowDataCache);
                            cmapBuffer.put(rowColorDataCache);
                        } else {
                            // readRasterRow(filerow, rowDataCache);
                            // rowCacheRow = filerow;
                            // rasterByteBuffer.put(rowDataCache);
                            readRasterRow(filerow, rowDataCache, rowColorDataCache);
                            rowCacheRow = filerow;
                            rasterByteBuffer.put(rowDataCache);
                            cmapBuffer.put(rowColorDataCache);
                        }
                    }
                    // System.out.println("FILEROWS="+filerows+",
                    // FILEROW="+filerow+", ROWCACHEROW="+rowCacheRow+",
                    // ROW_COUNTER="+row);
                }
                monitor.done();

                rowDataCache = null;
                rowColorDataCache = null;
                rowCacheRow = -1;
                System.gc();
            } catch (IOException e) {
                moreData = false;
            } catch (DataFormatException e) {
                moreData = false;
            }
            rasterByteBuffer.rewind();
        }

        /*
         * for (int i = 0; i < dataWindow.getRows(); i++) { for (int j = 0; j <
         * dataWindow.getCols(); j++) { System.out.print(rasterByteBuffer.getDouble() + " "); }
         * System.out.println(); } rasterByteBuffer.rewind();
         */

        return moreData;
    }

    /*
     * (non-Javadoc)
     * 
     * @see jgrass.io.MapReader#getNextData() returns the data in the required format
     */
    public Object getNextData() {
        moreData = false;
        int rows_x_cols = dataWindow.getRows() * dataWindow.getCols();

        /* Return data as type Vector */
        if (dataObject instanceof Vector) {
            // Not yet implemented
            dataObject = null;
        }
        /* Return data as type double[][] */
        else if (dataObject instanceof double[][]) {
            double[][] data = (double[][]) dataObject;
            if (data.length != rows_x_cols)
                dataObject = new double[dataWindow.getRows()][dataWindow.getCols()];
            getDoubleArray();
        }
        /* Return data as type float[][] */
        else if (dataObject instanceof float[][]) {
            float[][] data = (float[][]) dataObject;
            if (data.length != rows_x_cols)
                dataObject = new float[dataWindow.getRows()][dataWindow.getCols()];
            getFloatArray();
        }
        /* Return data as type int[][] */
        else if (dataObject instanceof int[][]) {
            int[][] data = (int[][]) dataObject;
            if (data.length != rows_x_cols)
                dataObject = new int[dataWindow.getRows()][dataWindow.getCols()];
            getIntArray();
        }
        /* Return data as type DoubleBuffer */
        else if (dataObject instanceof DoubleBuffer) {
            DoubleBuffer data = (DoubleBuffer) dataObject;
            if (data.capacity() != rows_x_cols)
                dataObject = ByteBuffer.allocate(rows_x_cols * 8).asDoubleBuffer();
            getDoubleBuffer();
        }
        /* Return data as type FloatBuffer */
        else if (dataObject instanceof FloatBuffer) {
            FloatBuffer data = (FloatBuffer) dataObject;
            if (data.capacity() != rows_x_cols)
                dataObject = ByteBuffer.allocate(rows_x_cols * 4).asFloatBuffer();
            getFloatBuffer();
        }
        /* Return data as type IntBuffer */
        else if (dataObject instanceof IntBuffer) {
            IntBuffer data = (IntBuffer) dataObject;
            if (data.capacity() != rows_x_cols)
                dataObject = ByteBuffer.allocate(rows_x_cols * 4).asIntBuffer();
            getIntBuffer();
        } else {
            // Don't know what top do yet, thro some exception
            dataObject = null;
        }
        return dataObject;
    }

    /*
     * (non-Javadoc)
     * 
     * @see jgrass.io.MapReader#getNextData() returns the data in the required format
     */
    public ColorMapBuffer getNextDataColor( int attColor ) throws IOException {
        moreData = false;

        if (colorTable != null && cmapBuffer != null && cmapBuffer.capacity() > 1) {
            ColorMapBuffer colorBuffer = new ColorMapBuffer();
            colorBuffer.setRowOffset(firstDataRow);
            colorBuffer.setRGBBuffer((ByteBuffer) cmapBuffer.position(firstDataRow * 4).flip());
            // colorBuffer.setRGBBuffer(cmapBuffer);
            return colorBuffer;
        }

        /*
         * Because color maps can be very large (especially with satellite images) they are not kept
         * around in memory.
         */
        if (colorTable == null) {
            colorTable = loadColorTable();
            if (colorTable == null)
                return null;
            if (colorTable.isEmpty())
                colorTable.createDefaultColorTable(dataRange);
        }

        ColorMapBuffer colorBuffer = new ColorMapBuffer();
        colorBuffer.setRowOffset(firstDataRow);
        int dataOffset = firstDataRow * dataWindow.getCols() * numberOfBytesPerValue;
        // interpolate the data to get the color map
        colorBuffer.setRGBBuffer((cmapBuffer = colorTable.interpolateColorMap(rasterMapType,
                rasterByteBuffer, dataOffset)));

        return colorBuffer;
    }

    /**
     * Create a string that defines how the legend will look.
     */
    public String getLegendString() {
        StringBuffer legend;

        /* If colour table has not been loaded then load it */
        if (colorTable == null)
            colorTable = loadColorTable();

        /* If attribute table not loaded then load it */
        if (legendAttribTable == null)
            legendAttribTable = loadAttributeTable(mapsetPath, filename);

        /*
         * If the attribute table has any values then this is a fixed color table
         */
        if (legendAttribTable.size() > 0) {
            legend = new StringBuffer(legendAttribTable.size() * 12);
            legend.append("raster");
            /* Iterate through all the category attribute values */
            Enumeration catagories = legendAttribTable.getCategories();
            while( catagories.hasMoreElements() ) {
                AttributeTable.CellAttribute catt = (AttributeTable.CellAttribute) catagories
                        .nextElement();
                byte[] clr = colorTable.getColor(catt.getLowcategoryValue());
                legend.append("|").append(catt.getText()).append("|#").append(
                        Format.sprintf("%02x", clr[0] & 0xff)).append(
                        Format.sprintf("%02x", clr[1] & 0xff)).append(
                        Format.sprintf("%02x", clr[2] & 0xff));
            }
        } else {
            legend = new StringBuffer(colorTable.size() * 10);
            legend.append("raster");
            /* Iterate through all the color values. */
            Enumeration crules = colorTable.getColorRules();
            while( crules.hasMoreElements() ) {
                ColorRule rule = (ColorRule) crules.nextElement();
                float low = rule.getLowCategoryValue();
                float range = rule.getCategoryRange();
                if (range == 0f) {
                    byte[] clr0 = rule.getColor(low);
                    legend.append("|").append(String.valueOf(low)).append("|#").append(
                            Format.sprintf("%02x", clr0[0] & 0xff)).append(
                            Format.sprintf("%02x", clr0[1] & 0xff)).append(
                            Format.sprintf("%02x", clr0[2] & 0xff));
                } else {
                    float inc = range / 6f;
                    String text = String.valueOf(low) + " to " + String.valueOf(low + range);
                    for( int i = 0; i < 7; i++ ) {
                        byte[] clr0 = rule.getColor(low);
                        legend.append("|").append((i == 3) ? text : "").append("|#").append(
                                Format.sprintf("%02x", clr0[0] & 0xff)).append(
                                Format.sprintf("%02x", clr0[1] & 0xff)).append(
                                Format.sprintf("%02x", clr0[2] & 0xff));
                        low += inc;
                    }
                }
            }
        }
        /* Can't afford to keep the table around in memory */
        legendAttribTable = null;
        System.out.println("LEGEND-->" + legend);
        return legend.toString();
    }

    /**
     * reset all the values if the map has been closed (else???)
     */
    public void hasChanged( boolean _haschanged ) {
        if (_haschanged) {
            novalue = new Double(Double.NaN);
            matrixType = 0;
            // theFilePath = null;
            filename = null;
            mapsetPath = null;
            rasterByteBuffer = null;
            moreData = false;
            // fileWindow = null;
            // dataWindow = null;
            rasterMapType = -9999;
            numberOfBytesPerValue = -9999;
        }
        hasChanged = _haschanged;
    }

    /**
     * @return
     */
    private Object getIntBuffer() {
        if (numberOfBytesPerValue == 8) {
            for( int i = 0; i < ((IntBuffer) dataObject).capacity(); i++ ) {
                ((IntBuffer) dataObject).put((int) rasterByteBuffer.getDouble());
            }
        } else if (numberOfBytesPerValue == 4 && rasterMapType < 0) {
            dataObject = rasterByteBuffer.asIntBuffer();
        } else if (rasterMapType > -1) {
            for( int i = 0; i < ((DoubleBuffer) dataObject).capacity(); i++ ) {
                ((IntBuffer) dataObject).put(rasterByteBuffer.getInt());
            }
        }
        ((IntBuffer) dataObject).rewind();
        return dataObject;
    }

    /**
     * @return
     */
    private Object getFloatBuffer() {
        if (numberOfBytesPerValue == 8) {
            for( int i = 0; i < ((FloatBuffer) dataObject).capacity(); i++ ) {
                ((FloatBuffer) dataObject).put((float) rasterByteBuffer.getDouble());
            }
        } else if (numberOfBytesPerValue == 4 && rasterMapType < 0) {
            dataObject = rasterByteBuffer.asFloatBuffer();
        } else if (rasterMapType > -1) {
            for( int i = 0; i < ((DoubleBuffer) dataObject).capacity(); i++ ) {
                ((FloatBuffer) dataObject).put(rasterByteBuffer.getInt());
            }
        }
        ((FloatBuffer) dataObject).rewind();
        return dataObject;
    }

    /**
     * @return
     */
    private Object getDoubleBuffer() {
        if (numberOfBytesPerValue == 8) {
            dataObject = rasterByteBuffer.asDoubleBuffer();
        } else if (numberOfBytesPerValue == 4 && rasterMapType < 0) {
            for( int i = 0; i < ((DoubleBuffer) dataObject).capacity(); i++ ) {
                ((DoubleBuffer) dataObject).put(rasterByteBuffer.getFloat());
            }
        } else if (rasterMapType > -1) {
            for( int i = 0; i < ((DoubleBuffer) dataObject).capacity(); i++ ) {
                ((DoubleBuffer) dataObject).put(rasterByteBuffer.getInt());
            }
        }
        ((DoubleBuffer) dataObject).rewind();
        return dataObject;
    }

    /**
     * @return
     */
    private Object getIntArray() {
        if (numberOfBytesPerValue == 8) {
            dataObject = intFromDoubleGridRead();
        } else if (numberOfBytesPerValue == 4 && rasterMapType < 0) {
            dataObject = intFromFloatGridRead();
        }
        // from integer map
        else if (rasterMapType > -1) {
            dataObject = intFromIntGridRead();
        } else {
            // throw some exception
        }
        return dataObject;
    }

    /**
     * @return
     */
    private Object getFloatArray() {
        if (numberOfBytesPerValue == 8) {
            dataObject = floatFromDoubleGridRead();
        } else if (numberOfBytesPerValue == 4 && rasterMapType < 0) {
            dataObject = floatFromFloatGridRead();
        }
        // from integer map
        else if (rasterMapType > -1) {
            dataObject = floatFromIntGridRead();
        } else {
            // throw some exception
        }
        return dataObject;
    }

    /**
     * @return
     */
    private Object getDoubleArray() {
        if (numberOfBytesPerValue == 8) {
            dataObject = doubleFromDoubleGridRead();
        } else if (numberOfBytesPerValue == 4 && rasterMapType < 0) {
            dataObject = doubleFromFloatGridRead();
        }
        // from integer map
        else if (rasterMapType > -1) {
            dataObject = doubleFromIntGridRead();
        } else {
            // throw some exception
        }
        return dataObject;
    }

    /**
     * utility to set particular parameters
     */
    public void setParameter( String key, Object obj ) {
        if (key.equals("novalue")) {
            novalue = obj;
        } else if (key.equals("matrixtype")) {
            Integer dmtype = (Integer) obj;
            matrixType = dmtype.intValue();
        }
    }

    public void setOutputDataObject( Object _dataObject ) {
        /* Call parent class to store data object */
        super.setOutputDataObject(_dataObject);
    }

    /**
     * Determines the map type given the ascii file. It reads the information from the header file
     */
    private boolean getRasterMapTypesAndData() {
        HashMap<String, String> fileMapHeader = new HashMap<String, String>();
        /* Read contents of 'cellhd/name' file from the current mapset */
        String line = null;
        try {
            //
            BufferedReader grassasciireader = new BufferedReader(new FileReader(theFilePath));
            // read the header
            for( int i = 0; i < 8; i++ ) {
                if ((line = grassasciireader.readLine()) != null) {
                    String lowerline = line.toLowerCase().trim();
                    StringTokenizer tok = new StringTokenizer(lowerline, ":");

                    if (tok.countTokens() == 2 && lowerline.indexOf(':') != -1) {
                        String key = tok.nextToken().trim();
                        String value = tok.nextToken().trim();
                        /*
                         * exceptions that sometimes occur can be added here below
                         */
                        if (key.startsWith("e-w res")) // could be "e-w resol"
                        {
                            key = "e-w res";
                        }
                        if (key.startsWith("n-s res")) {
                            key = "n-s res";
                        }
                        fileMapHeader.put(key, value);
                    } else {
                        break;
                    }
                }
            }

            double north = 0.0;
            double south = 0.0;
            double east = 0.0;
            double west = 0.0;
            double xres = 0.0;
            double yres = 0.0;
            int thecols = 0;
            int therows = 0;

            north = Double.parseDouble(fileMapHeader.get("north"));
            south = Double.parseDouble(fileMapHeader.get("south"));
            east = Double.parseDouble(fileMapHeader.get("east"));
            west = Double.parseDouble(fileMapHeader.get("west"));
            if (!fileMapHeader.containsKey("e-w res") && !fileMapHeader.containsKey("n-s res")) {
                thecols = Integer.parseInt(fileMapHeader.get("cols"));
                therows = Integer.parseInt(fileMapHeader.get("rows"));

                xres = (east - west) / thecols;
                yres = (north - south) / therows;
            } else {
                xres = Double.parseDouble(fileMapHeader.get("e-w res"));
                yres = Double.parseDouble(fileMapHeader.get("n-s res"));

                therows = (int) ((north - south) / yres);
                thecols = (int) ((east - west) / xres);
            }

            /*
             * Setup file window object that holds the geographic limits of the file data.
             */
            fileWindow = null;
            if (fileMapHeader.containsKey("n-s res")) {
                fileWindow = new JGrassRegion(Double.parseDouble(fileMapHeader.get("west")), Double
                        .parseDouble(fileMapHeader.get("east")), Double.parseDouble(fileMapHeader
                        .get("south")), Double.parseDouble(fileMapHeader.get("north")), Double
                        .parseDouble(fileMapHeader.get("e-w res")), Double
                        .parseDouble(fileMapHeader.get("n-s res")));
            } else if (fileMapHeader.containsKey("cols")) {
                fileWindow = new JGrassRegion(Double.parseDouble(fileMapHeader.get("west")), Double
                        .parseDouble(fileMapHeader.get("east")), Double.parseDouble(fileMapHeader
                        .get("south")), Double.parseDouble(fileMapHeader.get("north")), Integer
                        .parseInt(fileMapHeader.get("rows")), Integer.parseInt(fileMapHeader
                        .get("cols")));
            } else {

            }
            dataWindow = fileWindow;

            /*
             * grass ascii files are parsed always as doubles
             */
            rasterMapType = -2;
            numberOfBytesPerValue = 8;

            /*
             * read the file and keep it in memory for further resolution issues
             */
            datamatrix = new double[fileWindow.getRows()][fileWindow.getCols()];
            // put all the values into a matrix
            String value = null;
            for( int i = 0; i < fileWindow.getRows(); i++ ) {
                StringTokenizer tok = new StringTokenizer(line);
                for( int j = 0; j < fileWindow.getCols(); j++ ) {
                    value = tok.nextToken().trim();
                    if (value.equals("*") || value.equals(String.valueOf(novalue))) {
                        datamatrix[i][j] = Double.NaN;
                    } else {
                        datamatrix[i][j] = Double.parseDouble(value);
                    }
                }
                if ((line = grassasciireader.readLine()) != null) {
                    continue;
                } else {
                    break;
                }

            }

            // close the handler to the file
            grassasciireader.close();

        } catch (FileNotFoundException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    /**
     * reads a row of data from the file into a byte array.
     * 
     * @param currentfilerow the current row to be extracted from the file
     * @param rowDataCache the byte array to store the unpacked row data
     * @return boolean TRUE for success, FALSE for failure.
     * @throws IOException
     * @throws DataFormatException
     */
    private boolean readRasterRow( int currentfilerow, byte[] rowDataCache, byte[] rowColorDataCache )
            throws IOException, DataFormatException {
        ByteBuffer rowBuffer = ByteBuffer.wrap(rowDataCache);
        ByteBuffer rowColorBuffer = ByteBuffer.wrap(rowColorDataCache);
        /*
         * Read the correct approximated row from the file. The row contents as saved in a cache for
         * along with the row number. If the row requested is the row in the cache then we do not
         * ned to read from the file.
         */
        int currentfilecol;

        /* Data window geographic boundaries */
        double activeewres = dataWindow.getWEResolution();
        double activewest = dataWindow.getWest();

        /* Map file geographic limits */
        double filewest = fileWindow.getWest();
        double fileewres = fileWindow.getWEResolution();

        /* Reset row cache and read new row data */
        double[] rowCache = new double[datamatrix[currentfilerow].length];
        System.arraycopy(datamatrix[currentfilerow], 0, rowCache, 0, datamatrix[0].length);

        for( int col = 0; col < dataWindow.getCols(); col++ ) {
            /*
             * Calculate the column value of the data to be extracted from the row
             */
            currentfilecol = (int) (((activewest + (col * activeewres)) - filewest) / fileewres);
            /*
             * If file column value is outside the boundaries of the file or a null is returned from
             * the 'null' file in cell_misc directory then the value stored is NOVALUE.
             */
            if ((currentfilecol < 0 || currentfilecol >= fileWindow.getCols())) {
                /* For double values we use the NAN value. */
                rowBuffer.putDouble(Double.NaN);
                if (colorTable != null)
                    colorTable.interpolateColorValue(rowColorBuffer, Double.NaN);
            } else {
                rowBuffer.putDouble(rowCache[currentfilecol]);
                if (colorTable != null)
                    colorTable.interpolateColorValue(rowColorBuffer, rowCache[currentfilecol]);
                /* Update data range value */
                if (rowCache[currentfilecol] < dataRange[0])
                    dataRange[0] = rowCache[currentfilecol];
                else if (rowCache[currentfilecol] > dataRange[1])
                    dataRange[1] = rowCache[currentfilecol];
            }
        }

        return true;
    }

    /**
     * Utility method to read a double matrix of data from a double raster map
     * 
     * @throws IOException
     * @throws DataFormatException
     */
    private Object doubleFromDoubleGridRead() {

        int nx = dataWindow.getRows();
        int ny = dataWindow.getCols();

        /*
         * this defines the tipe of matrix to return: 0 = normal type[][] matrix
         */
        if (matrixType == 0) {
            double tmp;
            for( int i = 0; i < nx; i++ ) {
                for( int k = 0; k < ny; k++ ) {
                    tmp = rasterByteBuffer.getDouble();
                    // set the range
                    setRange(tmp, i, k);
                    // the value has to be different from NaN
                    if (tmp == tmp) {
                        ((double[][]) dataObject)[i][k] = tmp;
                    } else {
                        ((double[][]) dataObject)[i][k] = ((Double) novalue).doubleValue();
                    }
                }
            }
        }
        /*
         * 1 = type[][] matrix indexed from [1][1]
         */
        else if (matrixType == 1) {
            dataObject = new double[nx + 1][ny + 1];
            double tmp;
            for( int i = 0; i < nx; i++ ) {
                for( int k = 0; k < ny; k++ ) {
                    tmp = rasterByteBuffer.getDouble();
                    // set the range
                    setRange(tmp, i, k);
                    // the value has to be different from NaN
                    if (tmp == tmp) {
                        ((double[][]) dataObject)[i + 1][k + 1] = tmp;
                    } else {
                        ((double[][]) dataObject)[i + 1][k + 1] = ((Double) novalue).doubleValue();
                    }
                }
            }
        }
        /*
         * 2 = transposed of the original type[][] matrix
         */
        else if (matrixType == 2) {
            dataObject = new double[ny][nx];

            double tmp;
            for( int i = 0; i < nx; i++ ) {
                for( int k = 0; k < ny; k++ ) {
                    tmp = rasterByteBuffer.getDouble();
                    // set the range
                    setRange(tmp, i, k);
                    // the value has to be different from NaN
                    if (tmp == tmp) {
                        ((double[][]) dataObject)[k][i] = tmp;
                        // System.out.println("TMP = " + tmp + " and the used
                        // novalue is = " + ((Double) novalue).doubleValue() + "
                        // TMP CHOSEN");
                    } else {
                        ((double[][]) dataObject)[k][i] = ((Double) novalue).doubleValue();
                        // System.out.println("TMP = " + tmp + " and the used
                        // novalue is = " + ((Double) novalue).doubleValue() + "
                        // NOVALUE CHOSEN");
                    }
                }
            }
        }
        /*
         * 3 = transposed of the original type[][] matrix and indexed by [1][1]
         */
        else if (matrixType == 3) {
            double tmp;
            dataObject = new double[ny + 1][nx + 1];
            for( int i = 0; i < nx; i++ ) {
                for( int k = 0; k < ny; k++ ) {
                    tmp = rasterByteBuffer.getDouble();
                    // set the range
                    setRange(tmp, i, k);
                    // the value has to be different from NaN
                    if (tmp == tmp) {
                        ((double[][]) dataObject)[k + 1][i + 1] = tmp;
                    } else {
                        ((double[][]) dataObject)[k + 1][i + 1] = ((Double) novalue).doubleValue();
                    }
                }
            }
        } else {
            // throw something
        }
        return dataObject;
    }

    /**
     * Utility method to read a double matrix of data from a float raster map
     * 
     * @throws IOException
     * @throws DataFormatException
     */
    private Object doubleFromFloatGridRead() {
        int nx = dataWindow.getRows();
        int ny = dataWindow.getCols();

        /*
         * this defines the tipe of matrix to return: 0 = normal type[][] matrix
         */
        if (matrixType == 0) {
            double tmp;
            for( int i = 0; i < nx; i++ ) {
                for( int k = 0; k < ny; k++ ) {
                    tmp = rasterByteBuffer.getFloat();
                    // set the range
                    setRange(tmp, i, k);
                    // the value has to be different from NaN
                    if (tmp == tmp) {
                        ((double[][]) dataObject)[i][k] = tmp;
                    } else {
                        ((double[][]) dataObject)[i][k] = ((Double) novalue).doubleValue();
                    }
                }
            }
        }
        /*
         * 1 = type[][] matrix indexed from [1][1]
         */
        else if (matrixType == 1) {
            dataObject = new double[nx + 1][ny + 1];
            double tmp;
            for( int i = 0; i < nx; i++ ) {
                for( int k = 0; k < ny; k++ ) {
                    tmp = rasterByteBuffer.getFloat();
                    // set the range
                    setRange(tmp, i, k);
                    // the value has to be different from NaN
                    if (tmp == tmp) {
                        ((double[][]) dataObject)[i + 1][k + 1] = tmp;
                    } else {
                        ((double[][]) dataObject)[i + 1][k + 1] = ((Double) novalue).doubleValue();
                    }
                }
            }
        }
        /*
         * 2 = transposed of the original type[][] matrix
         */
        else if (matrixType == 2) {
            double tmp;
            dataObject = new double[ny][nx];
            for( int i = 0; i < nx; i++ ) {
                for( int k = 0; k < ny; k++ ) {
                    tmp = rasterByteBuffer.getFloat();
                    // set the range
                    setRange(tmp, i, k);
                    // the value has to be different from NaN
                    if (tmp == tmp) {
                        ((double[][]) dataObject)[k][i] = tmp;
                    } else {
                        ((double[][]) dataObject)[k][i] = ((Double) novalue).doubleValue();
                    }
                }
            }
        }
        /*
         * 3 = transposed of the original type[][] matrix and indexed by [1][1]
         */
        else if (matrixType == 3) {
            double tmp;
            dataObject = new double[ny + 1][nx + 1];
            for( int i = 0; i < nx; i++ ) {
                for( int k = 0; k < ny; k++ ) {
                    tmp = rasterByteBuffer.getFloat();
                    // set the range
                    setRange(tmp, i, k);
                    // the value has to be different from NaN
                    if (tmp == tmp) {
                        ((double[][]) dataObject)[k + 1][i + 1] = tmp;
                    } else {
                        ((double[][]) dataObject)[k + 1][i + 1] = ((Double) novalue).doubleValue();
                    }
                }
            }
        } else {
            // throw something
        }
        return dataObject;
    }

    /**
     * Utility method to read a double matrix of data from a Integer raster map
     * 
     * @throws IOException
     * @throws DataFormatException
     */
    private Object doubleFromIntGridRead() {
        int nx = dataWindow.getRows();
        int ny = dataWindow.getCols();

        /*
         * this defines the tipe of matrix to return: 0 = normal type[][] matrix
         */
        if (matrixType == 0) {
            double tmp;
            for( int i = 0; i < nx; i++ ) {
                for( int k = 0; k < ny; k++ ) {
                    tmp = rasterByteBuffer.getInt();
                    // set the range
                    setRange(tmp, i, k);
                    // the value has to be different from NaN
                    if (tmp == tmp && tmp != Integer.MAX_VALUE) {
                        ((double[][]) dataObject)[i][k] = tmp;
                    } else {
                        ((double[][]) dataObject)[i][k] = ((Double) novalue).doubleValue();
                    }
                }
            }
        }
        /*
         * 1 = type[][] matrix indexed from [1][1]
         */
        else if (matrixType == 1) {
            dataObject = new double[nx + 1][ny + 1];
            double tmp;
            for( int i = 0; i < nx; i++ ) {
                for( int k = 0; k < ny; k++ ) {
                    /*
                     * integers can be of 1, 2, 3 or 4 bytes. Need to 0-pad them to get the value as
                     * a java integer
                     */
                    ByteBuffer tmpbuf = ByteBuffer.allocate(4);
                    // padding
                    for( int j = 0; j < (4 - rasterMapType); j++ ) {
                        tmpbuf.put((byte) 0);
                    }
                    for( int j = 0; j < rasterMapType; j++ ) {
                        tmpbuf.put(rasterByteBuffer.get());
                    }
                    tmpbuf.rewind();
                    tmp = tmpbuf.getInt();
                    // set the range
                    setRange(tmp, i, k);
                    // the value has to be different from NaN
                    if (tmp == tmp && tmp != Integer.MAX_VALUE) {
                        ((double[][]) dataObject)[i + 1][k + 1] = tmp;
                    } else {
                        ((double[][]) dataObject)[i + 1][k + 1] = ((Double) novalue).doubleValue();
                    }
                }
            }
        }
        /*
         * 2 = transposed of the original type[][] matrix
         */
        else if (matrixType == 2) {
            dataObject = new double[ny][nx];
            double tmp;
            for( int i = 0; i < nx; i++ ) {
                for( int k = 0; k < ny; k++ ) {
                    /*
                     * integers can be of 1, 2, 3 or 4 bytes. Need to 0-pad them to get the value as
                     * a java integer
                     */
                    ByteBuffer tmpbuf = ByteBuffer.allocate(4);
                    // padding
                    for( int j = 0; j < (4 - rasterMapType); j++ ) {
                        tmpbuf.put((byte) 0);
                    }
                    for( int j = 0; j < rasterMapType; j++ ) {
                        tmpbuf.put(rasterByteBuffer.get());
                    }
                    tmpbuf.rewind();
                    tmp = tmpbuf.getInt();
                    // set the range
                    setRange(tmp, i, k);
                    // the value has to be different from NaN
                    if (tmp == tmp && tmp != Integer.MAX_VALUE) {
                        ((double[][]) dataObject)[k][i] = tmp;
                    } else {
                        ((double[][]) dataObject)[k][i] = ((Double) novalue).doubleValue();
                    }
                }
            }
        }
        /*
         * 3 = transposed of the original type[][] matrix and indexed by [1][1]
         */
        else if (matrixType == 3) {
            double tmp;
            dataObject = new double[ny + 1][nx + 1];
            for( int i = 0; i < nx; i++ ) {
                for( int k = 0; k < ny; k++ ) {
                    /*
                     * integers can be of 1, 2, 3 or 4 bytes. Need to 0-pad them to get the value as
                     * a java integer
                     */
                    ByteBuffer tmpbuf = ByteBuffer.allocate(4);
                    // padding
                    for( int j = 0; j < (4 - rasterMapType); j++ ) {
                        tmpbuf.put((byte) 0);
                    }
                    for( int j = 0; j < rasterMapType; j++ ) {
                        tmpbuf.put(rasterByteBuffer.get());
                    }
                    tmpbuf.rewind();
                    tmp = tmpbuf.getInt();
                    // set the range
                    setRange(tmp, i, k);
                    // the value has to be different from NaN
                    if (tmp == tmp && tmp != Integer.MAX_VALUE) {
                        ((double[][]) dataObject)[k + 1][i + 1] = tmp;
                    } else {
                        ((double[][]) dataObject)[k + 1][i + 1] = ((Double) novalue).doubleValue();
                    }
                }
            }
        } else {
            // throw something
        }
        return dataObject;
    }

    /**
     * Utility method to read a float matrix of data from a double raster map
     * 
     * @throws IOException
     * @throws DataFormatException
     */
    private Object floatFromDoubleGridRead() {
        int nx = dataWindow.getRows();
        int ny = dataWindow.getCols();

        /*
         * this defines the tipe of matrix to return: 0 = normal type[][] matrix
         */
        if (matrixType == 0) {
            float tmp;
            for( int i = 0; i < nx; i++ ) {
                for( int k = 0; k < ny; k++ ) {
                    tmp = (float) rasterByteBuffer.getDouble();
                    // set the range
                    setRange(tmp, i, k);
                    // the value has to be different from NaN
                    if (tmp == tmp) {
                        ((float[][]) dataObject)[i][k] = tmp;
                    } else {
                        ((float[][]) dataObject)[i][k] = ((Float) novalue).floatValue();
                    }
                }
            }
        }
        /*
         * 1 = type[][] matrix indexed from [1][1]
         */
        else if (matrixType == 1) {
            dataObject = new float[nx + 1][ny + 1];
            float tmp;
            for( int i = 0; i < nx; i++ ) {
                for( int k = 0; k < ny; k++ ) {
                    tmp = (float) rasterByteBuffer.getDouble();
                    // set the range
                    setRange(tmp, i, k);
                    // the value has to be different from NaN
                    if (tmp == tmp) {
                        ((float[][]) dataObject)[i + 1][k + 1] = tmp;
                    } else {
                        ((float[][]) dataObject)[i + 1][k + 1] = ((Float) novalue).floatValue();
                    }
                }
            }
        }
        /*
         * 2 = transposed of the original type[][] matrix
         */
        else if (matrixType == 2) {
            dataObject = new float[ny][nx];
            float tmp;
            for( int i = 0; i < nx; i++ ) {
                for( int k = 0; k < ny; k++ ) {
                    tmp = (float) rasterByteBuffer.getDouble();
                    // set the range
                    setRange(tmp, i, k);
                    // the value has to be different from NaN
                    if (tmp == tmp) {
                        ((float[][]) dataObject)[k][i] = tmp;
                    } else {
                        ((float[][]) dataObject)[k][i] = ((Float) novalue).floatValue();
                    }
                }
            }
        }
        /*
         * 3 = transposed of the original type[][] matrix and indexed by [1][1]
         */
        else if (matrixType == 3) {
            double tmp;
            dataObject = new float[ny + 1][nx + 1];
            for( int i = 0; i < nx; i++ ) {
                for( int k = 0; k < ny; k++ ) {
                    tmp = (float) rasterByteBuffer.getDouble();
                    // set the range
                    setRange(tmp, i, k);
                    // the value has to be different from NaN
                    if (tmp == tmp) {
                        ((double[][]) dataObject)[k + 1][i + 1] = tmp;
                    } else {
                        ((double[][]) dataObject)[k + 1][i + 1] = ((Float) novalue).floatValue();
                    }
                }
            }
        } else {
            // throw something
        }
        return dataObject;
    }

    /**
     * Utility method to read a float matrix of data from a float raster map
     * 
     * @throws IOException
     * @throws DataFormatException
     */
    private Object floatFromFloatGridRead() {
        int nx = dataWindow.getRows();
        int ny = dataWindow.getCols();

        /*
         * this defines the tipe of matrix to return: 0 = normal type[][] matrix
         */
        if (matrixType == 0) {
            float tmp;
            for( int i = 0; i < nx; i++ ) {
                for( int k = 0; k < ny; k++ ) {
                    tmp = rasterByteBuffer.getFloat();
                    // set the range
                    setRange(tmp, i, k);
                    // the value has to be different from NaN
                    if (tmp == tmp) {
                        ((float[][]) dataObject)[i][k] = tmp;
                    } else {
                        ((float[][]) dataObject)[i][k] = ((Float) novalue).floatValue();
                    }
                }
            }
        }
        /*
         * 1 = type[][] matrix indexed from [1][1]
         */
        else if (matrixType == 1) {
            dataObject = new float[nx + 1][ny + 1];
            float tmp;
            for( int i = 0; i < nx; i++ ) {
                for( int k = 0; k < ny; k++ ) {
                    tmp = rasterByteBuffer.getFloat();
                    // set the range
                    setRange(tmp, i, k);
                    // the value has to be different from NaN
                    if (tmp == tmp) {
                        ((float[][]) dataObject)[i + 1][k + 1] = tmp;
                    } else {
                        ((float[][]) dataObject)[i + 1][k + 1] = ((Float) novalue).floatValue();
                    }
                }
            }
        }
        /*
         * 2 = transposed of the original type[][] matrix
         */
        else if (matrixType == 2) {
            dataObject = new float[ny][nx];
            float tmp;
            for( int i = 0; i < nx; i++ ) {
                for( int k = 0; k < ny; k++ ) {
                    tmp = rasterByteBuffer.getFloat();
                    // set the range
                    setRange(tmp, i, k);
                    // the value has to be different from NaN
                    if (tmp == tmp) {
                        ((float[][]) dataObject)[k][i] = tmp;
                    } else {
                        ((float[][]) dataObject)[k][i] = ((Float) novalue).floatValue();
                    }
                }
            }
        }
        /*
         * 3 = transposed of the original type[][] matrix and indexed by [1][1]
         */
        else if (matrixType == 3) {
            float tmp;
            dataObject = new float[ny + 1][nx + 1];
            for( int i = 0; i < nx; i++ ) {
                for( int k = 0; k < ny; k++ ) {
                    tmp = rasterByteBuffer.getFloat();
                    // set the range
                    setRange(tmp, i, k);
                    // the value has to be different from NaN
                    if (tmp == tmp) {
                        ((float[][]) dataObject)[k + 1][i + 1] = tmp;
                    } else {
                        ((float[][]) dataObject)[k + 1][i + 1] = ((Float) novalue).floatValue();
                    }
                }
            }
        } else {
            // throw something
        }
        return dataObject;
    }

    /**
     * Utility method to read a float matrix of data from a Integer raster map
     * 
     * @throws IOException
     * @throws DataFormatException
     */
    private Object floatFromIntGridRead() {
        int nx = dataWindow.getRows();
        int ny = dataWindow.getCols();

        /*
         * this defines the tipe of matrix to return: 0 = normal type[][] matrix
         */
        if (matrixType == 0) {
            float tmp;
            for( int i = 0; i < nx; i++ ) {
                for( int k = 0; k < ny; k++ ) {
                    tmp = rasterByteBuffer.getInt();
                    // set the range
                    setRange(tmp, i, k);
                    // the value has to be different from NaN
                    if (tmp == tmp && tmp != Integer.MAX_VALUE) {
                        ((float[][]) dataObject)[i][k] = tmp;
                    } else {
                        ((float[][]) dataObject)[i][k] = ((Float) novalue).floatValue();
                    }
                }
            }
        }
        /*
         * 1 = type[][] matrix indexed from [1][1]
         */
        else if (matrixType == 1) {
            dataObject = new float[nx + 1][ny + 1];
            float tmp;
            for( int i = 0; i < nx; i++ ) {
                for( int k = 0; k < ny; k++ ) {
                    /*
                     * integers can be of 1, 2, 3 or 4 bytes. Need to 0-pad them to get the value as
                     * a java integer
                     */
                    ByteBuffer tmpbuf = ByteBuffer.allocate(4);
                    // padding
                    for( int j = 0; j < (4 - rasterMapType); j++ ) {
                        tmpbuf.put((byte) 0);
                    }
                    for( int j = 0; j < rasterMapType; j++ ) {
                        tmpbuf.put(rasterByteBuffer.get());
                    }
                    tmpbuf.rewind();
                    tmp = tmpbuf.getInt();
                    // set the range
                    setRange(tmp, i, k);
                    // the value has to be different from NaN
                    if (tmp == tmp && tmp != Integer.MAX_VALUE) {
                        ((float[][]) dataObject)[i + 1][k + 1] = tmp;
                    } else {
                        ((float[][]) dataObject)[i + 1][k + 1] = ((Float) novalue).floatValue();
                    }
                }
            }
        }
        /*
         * 2 = transposed of the original type[][] matrix
         */
        else if (matrixType == 2) {
            dataObject = new float[ny][nx];
            float tmp;
            for( int i = 0; i < nx; i++ ) {
                for( int k = 0; k < ny; k++ ) {
                    /*
                     * integers can be of 1, 2, 3 or 4 bytes. Need to 0-pad them to get the value as
                     * a java integer
                     */
                    ByteBuffer tmpbuf = ByteBuffer.allocate(4);
                    // padding
                    for( int j = 0; j < (4 - rasterMapType); j++ ) {
                        tmpbuf.put((byte) 0);
                    }
                    for( int j = 0; j < rasterMapType; j++ ) {
                        tmpbuf.put(rasterByteBuffer.get());
                    }
                    tmpbuf.rewind();
                    tmp = tmpbuf.getInt();
                    // set the range
                    setRange(tmp, i, k);
                    // the value has to be different from NaN
                    if (tmp == tmp && tmp != Integer.MAX_VALUE) {
                        ((float[][]) dataObject)[k][i] = tmp;
                    } else {
                        ((float[][]) dataObject)[k][i] = ((Float) novalue).floatValue();
                    }
                }
            }
        }
        /*
         * 3 = transposed of the original type[][] matrix and indexed by [1][1]
         */
        else if (matrixType == 3) {
            float tmp;
            dataObject = new float[ny + 1][nx + 1];
            for( int i = 0; i < nx; i++ ) {
                for( int k = 0; k < ny; k++ ) {
                    /*
                     * integers can be of 1, 2, 3 or 4 bytes. Need to 0-pad them to get the value as
                     * a java integer
                     */
                    ByteBuffer tmpbuf = ByteBuffer.allocate(4);
                    // padding
                    for( int j = 0; j < (4 - rasterMapType); j++ ) {
                        tmpbuf.put((byte) 0);
                    }
                    for( int j = 0; j < rasterMapType; j++ ) {
                        tmpbuf.put(rasterByteBuffer.get());
                    }
                    tmpbuf.rewind();
                    tmp = tmpbuf.getInt();
                    // set the range
                    setRange(tmp, i, k);
                    // the value has to be different from NaN
                    if (tmp == tmp && tmp != Integer.MAX_VALUE) {
                        ((float[][]) dataObject)[k + 1][i + 1] = tmp;
                    } else {
                        ((float[][]) dataObject)[k + 1][i + 1] = ((Float) novalue).floatValue();
                    }
                }
            }
        } else {
            // throw something
        }
        return dataObject;
    }

    /**
     * Utility method to read a int matrix of data from a double raster map
     * 
     * @throws IOException
     * @throws DataFormatException
     */
    private Object intFromDoubleGridRead() {
        int nx = dataWindow.getRows();
        int ny = dataWindow.getCols();

        /*
         * this defines the tipe of matrix to return: 0 = normal type[][] matrix
         */
        if (matrixType == 0) {
            int tmp;
            for( int i = 0; i < nx; i++ ) {
                for( int k = 0; k < ny; k++ ) {
                    tmp = (int) rasterByteBuffer.getDouble();
                    // set the range
                    setRange(tmp, i, k);
                    // the value has to be different from NaN
                    if (tmp == tmp) {
                        ((int[][]) dataObject)[i][k] = tmp;
                    } else {
                        ((int[][]) dataObject)[i][k] = ((Integer) novalue).intValue();
                    }
                }
            }
        }
        /*
         * 1 = type[][] matrix indexed from [1][1]
         */
        else if (matrixType == 1) {
            dataObject = new int[nx + 1][ny + 1];
            int tmp;
            for( int i = 0; i < nx; i++ ) {
                for( int k = 0; k < ny; k++ ) {
                    tmp = (int) rasterByteBuffer.getDouble();
                    // set the range
                    setRange(tmp, i, k);
                    // the value has to be different from NaN
                    if (tmp == tmp) {
                        ((int[][]) dataObject)[i + 1][k + 1] = tmp;
                    } else {
                        ((int[][]) dataObject)[i + 1][k + 1] = ((Integer) novalue).intValue();
                    }
                }
            }
        }
        /*
         * 2 = transposed of the original type[][] matrix
         */
        else if (matrixType == 2) {
            dataObject = new int[ny][nx];
            int tmp;
            for( int i = 0; i < nx; i++ ) {
                for( int k = 0; k < ny; k++ ) {
                    tmp = (int) rasterByteBuffer.getDouble();
                    // set the range
                    setRange(tmp, i, k);
                    // the value has to be different from NaN
                    if (tmp == tmp) {
                        ((int[][]) dataObject)[k][i] = tmp;
                    } else {
                        ((int[][]) dataObject)[k][i] = ((Integer) novalue).intValue();
                    }
                }
            }
        }
        /*
         * 3 = transposed of the original type[][] matrix and indexed by [1][1]
         */
        else if (matrixType == 3) {
            int tmp;
            dataObject = new int[ny + 1][nx + 1];
            for( int i = 0; i < nx; i++ ) {
                for( int k = 0; k < ny; k++ ) {
                    tmp = (int) rasterByteBuffer.getDouble();
                    // set the range
                    setRange(tmp, i, k);
                    // the value has to be different from NaN
                    if (tmp == tmp) {
                        ((int[][]) dataObject)[k + 1][i + 1] = tmp;
                    } else {
                        ((int[][]) dataObject)[k + 1][i + 1] = ((Integer) novalue).intValue();
                    }
                }
            }
        } else {
            // throw something
        }
        return dataObject;
    }

    /**
     * Utility method to read a int matrix of data from a float raster map
     * 
     * @throws IOException
     * @throws DataFormatException
     */
    private Object intFromFloatGridRead() {
        int nx = dataWindow.getRows();
        int ny = dataWindow.getCols();

        /*
         * this defines the tipe of matrix to return: 0 = normal type[][] matrix
         */
        if (matrixType == 0) {
            int tmp;
            for( int i = 0; i < nx; i++ ) {
                for( int k = 0; k < ny; k++ ) {
                    tmp = (int) rasterByteBuffer.getFloat();
                    // set the range
                    setRange(tmp, i, k);
                    // the value has to be different from NaN
                    if (tmp == tmp) {
                        ((int[][]) dataObject)[i][k] = tmp;
                    } else {
                        ((int[][]) dataObject)[i][k] = ((Integer) novalue).intValue();
                    }
                }
            }
        }
        /*
         * 1 = type[][] matrix indexed from [1][1]
         */
        else if (matrixType == 1) {
            dataObject = new int[nx + 1][ny + 1];
            int tmp;
            for( int i = 0; i < nx; i++ ) {
                for( int k = 0; k < ny; k++ ) {
                    tmp = (int) rasterByteBuffer.getFloat();
                    // set the range
                    setRange(tmp, i, k);
                    // the value has to be different from NaN
                    if (tmp == tmp) {
                        ((int[][]) dataObject)[i + 1][k + 1] = tmp;
                    } else {
                        ((int[][]) dataObject)[i + 1][k + 1] = ((Integer) novalue).intValue();
                    }
                }
            }
        }
        /*
         * 2 = transposed of the original type[][] matrix
         */
        else if (matrixType == 2) {
            dataObject = new int[ny][nx];
            int tmp;
            for( int i = 0; i < nx; i++ ) {
                for( int k = 0; k < ny; k++ ) {
                    tmp = (int) rasterByteBuffer.getFloat();
                    // set the range
                    setRange(tmp, i, k);
                    // the value has to be different from NaN
                    if (tmp == tmp) {
                        ((int[][]) dataObject)[k][i] = tmp;
                    } else {
                        ((int[][]) dataObject)[k][i] = ((Integer) novalue).intValue();
                    }
                }
            }
        }
        /*
         * 3 = transposed of the original type[][] matrix and indexed by [1][1]
         */
        else if (matrixType == 3) {
            int tmp;
            dataObject = new int[ny + 1][nx + 1];
            for( int i = 0; i < nx; i++ ) {
                for( int k = 0; k < ny; k++ ) {
                    tmp = (int) rasterByteBuffer.getFloat();
                    // set the range
                    setRange(tmp, i, k);
                    // the value has to be different from NaN
                    if (tmp == tmp) {
                        ((int[][]) dataObject)[k + 1][i + 1] = tmp;
                    } else {
                        ((int[][]) dataObject)[k + 1][i + 1] = ((Integer) novalue).intValue();
                    }
                }
            }
        } else {
            // throw something
        }
        return dataObject;
    }

    /**
     * Utility method to read a integer matrix of data from a Integer raster map
     * 
     * @throws IOException
     * @throws DataFormatException
     */
    private Object intFromIntGridRead() {
        int nx = dataWindow.getRows();
        int ny = dataWindow.getCols();

        /*
         * this defines the tipe of matrix to return: 0 = normal type[][] matrix
         */
        if (matrixType == 0) {
            int tmp;
            for( int i = 0; i < nx; i++ ) {
                for( int k = 0; k < ny; k++ ) {
                    tmp = rasterByteBuffer.getInt();
                    // set the range
                    setRange(tmp, i, k);
                    // the value has to be different from NaN
                    if (tmp == tmp && tmp != Integer.MAX_VALUE) {
                        ((int[][]) dataObject)[i][k] = tmp;
                    } else {
                        ((int[][]) dataObject)[i][k] = ((Integer) novalue).intValue();
                    }
                }
            }
        }
        /*
         * 1 = type[][] matrix indexed from [1][1]
         */
        else if (matrixType == 1) {
            dataObject = new int[nx + 1][ny + 1];
            int tmp;
            for( int i = 0; i < nx; i++ ) {
                for( int k = 0; k < ny; k++ ) {
                    /*
                     * integers can be of 1, 2, 3 or 4 bytes. Need to 0-pad them to get the value as
                     * a java integer
                     */
                    ByteBuffer tmpbuf = ByteBuffer.allocate(4);
                    // padding
                    for( int j = 0; j < (4 - rasterMapType); j++ ) {
                        tmpbuf.put((byte) 0);
                    }
                    for( int j = 0; j < rasterMapType; j++ ) {
                        tmpbuf.put(rasterByteBuffer.get());
                    }
                    tmpbuf.rewind();
                    tmp = tmpbuf.getInt();
                    // set the range
                    setRange(tmp, i, k);
                    // the value has to be different from NaN
                    if (tmp == tmp && tmp != Integer.MAX_VALUE) {
                        ((int[][]) dataObject)[i + 1][k + 1] = tmp;
                    } else {
                        ((int[][]) dataObject)[i + 1][k + 1] = ((Integer) novalue).intValue();
                    }
                }
            }
        }
        /*
         * 2 = transposed of the original type[][] matrix
         */
        else if (matrixType == 2) {
            dataObject = new int[ny][nx];
            int tmp;
            for( int i = 0; i < nx; i++ ) {
                for( int k = 0; k < ny; k++ ) {
                    /*
                     * integers can be of 1, 2, 3 or 4 bytes. Need to 0-pad them to get the value as
                     * a java integer
                     */
                    ByteBuffer tmpbuf = ByteBuffer.allocate(4);
                    // padding
                    for( int j = 0; j < (4 - rasterMapType); j++ ) {
                        tmpbuf.put((byte) 0);
                    }
                    for( int j = 0; j < rasterMapType; j++ ) {
                        tmpbuf.put(rasterByteBuffer.get());
                    }
                    tmpbuf.rewind();
                    tmp = tmpbuf.getInt();
                    // set the range
                    setRange(tmp, i, k);
                    // the value has to be different from NaN
                    if (tmp == tmp && tmp != Integer.MAX_VALUE) {
                        ((int[][]) dataObject)[k][i] = tmp;
                    } else {
                        ((int[][]) dataObject)[k][i] = ((Integer) novalue).intValue();
                    }
                }
            }
        }
        /*
         * 3 = transposed of the original type[][] matrix and indexed by [1][1]
         */
        else if (matrixType == 3) {
            int tmp;
            dataObject = new int[ny + 1][nx + 1];
            for( int i = 0; i < nx; i++ ) {
                for( int k = 0; k < ny; k++ ) {
                    /*
                     * integers can be of 1, 2, 3 or 4 bytes. Need to 0-pad them to get the value as
                     * a java integer
                     */
                    ByteBuffer tmpbuf = ByteBuffer.allocate(4);
                    // padding
                    for( int j = 0; j < (4 - rasterMapType); j++ ) {
                        tmpbuf.put((byte) 0);
                    }
                    for( int j = 0; j < rasterMapType; j++ ) {
                        tmpbuf.put(rasterByteBuffer.get());
                    }
                    tmpbuf.rewind();
                    tmp = tmpbuf.getInt();
                    // set the range
                    setRange(tmp, i, k);
                    // the value has to be different from NaN
                    if (tmp == tmp && tmp != Integer.MAX_VALUE) {
                        ((int[][]) dataObject)[k + 1][i + 1] = tmp;
                    } else {
                        ((int[][]) dataObject)[k + 1][i + 1] = ((Integer) novalue).intValue();
                    }
                }
            }
        } else {
            // throw something
        }
        return dataObject;
    }

    /**
     * @param tmp
     */
    private void setRange( double tmp, int i, int k ) {
        // set the range
        // if (logger.isDebugEnabled()) logger.debug("RANGEVALUE = " + tmp);
        if (i == 0 && k == 0 && tmp == tmp) {
            range[0] = tmp;
            range[1] = tmp;
        } else {
            if (tmp < range[0] && tmp == tmp)
                range[0] = tmp;
            if (tmp > range[1] && tmp == tmp)
                range[1] = tmp;
        }

    }

    /**
     * retrieve the range values of the map
     */
    public double[] getRange() {
        return range;
    }

}
