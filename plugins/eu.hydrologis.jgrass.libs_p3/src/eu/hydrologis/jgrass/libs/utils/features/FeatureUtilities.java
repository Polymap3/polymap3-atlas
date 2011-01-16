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
package eu.hydrologis.jgrass.libs.utils.features;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import net.refractions.udig.catalog.CatalogPlugin;
import net.refractions.udig.catalog.ICatalog;
import net.refractions.udig.catalog.IGeoResource;
import net.refractions.udig.catalog.IResolve;
import net.refractions.udig.catalog.IService;
import net.refractions.udig.catalog.IServiceFactory;
//import net.refractions.udig.project.IMap;
//import net.refractions.udig.project.ui.ApplicationGIS;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateList;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

import eu.hydrologis.jgrass.libs.JGrassLibsPlugin;
import eu.hydrologis.jgrass.libs.utils.JGrassConstants;
import eu.hydrologis.jgrass.libs.utils.monitor.IProgressMonitorJGrass;

public class FeatureUtilities {

    /**
     * This method orders the line type features contained in a vector of features by creating a
     * coordinate list starting from the first free point of all the features (i.e. point that has
     * no other line starting point in a buffer ddistance of the given threshold) and navigationg
     * all the features to the final free point. The features are assumed to be thought as
     * continuous.
     * 
     * @param featuresVector vector containing the features to order
     * @return the complete list of coordinates of all the features joined
     * @deprecated this method doesn't seem to be consistent in each case, use
     *             {@link #orderLineGeometries(List, double)} instead
     */
    public static CoordinateList orderFeatures( Vector<SimpleFeature> featuresVector,
            double thresHold ) {

        /*
         * first search the feature that is one of the two external points
         */
        SimpleFeature firstFeature = null;
        boolean foundFirst = true;
        boolean foundSecond = true;
        for( SimpleFeature feature : featuresVector ) {
            foundFirst = true;
            foundSecond = true;

            Geometry geom = (Geometry) feature.getDefaultGeometry();
            Coordinate[] coords = geom.getCoordinates();

            Coordinate first = coords[0];
            Coordinate last = coords[coords.length - 1];

            for( SimpleFeature compareFeature : featuresVector ) {
                Geometry compareGeom = (Geometry) compareFeature.getDefaultGeometry();
                Coordinate[] compareCoords = compareGeom.getCoordinates();

                Coordinate comparefirst = compareCoords[0];
                Coordinate comparelast = compareCoords[compareCoords.length - 1];

                /*
                 * check if the next point is far away
                 */
                if (first.distance(comparefirst) < thresHold
                        || first.distance(comparelast) < thresHold) {
                    foundFirst = false;
                } else if (last.distance(comparefirst) < thresHold
                        || last.distance(comparefirst) < thresHold) {
                    foundSecond = false;
                }

            }
            if (foundFirst || foundSecond) {
                firstFeature = feature;
                break;
            }

        }

        CoordinateList coordinateList = new CoordinateList();
        Coordinate[] coords = ((Geometry) firstFeature.getDefaultGeometry()).getCoordinates();
        addCoordsInProperDirection(foundFirst, coordinateList, coords, true, 0);

        featuresVector.remove(firstFeature);

        Coordinate currentCoordinate = coordinateList.getCoordinate(coordinateList.size() - 1);
        try {

            while( featuresVector.size() != 0 ) {

                for( int j = 0; j < featuresVector.size(); j++ ) {
                    SimpleFeature tmpFeature = featuresVector.elementAt(j);
                    Geometry compareGeom = (Geometry) tmpFeature.getDefaultGeometry();
                    Coordinate[] compareCoords = compareGeom.getCoordinates();

                    Coordinate comparefirst = compareCoords[0];
                    Coordinate comparelast = compareCoords[compareCoords.length - 1];

                    /*
                     * check if the next point is far away
                     */
                    if (currentCoordinate.distance(comparefirst) < thresHold) {
                        addCoordsInProperDirection(true, coordinateList, compareCoords, false, 1);
                        currentCoordinate = new Coordinate(comparelast);
                        featuresVector.remove(tmpFeature);
                        break;
                    } else if (currentCoordinate.distance(comparelast) < thresHold) {
                        addCoordsInProperDirection(false, coordinateList, compareCoords, false, 1);
                        currentCoordinate = new Coordinate(comparefirst);
                        featuresVector.remove(tmpFeature);
                        break;
                    }

                }

            }
        } catch (Exception e) {
            JGrassLibsPlugin.log("JGrassLibsPlugin problem", e); //$NON-NLS-1$
            e.printStackTrace();
        }

        return coordinateList;
    }

