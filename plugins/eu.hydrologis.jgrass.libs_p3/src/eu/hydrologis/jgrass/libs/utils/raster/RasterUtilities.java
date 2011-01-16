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
package eu.hydrologis.jgrass.libs.utils.raster;

import java.awt.Color;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.media.jai.RasterFactory;

import org.geotools.coverage.Category;
import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.geometry.Envelope2D;
import org.geotools.referencing.operation.builder.GridToEnvelopeMapper;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineSegment;

import eu.hydrologis.jgrass.libs.iodrivers.imageio.io.color.JGrassColorTable;
import eu.hydrologis.jgrass.libs.map.RasterData;
import eu.hydrologis.jgrass.libs.region.JGrassRegion;

/**
 * <p>
 * A class of utilities bound to raster analysis
 * </p>
 * 
 * @author Andrea Antonello - www.hydrologis.com
 * @since 1.1.0
 */
public class RasterUtilities {
    /**
     * Calculates the profile of a raster map.
     * 
     * <p>
     * Calculation of the profile adapted from r.profile of GRASS: Copyright (C)
     * 2000 by the GRASS Development Team Author: Bob Covill
     * <bcovill@tekmap.ns.ca>.
     * </p>
     * 
     * @param x1
     *            first coordinate
     * @param y1
     *            first coordinate
     * @param x2
     *            second coordinate
     * @param y2
     *            second coordinate
     * @param xres
     *            x resolution
     * @param yres
     *            y resolution
     * @param active
     *            the window to take into consideration
     * @param rasterData
     *            the data read from the window
     * @return and array of four rows:
     *         <ol>
     *         <li>easting</li>
     *         <li>northing</li>
     *         <li>progressive value</li>
     *         <li>elevation value</li>
     *         </ol>
     * @deprecated use
     *             {@link RasterUtilities#doProfile(double, double, double, double, double, double, JGrassRegion, RasterData)}
     *             instead, which makes use of jts for profile calculation
     */
    public static double[][] do_profile( double x1, double y1, double x2, double y2, double xres,
            double yres, JGrassRegion active, RasterData rasterData ) {
        double X = 0.0;
        double Y = 0.0;
        double AZI = 0.0;
        double e1 = x1;
        double n1 = y1;
        double e2 = x2;
        double n2 = y2;
        double e = 0.0;
        double n = 0.0;

        double maxvalue = Double.NEGATIVE_INFINITY;
        double minvalue = Double.POSITIVE_INFINITY;

        double progressivedist = 0;
        double xdist = e1 - e2;
        double ydist = n1 - n2;

        Point clickedRowCol = null;

        // calculate the length
        double len = PYTAGORAS(xdist, ydist);
        double resmean = (xres + yres) / 2.0;
        int pointnum = 0;
        if (len % resmean == 0) {
            pointnum = (int) Math.ceil(len / resmean);
        } else {
            pointnum = (int) Math.ceil(len / resmean) + 1;

        }

        // allocate space for everything
        double[][] distanceValueAbsolute = new double[4][pointnum];
        double value = 0.0;

        /* Calculate Azimuth of Line */
        if (ydist == 0 && xdist == 0) {
            /* Special case for no movement */
            return null;
        }

        if (ydist >= 0 && xdist < 0) {
            /* SE Quad or due east */
            AZI = Math.atan((ydist / xdist));
            Y = resmean * Math.sin(AZI);
            X = resmean * Math.cos(AZI);
            if (Y < 0)
                Y = Y * -1.;
            if (X < 0)
                X = X * -1.;
            int i = 0;
            progressivedist = 0 - PYTAGORAS(-X, Y);
            for( e = e1, n = n1; i < pointnum; e += X, n -= Y, i++ ) {
                clickedRowCol = putClickToCenterOfCell(active, new Point2D.Double(e, n));
                value = rasterData.getValueAt(clickedRowCol.x, clickedRowCol.y);

                progressivedist += PYTAGORAS(-X, Y);

                if (value > maxvalue)
                    maxvalue = value;
                if (value < minvalue)
                    minvalue = value;

                distanceValueAbsolute[0][i] = e;
                distanceValueAbsolute[1][i] = n;
                distanceValueAbsolute[2][i] = progressivedist;
                distanceValueAbsolute[3][i] = value;
            }
        }

        if (ydist < 0 && xdist <= 0) {
            /* NE Quad or due north */
            AZI = Math.atan((xdist / ydist));
            X = resmean * Math.sin(AZI);
            Y = resmean * Math.cos(AZI);
            if (Y < 0)
                Y = Y * -1.;
            if (X < 0)
                X = X * -1.;
            int i = 0;
            progressivedist = 0 - PYTAGORAS(-X, -Y);
            for( e = e1, n = n1; i < pointnum; e += X, n += Y, i++ ) {
                clickedRowCol = putClickToCenterOfCell(active, new Point2D.Double(e, n));
                value = rasterData.getValueAt(clickedRowCol.x, clickedRowCol.y);

                progressivedist += PYTAGORAS(-X, -Y);

                if (value > maxvalue)
                    maxvalue = value;
                if (value < minvalue)
                    minvalue = value;

                distanceValueAbsolute[0][i] = e;
                distanceValueAbsolute[1][i] = n;
                distanceValueAbsolute[2][i] = progressivedist;
                distanceValueAbsolute[3][i] = value;
            }
        }

        if (ydist > 0 && xdist >= 0) {
            /* SW Quad or due south */
            AZI = Math.atan((ydist / xdist));
            X = resmean * Math.cos(AZI);
            Y = resmean * Math.sin(AZI);
            if (Y < 0)
                Y = Y * -1.;
            if (X < 0)
                X = X * -1.;
            int i = 0;
            progressivedist = 0 - PYTAGORAS(X, Y);
            for( e = e1, n = n1; i < pointnum; e -= X, n -= Y, i++ ) {
                clickedRowCol = putClickToCenterOfCell(active, new Point2D.Double(e, n));
                value = rasterData.getValueAt(clickedRowCol.x, clickedRowCol.y);

                progressivedist += PYTAGORAS(X, Y);

                if (value > maxvalue)
                    maxvalue = value;
                if (value < minvalue)
                    minvalue = value;

                distanceValueAbsolute[0][i] = e;
                distanceValueAbsolute[1][i] = n;
                distanceValueAbsolute[2][i] = progressivedist;
                distanceValueAbsolute[3][i] = value;
            }
        }

        if (ydist <= 0 && xdist > 0) {
            /* NW Quad or due west */
            AZI = Math.atan((ydist / xdist));
            X = resmean * Math.cos(AZI);
            Y = resmean * Math.sin(AZI);
            if (Y < 0)
                Y = Y * -1.;
            if (X < 0)
                X = X * -1.;
            int i = 0;
            progressivedist = 0 - PYTAGORAS(X, -Y);
            for( e = e1, n = n1; i < pointnum; e -= X, n += Y, i++ ) {
                clickedRowCol = putClickToCenterOfCell(active, new Point2D.Double(e, n));
                value = rasterData.getValueAt(clickedRowCol.x, clickedRowCol.y);

                progressivedist += PYTAGORAS(X, -Y);

                if (value > maxvalue)
                    maxvalue = value;
                if (value < minvalue)
                    minvalue = value;

                distanceValueAbsolute[0][i] = e;
                distanceValueAbsolute[1][i] = n;
                distanceValueAbsolute[2][i] = progressivedist;
                distanceValueAbsolute[3][i] = value;
            }
        }

        // /*
        // * the last point was taken wrong due to the resolution adding, with
        // the last +resmean we
        // * went over the limit. Therefore the last point is subtituted with
        // the value in the
        // second
        // * click.
        // */
        // clickedRowCol = putClickToCenterOfCell(active, new Point2D.Double(e2,
        // n2));
        // value = rasterData.getValueAt(clickedRowCol.x, clickedRowCol.y);
        //
        // progressivedist = len;
        //
        // if (value > maxvalue)
        // maxvalue = value;
        // if (value < minvalue)
        // minvalue = value;
        // distanceValueAbsolute[0][pointnum - 1] = e;
        // distanceValueAbsolute[1][pointnum - 1] = n;
        // distanceValueAbsolute[2][pointnum - 1] = len;
        // distanceValueAbsolute[3][pointnum - 1] = value;

        return distanceValueAbsolute;

    } /* done with do_profile */

