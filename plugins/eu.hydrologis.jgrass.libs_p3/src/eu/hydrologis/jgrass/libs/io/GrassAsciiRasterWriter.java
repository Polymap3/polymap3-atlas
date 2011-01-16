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

import static eu.hydrologis.jgrass.libs.utils.JGrassConstants.isNovalue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import eu.hydrologis.jgrass.libs.map.RasterData;
import eu.hydrologis.jgrass.libs.region.JGrassRegion;
import eu.hydrologis.jgrass.libs.utils.JGrassConstants;

/**
 * This writes JGrass Raster maps to disk in ascii format.
 * 
 * @author Andrea Antonello - www.hydrologis.com
 */
public class GrassAsciiRasterWriter extends MapWriter {

    private String name = null;
    private String outputasciifilepath = null;

    private Object novalue = "*";
    private String LASTFOLDER = JGrassConstants.GRASSASCIIRASTER;

    /**
     * 
     */
    public GrassAsciiRasterWriter() {
        super(MapWriter.RASTER_WRITER);
    }
    public boolean open( String mapPath ) {
        File mapFile = new File(mapPath);
        File dummyMapsetFile = mapFile.getParentFile();
        File dummyLocationFile = dummyMapsetFile.getParentFile();

        LASTFOLDER = ""; //$NON-NLS-1$

        return open(mapFile.getName(), dummyLocationFile.getAbsolutePath(), dummyMapsetFile
                .getName());
    }
    /*
     * (non-Javadoc)
     * 
     * @see jgrass.io.MapWriter#open(java.lang.String) this method tests for the existence of the
     *      needed folder structure and creates missing parts. The dataOutputType for the map to be
     *      created is checked
     */
    public boolean open( String fileName, String locationPath, String mapsetName ) {
        if (dataWindow != null) {
            name = fileName;

            outputasciifilepath = locationPath + File.separator + mapsetName + File.separator
                    + LASTFOLDER + File.separator + name;

            File check = new File(locationPath + File.separator + mapsetName + File.separator
                    + LASTFOLDER);
            if (!check.exists())
                return false;
        } else {
            return false;
        }
        return true;
    }

    /**
     * this method writes the new map using the geographic region and settings of the active region
     * (dataWindow). Parameter is the dataobject holding the data
     */
    /*
     * (non-Javadoc)
     * 
     * @see jgrass.io.MapWriter#write(java.lang.Object)
     */
    public boolean write( Object dataObject ) throws Exception {
        /*
         * the file for the map to create
         */
        BufferedWriter os = new BufferedWriter(new FileWriter(outputasciifilepath));

        /*
         * finally writing to disk
         */
        StringBuffer header = new StringBuffer();

        // grass ascii grid
        header.append("north:" + dataWindow.getNorth() + "\n");
        header.append("south:" + dataWindow.getSouth() + "\n");
        header.append("east:" + dataWindow.getEast() + "\n");
        header.append("west:" + dataWindow.getWest() + "\n");
        header.append("rows:" + dataWindow.getRows() + "\n");
        header.append("cols:" + dataWindow.getCols() + "\n");

        os.write(header.toString());

        if (dataObject instanceof RasterData) {
            for( int i = 0; i < dataWindow.getRows(); i++ ) {
                for( int j = 0; j < dataWindow.getCols(); j++ ) {
                    double value = ((RasterData) dataObject).getValueAt(i, j);
                    if (isNovalue(value)) {
                        os.write(novalue + " ");
                    } else {
                        os.write(value + " ");
                    }
                }
                os.write("\n");
            }
        } else if (dataObject instanceof double[][]) {
            double[][] dataArray = (double[][]) dataObject;
            for( int i = 0; i < dataWindow.getRows(); i++ ) {
                for( int j = 0; j < dataWindow.getCols(); j++ ) {
                    double value = dataArray[i][j];
                    if (isNovalue(value)) {
                        os.write(novalue + " ");
                    } else {
                        os.write(value + " ");
                    }
                }
                os.write("\n");
            }
        } else if (dataObject instanceof float[][]) {
            float[][] dataArray = (float[][]) dataObject;
            for( int i = 0; i < dataWindow.getRows(); i++ ) {
                for( int j = 0; j < dataWindow.getCols(); j++ ) {
                    float value = dataArray[i][j];
                    if (isNovalue(value)) {
                        os.write(novalue + " ");
                    } else {
                        os.write(value + " ");
                    }
                }
                os.write("\n");
            }
        } else if (dataObject instanceof int[][]) {
            int[][] dataArray = (int[][]) dataObject;
            for( int i = 0; i < dataWindow.getRows(); i++ ) {
                for( int j = 0; j < dataWindow.getCols(); j++ ) {
                    int value = dataArray[i][j];
                    if (isNovalue(value)) {
                        os.write(novalue + " ");
                    } else {
                        os.write(value + " ");
                    }
                }
                os.write("\n");
            }
        }

        os.close();

        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see jgrass.io.MapWriter#setParameter(java.lang.String, java.lang.Object)
     */
    public void setParameter( String key, Object obj ) {
        if (key.equals("novalue")) {
            novalue = obj;
        }
    }

    public void setDataWindow( JGrassRegion window ) {
        dataWindow = window;
    }

}