    /**
     * Order the geometries of a list to be all directed in the same direction
     * 
     * @param geometryList the list of geometries to be ordered
     * @param thresHold a scalar value that defines the max distance between two points to be the
     *        same
     * @return a list of ordered coordinates
     */
    @SuppressWarnings("unchecked")
    public static CoordinateList orderLineGeometries( List<Geometry> geometryList, double thresHold ) {
        /*
         * first search the feature that is one of the two external points
         */
        Geometry firstFeature = null;
        boolean foundFirst = true;
        boolean foundSecond = true;
        for( Geometry feature : geometryList ) {
            foundFirst = true;
            foundSecond = true;

            Coordinate[] coords = feature.getCoordinates();

            Coordinate first = coords[0];
            Coordinate last = coords[coords.length - 1];

            for( Geometry compareFeature : geometryList ) {
                if (compareFeature.equals(feature))
                    continue;
                Coordinate[] compareCoords = compareFeature.getCoordinates();

                Coordinate comparefirst = compareCoords[0];
                Coordinate comparelast = compareCoords[compareCoords.length - 1];

                /*
                 * check if the next point is far away
                 */
                if (first.distance(comparefirst) < thresHold
                        || first.distance(comparelast) < thresHold) {
                    foundFirst = false;
                }
                if (last.distance(comparefirst) < thresHold
                        || last.distance(comparelast) < thresHold) {
                    foundSecond = false;
                }

            }
            if (foundFirst || foundSecond) {
                firstFeature = feature;
                break;
            }

        }

        CoordinateList coordinateList = new CoordinateList();
        Coordinate[] coords = firstFeature.getCoordinates();
        if (foundSecond) {
            for( int i = 0; i < coords.length; i++ ) {
                coordinateList.add(coords[coords.length - i - 1]);
            }
        } else {
            for( int i = 0; i < coords.length; i++ ) {
                coordinateList.add(coords[i]);
            }
        }

        // if (foundFirst) {
        // addCoordsInProperDirection(foundFirst, coordinateList, coords, true,
        // 0);
        // }else{
        // addCoordsInProperDirection(foundSecond, coordinateList, coords, true,
        // 0);
        // }

        geometryList.remove(firstFeature);

        Coordinate currentCoordinate = coordinateList.getCoordinate(coordinateList.size() - 1);
        try {
            while( geometryList.size() != 0 ) {

                for( int j = 0; j < geometryList.size(); j++ ) {
                    System.out.println(j);
                    Geometry compareGeom = geometryList.get(j);
                    Coordinate[] compareCoords = compareGeom.getCoordinates();

                    Coordinate comparefirst = compareCoords[0];
                    Coordinate comparelast = compareCoords[compareCoords.length - 1];

                    // System.out.println(j + " "
                    // + currentCoordinate.distance(comparefirst) + " "
                    // + currentCoordinate.distance(comparelast));

                    /*
                     * check if the next point is far away
                     */
                    if (currentCoordinate.distance(comparefirst) < thresHold) {
                        for( int i = 0; i < compareCoords.length; i++ ) {
                            coordinateList.add(compareCoords[i]);
                        }
                        currentCoordinate = new Coordinate(comparelast);
                        geometryList.remove(compareGeom);
                        break;
                    } else if (currentCoordinate.distance(comparelast) < thresHold) {
                        for( int i = 0; i < compareCoords.length; i++ ) {
                            coordinateList.add(compareCoords[compareCoords.length - i - 1]);
                        }
                        currentCoordinate = new Coordinate(comparefirst);
                        geometryList.remove(compareGeom);
                        break;
                    }

                }

            }
        } catch (Exception e) {
            JGrassLibsPlugin.log("JGrassLibsPlugin problem", e); //$NON-NLS-1$
            e.printStackTrace();
        }

        return coordinateList;
    }