    /**
     * Calculates the profile of a raster map between two given
     * {@link Coordinate coordinates}.
     * 
     * @param x1
     *            the easting of the first coordinate
     * @param y1
     *            the northing of the first coordinate
     * @param x2
     *            the easting of the final coordinate
     * @param y2
     *            the northing of the final coordinate
     * @param xres
     *            the x resolution to consider
     * @param yres
     *            the y resolution to consider
     * @param active
     *            the active region object
     * @param rasterData
     *            the raster from which to take the elevations
     * @return a list of double arrays that contain for every point of the
     *         profile progressive, elevation, easting, northing
     */
    public List<Double[]> doProfile( double x1, double y1, double x2, double y2, double xres,
            double yres, JGrassRegion active, RasterData rasterData ) {

        Point clickedRowCol = null;
        Coordinate start = new Coordinate(x1, y1);
        Coordinate end = new Coordinate(x2, y2);
        LineSegment pline = new LineSegment(start, end);

        double lenght = pline.getLength();

        List<Double[]> distanceValueAbsolute = new ArrayList<Double[]>();
        double progressive = 0.0;

        // ad the first point
        clickedRowCol = putClickToCenterOfCell(active, new Point2D.Double(start.x, start.y));
        Double[] d = {0.0, rasterData.getValueAt(clickedRowCol.x, clickedRowCol.y), start.x,
                start.y};
        distanceValueAbsolute.add(d);
        progressive = progressive + xres;

        while( progressive < lenght ) {

            Coordinate c = pline.pointAlong(progressive / lenght);
            clickedRowCol = putClickToCenterOfCell(active, new Point2D.Double(c.x, c.y));
            Double[] v = {progressive, rasterData.getValueAt(clickedRowCol.x, clickedRowCol.y),
                    c.x, c.y};
            distanceValueAbsolute.add(v);
            progressive = progressive + xres;
        }

        // add the last point
        clickedRowCol = putClickToCenterOfCell(active, new Point2D.Double(end.x, end.y));
        Double[] v = {lenght, rasterData.getValueAt(clickedRowCol.x, clickedRowCol.y), end.x, end.y};
        distanceValueAbsolute.add(v);

        return distanceValueAbsolute;
    }

