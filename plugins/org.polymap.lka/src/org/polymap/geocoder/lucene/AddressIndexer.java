/* 
 * polymap.org
 * Copyright 2009, Polymap GmbH, and individual contributors as indicated
 * by the @authors tag.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 * $Id$
 */
package org.polymap.geocoder.lucene;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureImpl;
import org.geotools.geojson.geom.GeometryJSON;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import org.apache.commons.collections.MultiHashMap;
import org.apache.commons.collections.MultiMap;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

import org.polymap.core.data.pipeline.PipelineIncubationException;
import org.polymap.core.model.event.IModelHandleable;
import org.polymap.core.model.event.ModelStoreEvent;
import org.polymap.core.model.event.IModelStoreListener;
import org.polymap.core.model.event.ModelStoreEvent.EventType;
import org.polymap.core.project.ILayer;
import org.polymap.core.project.IMap;
import org.polymap.core.project.ProjectRepository;
import org.polymap.core.workbench.PolymapWorkbench;
import org.polymap.geocoder.Address;
import org.polymap.lka.LKAPlugin;
import org.polymap.lka.poi.SearchServlet;
import org.polymap.lka.poi.lucene.PoiIndexer;

/**
 * Lucene based search and indexing engine.
 *
 * @author <a href="http://www.polymap.de">Falko Braeutigam</a>
 * @version POLYMAP3 ($Revision$)
 * @since 3.0
 */
public class AddressIndexer {
    
    private static final Log  log = LogFactory.getLog( AddressIndexer.class );
    
    public static final String[] PROP_STREET = { "street", "strasse", "straße" };

    public static final String[] PROP_NUMBER = { "number", "nummer", "hnr", "nr" };

    public static final String[] PROP_NUMBER_X = { "number_ext", "nummer_ext", "hnr_zusatz", "nr_zusatz" };

    public static final String[] PROP_CITY = { "city", "stadt", "ort" };

    public static final String[] PROP_CITY_X = { "city_ext", "stadt_zusatz", "ort_zusatz" };

    public static final String[] PROP_CITY_DISTRICT = { "district", "urban_district", "ortsteil" };

    public static final String[] PROP_POSTALCODE = { "postalcode", "code", "zip", "zipcode", "plz" };
    
    public static final String FIELD_GEOM = "_geom_";

    public static final String FIELD_SRS = "_srs_";

    public static final String FIELD_KEYWORDS = "_keywords_";

    public static final String FIELD_STREET = "strasse";

    public static final String FIELD_NUMBER = "nummer";

    public static final String FIELD_CITY = "ort";

    public static final String FIELD_POSTALCODE = "plz";

    public static final String FIELD_CATEGORIES = PoiIndexer.FIELD_CATEGORIES;

    
    private AddressProvider   provider;
    
    private File              indexDir;
    
    private Directory         directory;

    private Analyzer          analyzer = new AddressAnalyzer();

    private IndexSearcher     searcher;

    private IndexReader       indexReader;

    private boolean           index2file = false;

    private IModelStoreListener modelListener;


    /**
     * 
     * @param indexDir The directory to store the index into; null specifies
     *        that index is stored in RAM.
     * @throws IOException 
     * @throws PipelineIncubationException 
     */
    public AddressIndexer( AddressProvider provider, File indexDir ) 
    throws Exception {
        this.indexDir = indexDir;
        this.provider = provider;

        if (indexDir != null) {
            log.info( "    creating directory: " + indexDir );
            indexDir.mkdirs();
            directory = FSDirectory.open( indexDir );
            log.info( "    files in directry: " + Arrays.asList( directory.listAll() ) );
            if (directory.listAll().length == 0) {
                reindex();
            }
        }
        else {
            log.info( "    creating RAM directory..." );
            directory = new RAMDirectory();
            reindex();
        }
        
        log.info( "    creating index searcher..." );
        searcher = new IndexSearcher( directory, true ); // read-only=true

        log.info( "    creating index reader for autocomplete..." );
        indexReader = IndexReader.open( directory, false ); // read-only=true

        // listen to model changes
        modelListener = new IModelStoreListener() {
            public void modelChanged( ModelStoreEvent ev ) {
                if (ev.getEventType() == EventType.COMMIT) {
                    try {
                        Set<IMap> maps = new HashSet();
                        for (ILayer layer : AddressIndexer.this.provider.findLayers()) {
                            if (ev.hasChanged( (IModelHandleable)layer )) {
                                reindex();
                                return;
                            }
                            maps.add( layer.getMap() );
                        }
                        for (IMap map : maps) {
                            if (ev.hasChanged( (IModelHandleable)map )) {
                                reindex();
                                return;
                            }
                        }
                    }
                    catch (Exception e) {
                        PolymapWorkbench.handleError( LKAPlugin.PLUGIN_ID, this, "Fehler beim Re-Indizieren der Adressen.", e );
                    }
                }
            }
            public boolean isValid() {
                return true;
            }
        };
        ProjectRepository.instance().addModelStoreListener( modelListener );
    }

