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
package eu.hydrologis.jgrass.libs.map;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import net.refractions.udig.catalog.IGeoResource;

import eu.hydrologis.jgrass.libs.JGrassLibsPlugin;
import eu.hydrologis.jgrass.libs.io.MapIOFactory;
import eu.hydrologis.jgrass.libs.io.MapWriter;
import eu.hydrologis.jgrass.libs.io.RasterWritingFailureException;
import eu.hydrologis.jgrass.libs.region.JGrassRegion;
import eu.hydrologis.jgrass.libs.utils.FileUtilities;
import eu.hydrologis.jgrass.libs.utils.JGrassConstants;
import eu.hydrologis.jgrass.libs.utils.monitor.DummyProgressMonitor;
import eu.hydrologis.jgrass.libs.utils.monitor.IProgressMonitorJGrass;

/**
 * <p>
 * Facility to write JGrass maps
 * </p>
 * 
 * @author Andrea Antonello - www.hydrologis.com
 * @since 1.1.0
 */
public class JGrassRasterMapWriter {

    private MapWriter writer = null;
    private IGeoResource resource = null;
    private IProgressMonitorJGrass monitor = new DummyProgressMonitor();
    private String mapName = null;
    private String mapsetName = null;
    private String locationPath = null;
    private String mapPath;
    private String fullMapPath = null;

    /**
     * Creates a jgrass raster map writer with some default values (data are read as double values,
     * map is grass binary).
     * 
     * @param writeWindow the region to read
     * @param _resource the igeoresource (udig) to read from
     * @param _novalue the number to use internally instead of the map novalues
     * @param _mapType the raster map type to read (ex. {@link JGrassConstants#GRASSBINARYRASTERMAP})
     * @param _monitor a monitor object (if no monitro present, {@link NullProgressMonitor} can be
     *        used)
     */
    public JGrassRasterMapWriter( JGrassRegion writeWindow, IGeoResource _resource, Object novalue,
            String _mapType, IProgressMonitorJGrass _monitor ) {
        resource = _resource;
        monitor = _monitor;
        writer = MapIOFactory.CreateRasterMapWriter(_mapType);
        writer.setDataWindow(writeWindow);
        writer.setParameter("novalue", novalue);
        writer.setOutputDataObject(new Double(2)); // write data to
        writer.setHistoryComment("Created by JGrass in " + new Date().toString());

        fullMapPath = resource.getIdentifier().toExternalForm().replaceFirst("#", "").substring(5);
    }

    /**
     * Creates a jgrass raster map writer with some default values (data are read as double values,
     * map is grass binary).
     * 
     * @param readWindow the region to read
     * @param resource the igeoresource (udig) to read from
     * @param mapType the raster map type to read (ex. {@link JGrassConstants#GRASSBINARYRASTERMAP})
     * @param monitor a monitor object (if no monitro present, {@link NullProgressMonitor} can be
     *        used)
     */
    public JGrassRasterMapWriter( JGrassRegion writeWindow, IGeoResource _resource,
            String _mapType, IProgressMonitorJGrass _monitor ) {
        this(writeWindow, _resource, JGrassConstants.doubleNovalue, _mapType, _monitor);
    }

    /**
     * Creates a jgrass raster map writer with some default values (data are read as double values,
     * map is grass binary).
     * 
     * @param readWindow the region to read
     * @param resource the igeoresource (udig) to read from
     * @param novalue the value to write as novalue
     * @param monitor a monitor object (if no monitro present, {@link NullProgressMonitor} can be
     *        used)
     */
    public JGrassRasterMapWriter( JGrassRegion writeWindow, IGeoResource _resource, Object novalue,
            IProgressMonitorJGrass _monitor ) {
        this(writeWindow, _resource, novalue, JGrassConstants.GRASSBINARYRASTERMAP, _monitor);
    }

    /**
     * Creates a jgrass raster map reader with some default values (data are read as double values,
     * novalue is default {@link JGrassConstants#defaultNovalue}).
     * 
     * @param readWindow the region to read
     * @param resource the igeoresource (udig) to read from
     * @param monitor a monitor object (if no monitro present, {@link NullProgressMonitor} can be
     *        used)
     */
    public JGrassRasterMapWriter( JGrassRegion writeWindow, IGeoResource _resource,
            IProgressMonitorJGrass _monitor ) {
        this(writeWindow, _resource, new Double(JGrassConstants.doubleNovalue),
                JGrassConstants.GRASSBINARYRASTERMAP, _monitor);
    }