    public static double PYTAGORAS( double x, double y ) {
        return Math.sqrt(Math.pow(x, 2.0) + Math.pow(y, 2.0));
    }

    /**
     * When we click on the map the chosen point will be somewhere inside the
     * hit cell. It is a matter of rounding the northing and easting of the
     * point to get the right row and column that contains the hit. Therefore
     * this method returns the row and col in which the point falls.
     * 
     * @author Andrea Antonello - www.hydrologis.com
     * @param coordinates
     *            - the coordinates of the hit point
     * @return the reviewed point as row, col
     */
    public static Point putClickToCenterOfCell( JGrassRegion active, Point2D coordinates ) {
        double eastingClick = coordinates.getX();
        double northingClick = coordinates.getY();

        double startnorth = active.getNorth();
        double startwest = active.getWest();
        double northdelta = active.getNSResolution();
        double westdelta = active.getWEResolution();
        int clickrow = 0;
        int clickcol = 0;

        for( int i = 0; i < active.getRows(); i++ ) {
            startnorth = startnorth - northdelta;
            if (northingClick > startnorth) {
                clickrow = i;
                break;
            }
        }
        for( int i = 0; i < active.getCols(); i++ ) {
            startwest = startwest + westdelta;
            if (eastingClick < startwest) {
                clickcol = i;
                break;
            }
        }

        return new Point(clickrow, clickcol);
    }

    /**
     * @param active
     * @param coordinates
     * @return the point containing row (point.x) and column (point.y) of the
     *         given coordinate on the grid of the given region
     */
    public static Point putClickToCenterOfCell( JGrassRegion active, Coordinate coordinates ) {
        double eastingClick = coordinates.x;
        double northingClick = coordinates.y;

        double startnorth = active.getNorth();
        double startwest = active.getWest();
        double northdelta = active.getNSResolution();
        double westdelta = active.getWEResolution();
        int clickrow = 0;
        int clickcol = 0;

        for( int i = 0; i < active.getRows(); i++ ) {
            startnorth = startnorth - northdelta;
            if (northingClick > startnorth) {
                clickrow = i;
                break;
            }
        }
        for( int i = 0; i < active.getCols(); i++ ) {
            startwest = startwest + westdelta;
            if (eastingClick < startwest) {
                clickcol = i;
                break;
            }
        }

        return new Point(clickrow, clickcol);
    }

