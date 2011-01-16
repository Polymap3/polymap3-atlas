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

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Vector;

import org.eclipse.core.runtime.Platform;

import eu.hydrologis.jgrass.libs.JGrassLibsPlugin;
import eu.hydrologis.jgrass.libs.utils.xml.Tag;
import eu.hydrologis.jgrass.libs.utils.xml.TagExtractor;

/**
 * <p>
 * Factory to create various map readers
 * </p>
 * 
 * @author Andrea Antonello - www.hydrologis.com
 * @author John Preston
 * @since 1.1.0
 */
public class MapIOFactory {
    private static HashMap<String, String> rasterReaderClasses = null;

    private static HashMap<String, String> rasterWriterClasses = null;

    private static HashMap<String, String> vectorReaderClasses = null;

    private static HashMap<String, String> vectorWriterClasses = null;

    private static HashMap<String, String> pointReaderClasses = null;

    private static HashMap<String, String> pointWriterClasses = null;

    /**
     * read the different file loaders available from the handlers.xml file. The
     * classes can then be loaded by reflection.
     */
    @SuppressWarnings("nls")
    private static void loadReaderConfiguration() {
        rasterReaderClasses = new HashMap<String, String>();
        vectorReaderClasses = new HashMap<String, String>();
        pointReaderClasses = new HashMap<String, String>();
        rasterWriterClasses = new HashMap<String, String>();
        vectorWriterClasses = new HashMap<String, String>();
        pointWriterClasses = new HashMap<String, String>();
        
        rasterReaderClasses.put("grassbinaryraster", "eu.hydrologis.jgrass.libs.io.GrassRasterReader");
        rasterReaderClasses.put("grassasciiraster", "eu.hydrologis.jgrass.libs.io.GrassAsciiRasterReader");
        rasterReaderClasses.put("fluidturtleasciiraster", "eu.hydrologis.jgrass.libs.io.FluidturtleAsciiRasterReader");
        rasterReaderClasses.put("esriasciigrid", "eu.hydrologis.jgrass.libs.io.EsriAsciiRasterReader");
        
        vectorReaderClasses.put("grass", "eu.hydrologis.jgrass.libs.io.GrassVectorReader");
        vectorReaderClasses.put("dwg", "eu.hydrologis.jgrass.libs.io.DwgfileReader");
        
        pointReaderClasses.put("grass", "eu.hydrologis.jgrass.libs.io.GrassPointReader");
        
        rasterWriterClasses.put("grassbinaryraster", "eu.hydrologis.jgrass.libs.io.GrassRasterWriter");
        rasterWriterClasses.put("grassasciiraster", "eu.hydrologis.jgrass.libs.io.GrassAsciiRasterWriter");
        rasterWriterClasses.put("fluidturtleasciiraster", "eu.hydrologis.jgrass.libs.io.FluidturtleAsciiRasterWriter");
        rasterWriterClasses.put("esriasciigrid", "eu.hydrologis.jgrass.libs.io.EsriAsciiRasterWriter");
    }

    public static HashMap getMapReaders( int readerType ) {
        /* Test to see if the configuration has been done */
        if (rasterReaderClasses == null)
            loadReaderConfiguration();

        switch( readerType ) {
        case MapReader.RASTER_READER:
            return rasterReaderClasses;
        case MapReader.VECTOR_READER:
            return vectorReaderClasses;
        case MapReader.POINT_READER:
            return pointReaderClasses;
        }
        return new HashMap();
    }

    public static HashMap getMapWriters( int writerType ) {
        /* Test to see if the configuration has been done */
        if (rasterWriterClasses == null)
            loadReaderConfiguration();

        switch( writerType ) {
        case MapWriter.RASTER_WRITER:
            return rasterWriterClasses;
        case MapWriter.VECTOR_WRITER:
            return vectorWriterClasses;
        case MapWriter.POINT_WRITER:
            return pointWriterClasses;
        }
        return new HashMap();
    }

    public static MapReader CreateRasterMapReader( String readerName ) {
        MapReader reader = null;
        if (rasterReaderClasses == null)
            loadReaderConfiguration();
        /* Get class name of reader */
        String readerClass = (String) rasterReaderClasses.get(readerName);
        /* Instantiate reader class */
        try {
            reader = (MapReader) Class.forName(readerClass).newInstance();
            reader.setReaderType(MapReader.RASTER_READER);
            reader.setOutputDataObject(new double[0][0]);
        } catch (Exception ex) {
            // TODO throw exception
        }
        return reader;
    }

    public static MapReader CreateVectorMapReader( String readerName ) {
        MapReader reader = null;
        if (vectorReaderClasses == null)
            loadReaderConfiguration();
        /* Get class name of reader */
        String readerClass = (String) vectorReaderClasses.get(readerName);
        /* Instantiate reader class */
        try {
            reader = (MapReader) Class.forName(readerClass).newInstance();
            reader.setReaderType(MapReader.VECTOR_READER);
            reader.setOutputDataObject(new Vector());
        } catch (Exception ex) {
            // TODO throw exception
        }
        return reader;
    }

    public static MapReader CreatePointMapReader( String readerName ) {
        MapReader reader = null;
        if (pointReaderClasses == null)
            loadReaderConfiguration();
        /* Get class name of reader */
        String readerClass = (String) pointReaderClasses.get(readerName);
        if (readerClass == null)
            return null;
        /* Instantiate reader class */
        try {
            reader = (MapReader) Class.forName(readerClass).newInstance();
            reader.setReaderType(MapReader.POINT_READER);
            reader.setOutputDataObject(new Vector());
        } catch (Exception ex) {
            // TODO throw exception
        }
        return reader;
    }

    public static MapWriter CreateRasterMapWriter( String writerName ) {
        MapWriter writer = null;
        if (rasterWriterClasses == null)
            loadReaderConfiguration();
        /* Get class name of reader */
        String writerClass = (String) rasterWriterClasses.get(writerName);
        /* Instantiate reader class */
        try {
            writer = (MapWriter) Class.forName(writerClass).newInstance();
        } catch (Exception ex) {
            // TODO throw exception
        }
        return writer;
    }

    public static MapWriter CreateVectorMapWriter( String writerName ) {
        MapWriter writer = null;
        if (vectorWriterClasses == null)
            loadReaderConfiguration();
        /* Get class name of reader */
        String writerClass = (String) vectorWriterClasses.get(writerName);
        /* Instantiate reader class */
        try {
            writer = (MapWriter) Class.forName(writerClass).newInstance();
        } catch (Exception ex) {
            // TODO throw exception
        }
        return writer;
    }

    public static MapWriter CreatePointMapWriter( String writerName ) {
        MapWriter writer = null;
        if (pointWriterClasses == null)
            loadReaderConfiguration();
        /* Get class name of reader */
        String writerClass = (String) pointWriterClasses.get(writerName);
        /* Instantiate reader class */
        try {
            writer = (MapWriter) Class.forName(writerClass).newInstance();
        } catch (Exception ex) {
            // TODO throw exception
        }
        return writer;
    }
}