    /**
     * @param isForward are the coordinates going in the proper direction?
     * @param coordinateListToFill the coordinate list to fill
     * @param coordsToAdd array of coords to add
     * @param isFirst is this the first point of the line?
     * @param fromArrayPosition from which position of the array start to write the coords (usefull
     *        if you don't want to duplicate values that are the same in the last of array before
     *        and first in the array after)
     */
    @SuppressWarnings("unchecked")
    private static void addCoordsInProperDirection( boolean isForward,
            CoordinateList coordinateListToFill, Coordinate[] coordsToAdd, boolean isFirst,
            int fromArrayPosition ) {
        if (isFirst) {
            if (isForward) {
                for( int i = fromArrayPosition; i < coordsToAdd.length; i++ ) {
                    coordinateListToFill.add(coordsToAdd[i]);
                }
            } else {
                for( int i = fromArrayPosition; i < coordsToAdd.length; i++ ) {
                    coordinateListToFill.add(coordsToAdd[coordsToAdd.length - 1 - i]);
                }
            }
        } else {
            if (isForward) {
                for( int i = 0; i < coordsToAdd.length - fromArrayPosition; i++ ) {
                    coordinateListToFill.add(coordsToAdd[i]);
                }
            } else {
                for( int i = fromArrayPosition; i < coordsToAdd.length; i++ ) {
                    coordinateListToFill.add(coordsToAdd[i]);
                }
            }
        }

    }