    /**
     * <p>
     * Creates a jgrass raster map writer with some default values
     * </p>
     * <p>
     * <b>NOTE:</b> This doesn't need a working udig environment to run. It just uses paths.
     * Thought also for batch usage.
     * </p>
     * 
     * @param writeWindow the region to read
     * @param mapName the name of the map
     * @param mapsetName the name of the mapset
     * @param locationPath the path to the location
     * @param _novalue the value to write as novalue
     * @param mapType the raster map type to read (ex. {@link JGrassConstants#GRASSBINARYRASTERMAP})
     * @param monitor a monitor object (if no monitro present, {@link NullProgressMonitor} can be
     *        used)
     */
    public JGrassRasterMapWriter( JGrassRegion writeWindow, String mapName, String mapsetName,
            String locationPath, Object novalue, String mapType, IProgressMonitorJGrass monitor ) {
        this.monitor = monitor;
        this.mapName = mapName;
        this.mapsetName = mapsetName;
        this.locationPath = locationPath;
        writer = MapIOFactory.CreateRasterMapWriter(mapType);
        writer.setDataWindow(writeWindow);
        writer.setParameter("novalue", novalue);
        writer.setOutputDataObject(new Double(2)); // write data to
        writer.setHistoryComment("Created by JGrass in " + new Date().toString());

        fullMapPath = locationPath + File.separator + mapsetName + File.separator
                + JGrassConstants.CELL + File.separator + mapName;

    }
    /**
     * <p>
     * Creates a jgrass raster map writer with some default values
     * </p>
     * <p>
     * <b>NOTE:</b> This doesn't need a working udig environment to run. It just uses paths.
     * Thought also for batch usage.
     * </p>
     * 
     * @param writeWindow the region to read
     * @param mapName the name of the map
     * @param mapsetName the name of the mapset
     * @param locationPath the path to the location
     * @param _novalue the value to write as novalue
     * @param mapType the raster map type to read (ex. {@link JGrassConstants#GRASSBINARYRASTERMAP})
     * @param monitor a monitor object (if no monitro present, {@link NullProgressMonitor} can be
     *        used)
     */
    public JGrassRasterMapWriter( JGrassRegion writeWindow, String mapPath, Object novalue,
            String mapType, IProgressMonitorJGrass monitor ) {
        this.monitor = monitor;
        this.mapPath = mapPath;
        writer = MapIOFactory.CreateRasterMapWriter(mapType);
        writer.setDataWindow(writeWindow);
        writer.setParameter("novalue", novalue);
        writer.setOutputDataObject(new Double(2)); // write data to
        writer.setHistoryComment("Created by JGrass in " + new Date().toString());

        fullMapPath = mapPath;

    }

    /**
     * <p>
     * Creates a jgrass raster map writer with some default values
     * </p>
     * <p>
     * <b>NOTE:</b> This doesn't need a working udig environment to run. It just uses paths.
     * Thought also for batch usage.
     * </p>
     * 
     * @param writeWindow the region to read
     * @param mapName the name of the map
     * @param mapsetName the name of the mapset
     * @param locationPath the path to the location
     * @param _novalue the value to write as novalue
     * @param monitor a monitor object (if no monitro present, {@link NullProgressMonitor} can be
     *        used)
     */
    public JGrassRasterMapWriter( JGrassRegion writeWindow, String mapName, String mapsetName,
            String locationPath, Object novalue, IProgressMonitorJGrass monitor ) {

        this(writeWindow, mapName, mapsetName, locationPath, novalue,
                JGrassConstants.GRASSBINARYRASTERMAP, monitor);
    }

    /**
     * <p>
     * Creates a jgrass raster map writer with some default values
     * </p>
     * <p>
     * <b>NOTE:</b> This doesn't need a working udig environment to run. It just uses paths.
     * Thought also for batch usage.
     * </p>
     * 
     * @param writeWindow the region to read
     * @param mapName the name of the map
     * @param mapsetName the name of the mapset
     * @param locationPath the path to the location
     * @param mapType the raster map type to read (ex. {@link JGrassConstants#GRASSBINARYRASTERMAP})
     * @param monitor a monitor object (if no monitro present, {@link NullProgressMonitor} can be
     *        used)
     */
    public JGrassRasterMapWriter( JGrassRegion writeWindow, String mapName, String mapsetName,
            String locationPath, String mapType, IProgressMonitorJGrass monitor ) {

        this(writeWindow, mapName, mapsetName, locationPath, JGrassConstants.doubleNovalue,
                mapType, monitor);
    }