    /**
     * Find 'virtual' street addresses. The result records describe one street each.
     * The do not contain numbers. The 'middle' numbers coordinate is used.

     * @param search
     * @param maxResults
     * @return Up to <code>maxResults</code> complete addresses.
     * @throws CorruptIndexException
     * @throws IOException
     * @throws ParseException
     */
    public List<Address> findStreets( Address search, int maxResults )
    throws CorruptIndexException, IOException, ParseException {
        
//        StringBuffer queryString = new StringBuffer( 256 );
//        if (search.getCity() != null) {
//            queryString.append( FIELD_CITY ).append( ':' ).append( decorate( search.getCity() ) );
//        }
//        if (search.getStreet() != null) {
//            queryString.append( queryString.length() > 0 ? " AND " : "" );
//            queryString.append( FIELD_STREET ).append( ':' ).append( decorate( search.getStreet() ) );
//        }
//        if (search.getPostalCode() != null) {
//            queryString.append( queryString.length() > 0 ? " AND " : "" );
//            queryString.append( FIELD_POSTALCODE ).append( ':' ).append( decorate( search.getPostalCode() ) );
//        }
//        log.info( "Lucene search: " + queryString );
//
//        QueryParser parser = new QueryParser( Version.LUCENE_CURRENT, FIELD_STREET, analyzer );
//        parser.setDefaultOperator( QueryParser.AND_OPERATOR );
//        Query query = parser.parse( queryString.toString() );

        Address _search = search.clone();
        _search.setNumber( null );
        
        Query query = new AddressQueryParser( analyzer ).parse( _search );
        log.info( "Lucene search: " + query );

        ScoreDoc[] hits = searcher.search( query, null, 5000 ).scoreDocs;
        
        // city/street -> numbers
        MultiMap numbersMap = new MultiHashMap();
        for (ScoreDoc scoreDoc : hits) {
            Address address = buildAddress( scoreDoc );
            String key = address.getCountry() + address.getCity() + address.getStreet();
            numbersMap.put( key, address );
            if (numbersMap.keySet().size() > maxResults) {
                break;
            }
        }
        
        List<Address> result = new ArrayList();
        for (Object key : numbersMap.keySet()) {
            Collection addresses = (Collection)numbersMap.get( key );
            
            Address a = (Address)addresses.iterator().next();
            Address street = new Address( a.getStreet(), null, a.getPostalCode(), a.getCity(), a.getCountry() );
            street.setPoint( a.getPoint() );
            street.setSRS( a.getSRS() );
            result.add( street );
        }
        return result;
    }


