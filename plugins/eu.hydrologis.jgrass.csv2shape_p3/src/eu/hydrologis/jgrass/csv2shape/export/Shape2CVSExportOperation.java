/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package eu.hydrologis.jgrass.csv2shape.export;

import java.io.BufferedWriter;
import java.io.FileWriter;

import net.refractions.udig.project.ILayer;
import net.refractions.udig.project.ui.ApplicationGIS;
import net.refractions.udig.ui.operations.IOp;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

/**
 * Operation export a layer in CSV. If points, the thing is easy, if lines or polygons, the
 * coordinates are put in triplets line by line before the last one that has also all the attributes
 * in the same line.
 * 
 * @author Andrea Antonello - www.hydrologis.com
 */
public class Shape2CVSExportOperation implements IOp {
    private boolean goGo = false;
    private String path;

    /*
     * (non-Javadoc)
     * 
     * @see net.refractions.udig.ui.operations.IOp#op(org.eclipse.swt.widgets.Display,
     *      java.lang.Object, org.eclipse.core.runtime.IProgressMonitor)
     */
    public void op( Display display, Object target, IProgressMonitor monitor ) throws Exception {
        FeatureSource<SimpleFeatureType, SimpleFeature> source = (FeatureSource<SimpleFeatureType, SimpleFeature>) target;

        goGo = false;

        Display.getDefault().asyncExec(new Runnable(){

            public void run() {
                FileDialog fileDialog = new FileDialog(new Shell(Display.getDefault()), SWT.SAVE);
                fileDialog.setText("Choose were to save the output HydroCSV file:");
                path = fileDialog.open();
                goGo = true;
            }
        });
        while( !goGo ) {
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        goGo = false;

        if (path == null || path.length() < 1) {
            return;
        }

        /*
         * take selection if one, else the whole layer
         */
        ILayer selectedLayer = ApplicationGIS.getActiveMap().getEditManager().getSelectedLayer();

        FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection = source
                .getFeatures(selectedLayer.getFilter());
        FeatureIterator<SimpleFeature> featureIterator = featureCollection.features();
        // if no fetaures in the selection, use the whole layer
        if (!featureIterator.hasNext()) {
            featureCollection = source.getFeatures();
            featureIterator = featureCollection.features();
        }

        /*
         * count the features for monitoring
         */
        int fcount = 0;
        while( featureIterator.hasNext() ) {
            featureIterator.next();
            fcount++;
        }

        BufferedWriter bW = new BufferedWriter(new FileWriter(path));
        try {
            monitor.beginTask("Exporting layer...", fcount);
            featureIterator = featureCollection.features();
            while( featureIterator.hasNext() ) {
                monitor.worked(1);
                SimpleFeature tmpFeature = featureIterator.next();

                /*
                 * the geometry
                 */
                Geometry featureGeometry = (Geometry) tmpFeature.getDefaultGeometry();
                Coordinate[] coordinates = featureGeometry.getCoordinates();
                for( int i = 0; i < coordinates.length - 1; i++ ) {
                    bW.write(String.valueOf(coordinates[i].x));
                    bW.write(",");
                    bW.write(String.valueOf(coordinates[i].y));
                    if (coordinates[i].z == coordinates[i].z) {
                        bW.write(",");
                        bW.write(String.valueOf(coordinates[i].z));
                    }
                    bW.write("\n");
                }
                // last line
                bW.write(String.valueOf(coordinates[coordinates.length - 1].x));
                bW.write(",");
                bW.write(String.valueOf(coordinates[coordinates.length - 1].y));
                if (coordinates[coordinates.length - 1].z == coordinates[coordinates.length - 1].z) {
                    bW.write(",");
                    bW.write(String.valueOf(coordinates[coordinates.length - 1].z));
                }
                /*
                 * the attributes
                 */
                for( int i = 0; i < tmpFeature.getAttributeCount(); i++ ) {
                    Object attribute = tmpFeature.getAttribute(i);
                    if (!(attribute instanceof Geometry)) {
                        bW.write(",");
                        bW.write(String.valueOf(attribute));
                    }
                }
                bW.write("\n");
            }
            bW.close();
            monitor.done();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}