    /**
     * <p>
     * Creates a jgrass raster map reader with some default values (data are read as double values,
     * novalue is default {@link JGrassConstants#defaultNovalue}).
     * </p>
     * <p>
     * <b>NOTE:</b> This doesn't need a working udig environment to run. It just uses paths.
     * Thought also for batch usage.
     * </p>
     * 
     * @param writeWindow the region to read
     * @param mapName the name of the map
     * @param mapsetName the name of the mapset
     * @param locationPath the path to the location
     * @param monitor a monitor object (if no monitro present, {@link NullProgressMonitor} can be
     *        used)
     */
    public JGrassRasterMapWriter( JGrassRegion writeWindow, String mapName, String mapsetName,
            String locationPath, IProgressMonitorJGrass monitor ) {

        this(writeWindow, mapName, mapsetName, locationPath, JGrassConstants.doubleNovalue,
                JGrassConstants.GRASSBINARYRASTERMAP, monitor);
    }

    /**
     * <p>
     * Opens the raster map and does some first checking
     * </p>
     * 
     * @return true if everything went alright
     */
    public boolean open() throws RasterWritingFailureException {
        boolean ok;
        try {
            if (mapPath != null) {
                ok = writer.open(mapPath);
            } else if (resource != null) {
                String mapsesPath = resource.parent(null).getIdentifier().toExternalForm()
                        .replaceFirst("#", "").substring(5);
                String locationPath = new File(mapsesPath).getParent();
                ok = writer.open(
                // map name
                        resource.getInfo(null).getName(),
                        // location path
                        locationPath,
                        // mapset name
                        new File(mapsesPath).getName());
            } else if (locationPath != null && mapsetName != null && mapName != null) {
                ok = writer.open(mapName, locationPath, mapsetName);
            } else {
                return false;
            }
        } catch (IOException e) {
            throw new RasterWritingFailureException(e.getLocalizedMessage());
        }

        return ok;
    }

    /**
     * <p>
     * Write the rasterData to disk.
     * </p>
     * 
     * @param rasterData
     * @return true if everything went well
     * @throws Exception
     */
    public boolean write( RasterData rasterData ) throws RasterWritingFailureException {
        try {
            return writer.write(rasterData);
        } catch (Exception e) {
            JGrassLibsPlugin
                    .log(
                            "JGrassLibsPlugin problem: eu.hydrologis.jgrass.libs.map#JGrassRasterMapWriter#write", e); //$NON-NLS-1$
            e.printStackTrace();
            throw new RasterWritingFailureException(e.getLocalizedMessage());
        }

    }

    public void close() {
        writer.close();
    }

    public String getFullMapPath() {
        return fullMapPath;
    }

    public void cloneColorTableFromReader( JGrassRasterMapReader jgReader ) {
        // start
        String readerMapPath = jgReader.getFullMapPath();
        String tmpMapName = new File(readerMapPath).getName();
        File mapsetFile = new File(readerMapPath).getParentFile().getParentFile();
        String colorFilePath = mapsetFile.getAbsolutePath() + File.separator + JGrassConstants.COLR
                + File.separator + tmpMapName;
        // destination
        String destMapName = new File(fullMapPath).getName();
        File destMapsetFile = new File(fullMapPath).getParentFile().getParentFile();
        String destColorFilePath = destMapsetFile.getAbsolutePath() + File.separator
                + JGrassConstants.COLR + File.separator + destMapName;

        // copy it over
        FileUtilities.copyFile(colorFilePath, destColorFilePath);
    }

    public void cloneCategoriesFromReader( JGrassRasterMapReader jgReader ) {
        // start
        String readerMapPath = jgReader.getFullMapPath();
        String tmpMapName = new File(readerMapPath).getName();
        File mapsetFile = new File(readerMapPath).getParentFile().getParentFile();
        String catsFilePath = mapsetFile.getAbsolutePath() + File.separator + JGrassConstants.CATS
                + File.separator + tmpMapName;
        // destination
        String destMapName = new File(fullMapPath).getName();
        File destMapsetFile = new File(fullMapPath).getParentFile().getParentFile();
        String destCatsFilePath = destMapsetFile.getAbsolutePath() + File.separator
                + JGrassConstants.CATS + File.separator + destMapName;

        // copy it over
        FileUtilities.copyFile(catsFilePath, destCatsFilePath);
    }

}