    /**
     * @param outputFile
     * @param addToCatalog
     * @param addToActiveMap
     * @param progressMonitor
     */
    public static synchronized void addServiceToCatalogAndMap( String outputFile,
            boolean addToCatalog, boolean addToActiveMap, IProgressMonitor progressMonitor ) {
        try {
            URL fileUrl = new File(outputFile).toURI().toURL();
            if (addToCatalog) {
                IServiceFactory sFactory = CatalogPlugin.getDefault().getServiceFactory();
                ICatalog catalog = CatalogPlugin.getDefault().getLocalCatalog();
                List<IService> services = sFactory.createService(fileUrl);
                for( IService service : services ) {
                    catalog.add(service);
                    if (addToActiveMap) {
                        IMap activeMap = ApplicationGIS.getActiveMap();
                        int layerNum = activeMap.getMapLayers().size();
                        List<IResolve> members = service.members(progressMonitor);
                        for( IResolve iRes : members ) {
                            if (iRes.canResolve(IGeoResource.class)) {
                                IGeoResource geoResource = iRes.resolve(IGeoResource.class,
                                        progressMonitor);
                                ApplicationGIS.addLayersToMap(null, Collections
                                        .singletonList(geoResource), layerNum);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            JGrassLibsPlugin.log("JGrassLibsPlugin problem", e); //$NON-NLS-1$
            e.printStackTrace();
        }
    }

    /**
     * Fill the prj file with the actual map projection.
     * 
     * @param shapePath the path to the regarding shapefile
     */
    public static void writeProjectionFile( String shapePath ) {
        /*
         * fill a prj file
         */
        IMap activeMap = ApplicationGIS.getActiveMap();
        CoordinateReferenceSystem mapCrs = activeMap.getViewportModel().getCRS();

        String prjPath = null;
        if (shapePath.toLowerCase().endsWith(".shp")) {
            int dotLoc = shapePath.lastIndexOf(".");
            prjPath = shapePath.substring(0, dotLoc);
            prjPath = prjPath + ".prj";
        } else {

            prjPath = shapePath + ".prj";
        }

        BufferedWriter bufferedWriter = null;
        try {
            bufferedWriter = new BufferedWriter(new FileWriter(prjPath));
            bufferedWriter.write(mapCrs.toWKT());
            bufferedWriter.close();
        } catch (IOException e) {
            JGrassLibsPlugin.log("JGrassLibsPlugin problem", e); //$NON-NLS-1$
            e.printStackTrace();
        }

    }

    /**
     * Create a featurecollection from a vector of features
     * 
     * @param features - the vectore of features
     * @return the created featurecollection
     */
    public static FeatureCollection<SimpleFeatureType, SimpleFeature> createFeatureCollection(
            SimpleFeature... features ) {
        FeatureCollection<SimpleFeatureType, SimpleFeature> fcollection = FeatureCollections
                .newCollection();

        for( SimpleFeature feature : features ) {
            fcollection.add(feature);
        }
        return fcollection;
    }

    /**
     * <p>
     * Convert a csv file to a FeatureCollection. 
     * <b>This for now supports only point geometries</b>.<br>
     * For different crs it also performs coor transformation.
     * </p>
     * <p>
     * <b>NOTE: this doesn't support date attributes</b>
     * </p>
     * 
     * @param csvFile the csv file.
     * @param crs the crs to use.
     * @param fieldsAndTypes the {@link Map} of filed names and {@link JGrassConstants#CSVTYPESARRAY types}.
     * @param pm progress monitor.
     * @param separatorthe separator to use, if null, comma is used.
     * @return the created {@link FeatureCollection}
     * @throws Exception
     */
    @SuppressWarnings("nls")
    public static FeatureCollection<SimpleFeatureType, SimpleFeature> csvFileToFeatureCollection(
            File csvFile, CoordinateReferenceSystem crs,
            LinkedHashMap<String, Integer> fieldsAndTypesIndex, String separator,
            IProgressMonitorJGrass pm ) throws Exception {
        GeometryFactory gf = new GeometryFactory();
        Map<String, Class> typesMap = JGrassConstants.CSVTYPESCLASSESMAP;
        String[] typesArray = JGrassConstants.CSVTYPESARRAY;

        if (separator == null) {
            separator = ",";
        }

        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("csvimport");
        b.setCRS(crs);
        b.add("the_geom", Point.class);

        int xIndex = -1;
        int yIndex = -1;
        Set<String> fieldNames = fieldsAndTypesIndex.keySet();
        String[] fieldNamesArray = (String[]) fieldNames.toArray(new String[fieldNames.size()]);
        for( int i = 0; i < fieldNamesArray.length; i++ ) {
            String fieldName = fieldNamesArray[i];
            Integer typeIndex = fieldsAndTypesIndex.get(fieldName);

            if (typeIndex == 0) {
                xIndex = i;
            } else if (typeIndex == 1) {
                yIndex = i;
            } else {
                Class class1 = typesMap.get(typesArray[typeIndex]);
                b.add(fieldName, class1);
            }
        }
        SimpleFeatureType featureType = b.buildFeatureType();

        FeatureCollection<SimpleFeatureType, SimpleFeature> newCollection = FeatureCollections
                .newCollection();
        try {
            Collection<Integer> orderedTypeIndexes = fieldsAndTypesIndex.values();
            Integer[] orderedTypeIndexesArray = (Integer[]) orderedTypeIndexes
                    .toArray(new Integer[orderedTypeIndexes.size()]);

            BufferedReader bR = new BufferedReader(new FileReader(csvFile));
            String line = null;
            int featureId = 0;
            pm.beginTask("Importing raw data", -1);
            while( (line = bR.readLine()) != null ) {
                pm.worked(1);
                if (line.startsWith("#")) {
                    continue;
                }

                SimpleFeatureBuilder builder = new SimpleFeatureBuilder(featureType);
                Object[] values = new Object[fieldNames.size() - 1];

                String[] lineSplit = line.split(separator);
                double x = Double.parseDouble(lineSplit[xIndex]);
                double y = Double.parseDouble(lineSplit[yIndex]);
                Point point = gf.createPoint(new Coordinate(x, y));
                values[0] = point;

                int objIndex = 1;
                for( int i = 0; i < lineSplit.length; i++ ) {
                    if (i == xIndex || i == yIndex) {
                        continue;
                    }

                    String value = lineSplit[i];
                    int typeIndex = orderedTypeIndexesArray[i];
                    String typeName = typesArray[typeIndex];
                    if (typeName.equals(typesArray[3])) {
                        values[objIndex] = value;
                    } else if (typeName.equals(typesArray[4])) {
                        values[objIndex] = new Double(value);
                    } else if (typeName.equals(typesArray[5])) {
                        values[objIndex] = new Integer(value);
                    } else {
                        throw new IllegalArgumentException("An undefined value type was found");
                    }
                    objIndex++;
                }
                builder.addAll(values);

                SimpleFeature feature = builder.buildFeature(featureType.getTypeName() + "."
                        + featureId);
                featureId++;
                newCollection.add(feature);
            }
            bR.close();
            pm.done();

        } catch (Exception e) {
            JGrassLibsPlugin.log("JGrassLibsPlugin problem", e); //$NON-NLS-1$
            e.printStackTrace();
        }
        return newCollection;
    }

    /**
     * Reproject a geometry
     * 
     * @param from the starting crs
     * @param to the destination crs
     * @param geometries the array of geometries, wrapped into an Object array
     * @throws Exception
     */
    public static void reproject( CoordinateReferenceSystem from, CoordinateReferenceSystem to,
            Object[] geometries ) throws Exception {
        // if no from crs, use the map's one
        if (from == null) {
            from = ApplicationGIS.getActiveMap().getViewportModel().getCRS();
        }
        // if no to crs, use lat/long wgs84
        if (to == null) {
            to = CRS.decode("EPSG:4326"); //$NON-NLS-1$
        }
        MathTransform mathTransform = CRS.findMathTransform(from, to);

        for( int i = 0; i < geometries.length; i++ ) {
            geometries[i] = JTS.transform((Geometry) geometries[i], mathTransform);
        }
    }

    /**
     * @param fet the featurecollection for which to create a temporary layer resource
     */
    public static void featureCollectionToTempLayer( FeatureCollection fet ) {
        IGeoResource resource = CatalogPlugin.getDefault().getLocalCatalog()
                .createTemporaryResource(fet.getSchema());
        try {

            FeatureStore fStore = resource.resolve(FeatureStore.class, new NullProgressMonitor());
            fStore.addFeatures(fet);
        } catch (IOException e) {
            e.printStackTrace();
        }
        ApplicationGIS.addLayersToMap(ApplicationGIS.getActiveMap(), Collections
                .singletonList(resource), -1);
    }

    /**
     * <p>
     * The easy way to create a shapefile from attributes and geometries
     * </p>
     * <p>
     * <b>NOTE: this doesn't support date attributes</b>
     * </p>
     * 
     * @param shapeFilePath the shapefile name
     * @param crs the destination crs
     * @param fet the featurecollection
     */
    public static synchronized boolean collectionToShapeFile( String shapeFilePath,
            CoordinateReferenceSystem crs, FeatureCollection<SimpleFeatureType, SimpleFeature> fet ) {
        try {

            // Create the DataStoreFactory

            // Create the file you want to write to
            File file = null;
            if (shapeFilePath.toLowerCase().endsWith(".shp")) { //$NON-NLS-1$
                file = new File(shapeFilePath);
            } else {
                file = new File(shapeFilePath + ".shp"); //$NON-NLS-1$
            }

            ShapefileDataStoreFactory factory = new ShapefileDataStoreFactory();

            Map<String, Serializable> create = new HashMap<String, Serializable>();
            create.put("url", file.toURI().toURL());
            ShapefileDataStore newDataStore = (ShapefileDataStore) factory
                    .createNewDataStore(create);

            newDataStore.createSchema(fet.getSchema());
            if (crs != null)
                newDataStore.forceSchemaCRS(crs);
            Transaction transaction = new DefaultTransaction();
            FeatureStore<SimpleFeatureType, SimpleFeature> featureStore = (FeatureStore<SimpleFeatureType, SimpleFeature>) newDataStore
                    .getFeatureSource();
            featureStore.setTransaction(transaction);
            try {
                featureStore.addFeatures(fet);
                transaction.commit();
            } catch (Exception problem) {
                problem.printStackTrace();
                transaction.rollback();
            } finally {
                transaction.close();
            }
            return true;
        } catch (IOException e) {
            JGrassLibsPlugin
                    .log(
                            "JGrassLibsPlugin problem: eu.hydrologis.jgrass.libs.utils.features#FeatureUtilities#collectionToShapeFile", e); //$NON-NLS-1$
            e.printStackTrace();
            return false;
        }
    }
    /**
     * @param name the shapefile name
     * @param fieldsSpec to create other fields you can use a string like : <br>
     *        "geom:MultiLineString,FieldName:java.lang.Integer" <br>
     *        field name can not be over 10 characters use a ',' between each field <br>
     *        field types can be : java.lang.Integer, java.lang.Long, // java.lang.Double,
     *        java.lang.String or java.util.Date
     * @return
     */
    @SuppressWarnings("nls")
    public static synchronized ShapefileDataStore createShapeFileDatastore( String name,
            String fieldsSpec, CoordinateReferenceSystem crs ) {
        try {

            // Create the file you want to write to
            File file = null;
            if (name.toLowerCase().endsWith(".shp")) {
                file = new File(name);
            } else {
                file = new File(name + ".shp");
            }
            // Create a Map object used by our DataStore Factory
            // NOTE: file.toURI().toURL() is used because file.toURL() is
            // deprecated
            Map<String, Serializable> map = Collections.singletonMap("url", (Serializable) file
                    .toURI().toURL());

            ShapefileDataStoreFactory factory = new ShapefileDataStoreFactory();
            Map<String, Serializable> create = new HashMap<String, Serializable>();
            create.put("url", file.toURI().toURL());
            ShapefileDataStore myData = (ShapefileDataStore) factory.createNewDataStore(create);

            // Tell this shapefile what type of data it will store
            // Shapefile handle only : Point, MultiPoint, MultiLineString,
            // MultiPolygon
            SimpleFeatureType featureType = DataUtilities.createType(name, fieldsSpec);

            // Create the Shapefile (empty at this point)
            myData.createSchema(featureType);

            // Tell the DataStore what type of Coordinate Reference System (CRS)
            // to use
            myData.forceSchemaCRS(crs);

            return myData;

        } catch (IOException e) {
            JGrassLibsPlugin.log("JGrassLibsPlugin problem", e); //$NON-NLS-1$
            e.printStackTrace();
        } catch (SchemaException se) {
            JGrassLibsPlugin.log("JGrassLibsPlugin problem", se); //$NON-NLS-1$
            se.printStackTrace();
        }
        return null;
    }

    /**
     * Writes a featurecollection to a shapefile
     * 
     * @param data the datastore
     * @param collection the featurecollection
     */
    private static synchronized boolean writeToShapefile( ShapefileDataStore data,
            FeatureCollection<SimpleFeatureType, SimpleFeature> collection ) {
        String featureName = data.getTypeNames()[0]; // there is only one in
        // a shapefile
        FeatureStore<SimpleFeatureType, SimpleFeature> store = null;

        Transaction transaction = null;
        try {

            // Create the DefaultTransaction Object
            transaction = Transaction.AUTO_COMMIT;

            // Tell it the name of the shapefile it should look for in our
            // DataStore
            FeatureSource<SimpleFeatureType, SimpleFeature> source = data
                    .getFeatureSource(featureName);
            store = (FeatureStore<SimpleFeatureType, SimpleFeature>) source;
            store.addFeatures(collection);
            data.getFeatureWriter(transaction);

            // TODO is this needed transaction.commit();
            return true;
        } catch (Exception eek) {
            eek.printStackTrace();
            try {
                transaction.rollback();
            } catch (IOException e) {
                JGrassLibsPlugin.log("JGrassLibsPlugin problem", e); //$NON-NLS-1$
                e.printStackTrace();
                return false;
            }
            return false;
        } finally {
            try {
                transaction.close();
            } catch (IOException e) {
                JGrassLibsPlugin.log("JGrassLibsPlugin problem", e); //$NON-NLS-1$
                e.printStackTrace();
                return false;
            }
        }
    }

    /**
     * Creates a {@link FeatureExtender}.
     * 
     * <p>Useful when cloning features while adding new attributes.</p>
     * 
     * @param oldFeatureType the {@link FeatureType} of the existing features.
     * @param fieldArray the list of the names of new fields. 
     * @param classesArray the list of classes of the new fields.
     * @throws FactoryRegistryException 
     * @throws SchemaException
     */
    public static FeatureExtender createFeatureExteder( SimpleFeatureType oldFeatureType,
            String[] fieldArray, Class[] classesArray ) throws FactoryRegistryException,
            SchemaException {
        FeatureExtender fExt = new FeatureExtender(oldFeatureType, fieldArray, classesArray);
        return fExt;
    }

}