    /**
     * Find 'virtual' city addresses. The result records describe one city each.
     * The records do not contain numbers. The centroid of the convex hull of all
     * coordinates is used for the location.
     * 
     * @param search
     * @param maxResults
     * @return Up to <code>maxResults</code> complete addresses.
     * @throws CorruptIndexException
     * @throws IOException
     * @throws ParseException
     */
    public List<Address> findCities( Address search, int maxResults )
    throws CorruptIndexException, IOException, ParseException {
        
        Address _search = search.clone();
        _search.setStreet( null );
        _search.setNumber( null );
        
        Query query = new AddressQueryParser( analyzer ).parse( _search );
        log.info( "Lucene search: " + query );

//        QueryParser parser = new QueryParser( Version.LUCENE_CURRENT, FIELD_STREET, analyzer );
//        parser.setDefaultOperator( QueryParser.AND_OPERATOR );
//        Query query = parser.parse( queryString.toString() );
        
        ScoreDoc[] hits = searcher.search( query, null, 100 ).scoreDocs;
        
        // city -> all, complete addresses
        MultiMap numbersMap = new MultiHashMap();
        for (ScoreDoc scoreDoc : hits) {
            Address address = buildAddress( scoreDoc );
            String key = address.getCountry() + address.getCity();
            numbersMap.put( key, address );
            if (numbersMap.keySet().size() > maxResults) {
                break;
            }
        }
        
        List<Address> result = new ArrayList();
        for (Object key : numbersMap.keySet()) {
            Collection addresses = (Collection)numbersMap.get( key );
            
            Geometry hull = null;
            String srs = null;
            for (Object obj : addresses) {
                hull = hull == null 
                        ? ((Address)obj).getPoint()
                        : hull.union( ((Address)obj).getPoint() );
                srs = ((Address)obj).getSRS();
            }
            
            Address a = (Address)addresses.iterator().next();
            Address city = new Address( null, null, a.getPostalCode(), a.getCity(), a.getCountry() );
            city.setPoint( hull.getCentroid() );
            city.setSRS( srs );

            result.add( city );
        }
        return result;
    }

    
    /**
     *
     * @param search
     * @param maxResults
     * @return Up to <code>maxResults</code> complete addresses.
     * @throws CorruptIndexException
     * @throws IOException
     * @throws ParseException
     */
    public List<Address> find( Address search, int maxResults )
    throws CorruptIndexException, IOException, ParseException {
        
//        BooleanQuery query = new BooleanQuery();
//        query.add( new FuzzyQuery( new Term( FIELD_CITY, search.getCity() ) ), BooleanClause.Occur.MUST );
//        query.add( new PrefixQuery( new Term( FIELD_STREET, search.getStreet() ) ), BooleanClause.Occur.MUST );
        
        Query query = new AddressQueryParser( analyzer ).parse( search );
        log.info( "    ===> Lucene query: " + query );
        
//        StringBuffer queryString = new StringBuffer( 256 );
//        if (search.getCity() != null) {
//            queryString.append( ' ' ).append( decorate( search.getCity() ) );
//        }
//        if (search.getStreet() != null) {
//            queryString.append( ' ' ).append( decorate( search.getStreet() ) );
//        }
//        if (search.getNumber() != null) {
//            queryString.append( ' ' ).append( decorate( search.getNumber() ) );
//        }
//        if (search.getPostalCode() != null) {
//            queryString.append( ' ' ).append( decorate( search.getPostalCode() ) );
//        }
//        log.info( "Lucene search: " + queryString );
//
//        QueryParser parser = new QueryParser( Version.LUCENE_CURRENT, FIELD_KEYWORDS, analyzer );
//        parser.setDefaultOperator( QueryParser.AND_OPERATOR );
//        Query query = parser.parse( queryString.toString() );
        
        ScoreDoc[] hits = searcher.search( query, null, maxResults ).scoreDocs;
        List<Address> result = new LinkedList();
        for (ScoreDoc scoreDoc : hits) {
            Address address = buildAddress( scoreDoc );
            result.add( address );
            log.info( address );
        }
        return result;
    }
    
    
//    protected String decorate( String propValue ) {
//       return propValue + "~0.5";
//    }
    
    
    public List<Address> find( String addressStr, int maxResults )
    throws CorruptIndexException, IOException, ParseException {

        Query query = null;
        
        // check if search contains special parser chars
        if (StringUtils.containsNone( addressStr, "*?~:" )
                && !StringUtils.contains( addressStr, " OR " )
                && !StringUtils.contains( addressStr, " AND " )) {
            query = new AddressQueryParser( analyzer ).parse( addressStr );
        }
        else {
            WhitespaceAnalyzer a = new WhitespaceAnalyzer( LKAPlugin.LUCENE_VERSION );
            QueryParser parser = new QueryParser( LKAPlugin.LUCENE_VERSION, FIELD_KEYWORDS, a );
            parser.setDefaultOperator( QueryParser.AND_OPERATOR );
            query = parser.parse( addressStr );
        }

        log.info( "    ===> Address: Lucene query: " + query );
        ScoreDoc[] hits = searcher.search( query, null, maxResults ).scoreDocs;
        List<Address> result = new LinkedList();
        for (ScoreDoc scoreDoc : hits) {
            result.add( buildAddress( scoreDoc ) );
        }
        return result;
    }
    
    
    /**
     * Possible search terms. Useful for autocomplete.
     * 
     * @param addressStr
     * @param maxResults
     * @throws IOException
     * @throws ParseException 
     */
    public String[] searchTerms( String field, String addressStr, int maxResults ) 
    throws IOException, ParseException {
        List<String> result = new ArrayList();
        
        TermEnum terms = indexReader.terms( new Term( field, addressStr ) );
        try {
            for (int i=0; i<maxResults; i++) {
                Term term = terms.term();
                if (term == null || !term.text().startsWith( addressStr )) {
                    break;
                }
                result.add( terms.term().text() );
                if (!terms.next()) {
                    break;
                }
            }
            // take first maxResults
            String[] array = new String[result.size()];
            int i = 0;
            for (String s : result) {
                array[i++] = s;
            }
            return array;
        }
        finally {
            terms.close();
        }
    }

    
    public void reindex()
    throws CorruptIndexException, IOException, PipelineIncubationException {
        log.info( "Re-indexing addresses..." );

        log.info( "    creating index writer for directory: " + directory + " ..." );
        
        IndexWriterConfig config = new IndexWriterConfig( LKAPlugin.LUCENE_VERSION, analyzer );
        config.setOpenMode( OpenMode.CREATE );
        IndexWriter iwriter = new IndexWriter( directory, config );

        for (FeatureSource fs : provider.findFeatureSources()) {
            // SRS
            CoordinateReferenceSystem dataCRS = fs.getSchema().getCoordinateReferenceSystem();
            String dataSRS = SearchServlet.toSRS( dataCRS );

            log.info( "    found FeatureSource: " + fs + ", SRS: " + dataSRS );
            FeatureCollection fc = fs.getFeatures();
            Iterator it = fc.iterator();
            
            try {
                int size = 0;
                while (it.hasNext()) {
                    SimpleFeatureImpl feature = (SimpleFeatureImpl)it.next();

                    Document doc = new Document();
                    StringBuilder keywords = new StringBuilder( 1024 );

                    String city = null;
                    String cityExt = null;
                    String cityDistrict = null;

                    String number = null;
                    String numberExt = null;
                    
                    Property prop = findProp( feature, PROP_CITY );
                    if (prop != null && prop.getValue() != null) {
                        city = prop.getValue().toString();
                        keywords.append( prop.getValue().toString() ).append( ' ' );
                    }
                    else {
                        throw new RuntimeException( "Notwendiges Feld für Adresse nicht gefunden: " + Arrays.asList( PROP_CITY ) );
                    }
                    prop = findProp( feature, PROP_CITY_X );
                    if (prop != null && prop.getValue() != null && prop.getValue().toString().length() > 0) {
                        cityExt = prop.getValue().toString();
                        keywords.append( prop.getValue().toString() ).append( ' ' );
                    }
                    prop = findProp( feature, PROP_CITY_DISTRICT );
                    if (prop != null && prop.getValue() != null && prop.getValue().toString().length() > 0) {
                        cityDistrict = prop.getValue().toString();
                        keywords.append( prop.getValue().toString() ).append( ' ' );
                    }
                    prop = findProp( feature, PROP_STREET );
                    if (prop != null && prop.getValue() != null) {
                        String propValue = prop.getValue().toString();
                        doc.add( new Field( FIELD_STREET, propValue, Field.Store.YES, Field.Index.ANALYZED ) );
                        keywords.append( prop.getValue().toString() ).append( ' ' );
                    }
                    else {
                        throw new RuntimeException( "Notwendiges Feld für Adresse nicht gefunden: " + Arrays.asList( PROP_STREET ) );
                    }
                    prop = findProp( feature, PROP_NUMBER );
                    if (prop != null && prop.getValue() != null) {
                        number = prop.getValue().toString();
                    }
                    else {
                        throw new RuntimeException( "Notwendiges Feld für Adresse nicht gefunden: " + Arrays.asList( PROP_NUMBER ) );
                    }
                    prop = findProp( feature, PROP_NUMBER_X );
                    if (prop != null && prop.getValue() != null && prop.getValue().toString().length() > 0) {
                        numberExt = prop.getValue().toString();
                    }
                    prop = findProp( feature, PROP_POSTALCODE );
                    if (prop != null && prop.getValue() != null) {
                        String propValue = prop.getValue().toString();
                        doc.add( new Field( FIELD_POSTALCODE, propValue, Field.Store.YES, Field.Index.ANALYZED ) );
                        keywords.append( prop.getValue().toString() ).append( ' ' );
                    }
                    else {
                        throw new RuntimeException( "Notwendiges Feld für Adresse nicht gefunden: " + Arrays.asList( PROP_POSTALCODE ) );
                    }

                    // cityFull
                    String cityFull = cityExt != null ? (city + "/" + cityExt) : city;
                    cityFull = cityDistrict != null ? (cityFull + " (" + cityDistrict + ")") : cityFull;
                    doc.add( new Field( FIELD_CITY, cityFull, Field.Store.YES, Field.Index.ANALYZED ) );

                    // numberFull
                    String numberFull = numberExt != null ? (number + numberExt) : number;
                    doc.add( new Field( FIELD_NUMBER, numberFull, Field.Store.YES, Field.Index.ANALYZED ) );
                    keywords.append( numberFull ).append( ' ' );
                    
                    // point
                    Point point = (Point)feature.getDefaultGeometry();
                    StringWriter out = new StringWriter( 128 );
                    new GeometryJSON().write( point, out );
                    doc.add( new Field( FIELD_GEOM, out.toString(),
                            Field.Store.YES, Field.Index.NO ) );
                    
                    doc.add( new Field( FIELD_SRS, dataSRS,
                            Field.Store.YES, Field.Index.NO ) );

                    doc.add( new Field( FIELD_KEYWORDS, keywords.toString(), Field.Store.NO, Field.Index.ANALYZED ) );
                    
                    doc.add( new Field( FIELD_CATEGORIES, "address Adresse",
                            Field.Store.YES, Field.Index.ANALYZED ) );

                    iwriter.addDocument( doc );
                    size++;
                }
                log.info( "    document: count=" + size );
            }
            catch (Exception e) {
                log.error( e.getLocalizedMessage(), e );
            }
            finally {
                fc.close( it );
            }
        }
        iwriter.close();
        log.info( "...done." );

        //indexReader.reopen();

        // XXX hack to get index reloaded
        log.info( "    creating index reader..." );
        indexReader = IndexReader.open( directory, false ); // read-only=true
        log.info( "    creating index searcher..." );
        searcher = new IndexSearcher( indexReader ); // read-only=true
        log.info( "Index reloaded." );
    }

    
    protected Property findProp( SimpleFeature feature, String[] propNames ) {
        for (Property prop : feature.getProperties()) {
            String propName = prop.getName().getLocalPart().toLowerCase();
            if (ArrayUtils.contains( propNames, propName )) {
                return prop;
            }
        }
        return null;    
    }
    
    
    protected Address buildAddress( ScoreDoc scoreDoc ) 
    throws CorruptIndexException, IOException {
        Address address = new Address();
        Document doc = searcher.doc( scoreDoc.doc );
        address.setPostalCode( doc.get( FIELD_POSTALCODE ) );
        
        String city = doc.get( FIELD_CITY );
//        String cityExt = doc.get( FIELD_CITY_X );
//        String cityDistrict = doc.get( FIELD_CITY_DISTRICT );
//        String cityFull = cityExt != null ? (city + "/" + cityExt) : city;
//        cityFull = cityDistrict != null ? (cityFull + " (" + cityDistrict + ")") : cityFull;
        address.setCity( city );
        
        address.setStreet( doc.get( FIELD_STREET ) );
        String number = doc.get( FIELD_NUMBER );
//        String numberExt = doc.get( FIELD_NUMBER_X );
        address.setNumber( number );
        
        //log.info( "score: " + scoreDoc.score );
        address.setScore( scoreDoc.score );

        Geometry point = new GeometryJSON().read( new StringReader( doc.get( FIELD_GEOM ) ) );
        address.setPoint( (Point)point );
        address.setSRS( doc.get( FIELD_SRS ) );
        return address;
    }
    
}