    public static JGrassRegion getJGrassRegionFromGridCoverage( GridCoverage2D gridCoverage ) {
        Envelope envelope = gridCoverage.getEnvelope();
        DirectPosition lowerCorner = envelope.getLowerCorner();
        double[] westSouth = lowerCorner.getCoordinate();
        DirectPosition upperCorner = envelope.getUpperCorner();
        double[] eastNorth = upperCorner.getCoordinate();

        AffineTransform gridToCRS = (AffineTransform) gridCoverage.getGridGeometry().getGridToCRS();
        double xRes = gridToCRS.getScaleX();
        double yRes = -gridToCRS.getScaleY();

        JGrassRegion jgrassRegion = new JGrassRegion(westSouth[0], eastNorth[0], westSouth[1],
                eastNorth[1], xRes, yRes);
        return jgrassRegion;
    }

    /**
     * Create a {@linkplain Raster raster} on which to write on
     * 
     * @param databufferType
     *            the type of data that has to be hold in the raster. This can
     *            be of type {@link DataBuffer#TYPE_DOUBLE
     *            DataBuffer.TYPE_DOUBLE}, {@link DataBuffer#TYPE_FLOAT
     *            DataBuffer.TYPE_FLOAT}, {@link DataBuffer#TYPE_INT
     *            DataBuffer.TYPE_INT}.
     * @return the created raster.
     */
    public static GridCoverage2D createWritableGridCoverage( String name, WritableRaster raster,
            int databufferType, JGrassRegion writeRegion, double[] dataRange,
            List<String> colorRulesList, CoordinateReferenceSystem crs ) {
        int rows = writeRegion.getRows();
        int cols = writeRegion.getCols();

        /*
         * create a single band double raster
         */
        if (raster == null)
            raster = RasterFactory.createBandedRaster(databufferType, cols, rows, 1, null);

        GridSampleDimension band = null;
        if (dataRange == null || colorRulesList == null) {
            band = new GridSampleDimension(name, new Category[]{}, null);
        } else {

            int rulesNum = colorRulesList.size();
            int COLORNUM = 60000;

            if (colorRulesList.size() > COLORNUM) {
                COLORNUM = colorRulesList.size() + 1;
            }
            if (COLORNUM > 65500) {
                COLORNUM = 65500;
            }

            List<Category> catsList = new ArrayList<Category>();

            double[][] values = new double[rulesNum][2];
            Color[][] colors = new Color[rulesNum][2];
            for( int i = 0; i < rulesNum; i++ ) {
                String colorRule = colorRulesList.get(i);
                JGrassColorTable.parseColorRule(colorRule, values[i], colors[i]);
            }

            Category noData = new Category("novalue", new Color(Color.WHITE.getRed(), Color.WHITE //$NON-NLS-1$
                    .getGreen(), Color.WHITE.getBlue(), 0), 0);
            catsList.add(noData);

            double a = (values[values.length - 1][1] - values[0][0]) / (double) (COLORNUM - 1);
            double pmin = 1;
            double scale = a;
            double offSet = values[0][0] - scale * pmin;

            for( int i = 0; i < rulesNum; i++ ) {
                StringBuilder sB = new StringBuilder();
                sB.append(name);
                sB.append("_"); //$NON-NLS-1$
                sB.append(i);

                int lower = (int) ((values[i][0] - values[0][0]) / scale + pmin);
                int upper = (int) ((values[i][1] - values[0][0]) / scale + pmin);
                if (lower == upper)
                    upper = upper + 1;
                Category dataCategory = new Category(sB.toString(), colors[i], lower, upper, scale,
                        offSet);

                catsList.add(dataCategory);
            }
            Category[] array = (Category[]) catsList.toArray(new Category[catsList.size()]);
            band = new GridSampleDimension(name, array, null);
        }

        band = band.geophysics(true);

        double west = writeRegion.getWest();
        double east = writeRegion.getEast();
        double north = writeRegion.getNorth();
        double south = writeRegion.getSouth();

        Envelope2D writeEnvelope = new Envelope2D(crs, west, south, east - west, north - south);
        GridCoverageFactory factory = CoverageFactoryFinder.getGridCoverageFactory(null);

        GridCoverage2D coverage2D = factory.create(name, raster, writeEnvelope,
                new GridSampleDimension[]{band});

        return coverage2D;
    }
}
