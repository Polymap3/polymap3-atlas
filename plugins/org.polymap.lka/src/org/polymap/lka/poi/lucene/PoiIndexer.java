/* 
 * polymap.org
 * Copyright 2009-2012, Polymap GmbH, and individual contributors as
 * indicated by the @authors tag.
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
 */
package org.polymap.lka.poi.lucene;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureImpl;
import org.geotools.geojson.geom.GeometryJSON;
import org.json.JSONException;
import org.opengis.feature.Property;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
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
import org.apache.lucene.queryParser.complexPhrase.ComplexPhraseQueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;

import com.vividsolutions.jts.geom.Geometry;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;

import org.polymap.core.data.PipelineFeatureSource;
import org.polymap.core.data.pipeline.PipelineIncubationException;
import org.polymap.core.project.ILayer;
import org.polymap.core.project.IMap;
import org.polymap.core.runtime.UIJob;
import org.polymap.core.runtime.entity.EntityStateTracker;
import org.polymap.core.runtime.entity.IEntityHandleable;
import org.polymap.core.runtime.entity.EntityStateEvent;
import org.polymap.core.runtime.entity.EntityStateEvent.EventType;
import org.polymap.core.runtime.event.EventHandler;
import org.polymap.core.runtime.event.Event;
import org.polymap.geocoder.Address;
import org.polymap.geocoder.lucene.AddressIndexer;
import org.polymap.lka.LKAPlugin;
import org.polymap.lka.poi.SearchSPI;

/**
 * Lucene based search and index methods.
 *
 * @author <a href="http://www.polymap.de">Falko Braeutigam</a>
 * @since 3.0
 */
class PoiIndexer {
    
    private static final Log  log = LogFactory.getLog( PoiIndexer.class );
    
    public static final String FIELD_TITLE = "name";

    public static final String FIELD_DESCRIPTION = "description";

    public static final String FIELD_GEOM = "_geom_";

    public static final String FIELD_SRS = "_srs_";

    public static final String FIELD_KEYWORDS = "_keywords_";

    public static final String FIELD_ADDRESS = "adresse";

    public static final String FIELD_CATEGORIES = SearchSPI.FIELD_CATEGORIES;

    private PoiProvider       provider;
    
    private File              indexDir;
    
    private Directory         directory;

    private Analyzer          analyzer = new PoiAnalyzer();

    private IndexSearcher     searcher;
    
    private IndexReader       indexReader;

    private boolean           index2file = false;

    
    /**
     * 
     * @param indexDir The directory to store the index into; null specifies
     *        that index is stored in RAM.
     * @throws IOException 
     * @throws PipelineIncubationException 
     */
    public PoiIndexer( PoiProvider provider, File indexDir ) 
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
        
        log.debug( "    creating index reader..." );
        indexReader = IndexReader.open( directory ); // read-only=true
        
        log.debug( "    creating index searcher..." );
        searcher = new IndexSearcher( indexReader ); // read-only=true
        
        // listen to model changes
        EntityStateTracker.instance().addListener( this );
    }

    
    @EventHandler(scope=Event.Scope.JVM)
    protected void modelChanged( EntityStateEvent ev ) {
        if (ev.getEventType() == EventType.COMMIT) {
            // check layers for changes
            Set<IMap> maps = new HashSet();
            for (ILayer layer : PoiIndexer.this.provider.findLayers()) {
                if (ev.hasChanged( (IEntityHandleable)layer )) {
                    EntityStateTracker.instance().removeListener( this );
                    LKAPlugin.getDefault().dropServiceContext();
                    EntityStateTracker.instance().addListener( this );

                    reindex();
                    return;
                }
                maps.add( layer.getMap() );
            }
            // check parent maps for changes
            for (IMap map : maps) {
                if (ev.hasChanged( (IEntityHandleable)map )) {
                    EntityStateTracker.instance().removeListener( this );
                    LKAPlugin.getDefault().dropServiceContext();
                    EntityStateTracker.instance().addListener( this );

                    reindex();
                    return;
                }
            }
        }
    }
    
    
    /**
     * Possible search terms. Useful for autocomplete.
     * 
     * @param searchStr
     * @param maxResults
     * @throws IOException
     */
    public String[] searchTerms( String term, int maxResults ) 
    throws IOException {
        // search for the last term in the search
        String searchStr = term.toLowerCase();
        String prefix = "";
        if (StringUtils.contains( searchStr, " " )) { 
            prefix = StringUtils.substringBeforeLast( term, " " ) + " ";
            searchStr = StringUtils.substringAfterLast( term, " " ).toLowerCase();
        }
        
//        if (searchStr.length() < 3 ) {
//            return new String[0];
//        }

        TermEnum terms = indexReader.terms( new Term( FIELD_KEYWORDS, searchStr ) );
        try {
            // sort descending; accept equal keys
            TreeMap<Integer,String> result = new TreeMap( new Comparator<Integer>() {
                public int compare( Integer o1, Integer o2 ) {
                    return o1.equals( o2 ) ? -1 : -o1.compareTo( o2 );
                }
            });
            // sort
            for (int i=0; i<maxResults*3; i++) {
                String text = terms.term().text();
                int docFreq = terms.docFreq();
                if (!text.startsWith( searchStr )) {
                    break;
                }
                log.info( "   Term: " + text + ", docFreq: " + docFreq );
                result.put( docFreq, StringUtils.capitalize( text ) );
                if (!terms.next()) {
                    break;
                }
            }
            // take first maxResults
            String[] array = new String[result.size()];
            int i = 0;
            for (Iterator it=result.values().iterator(); it.hasNext(); i++) {
                array[i] = prefix + (String)it.next();
            }
            return array;
        }
        //
        catch (Exception e) {
            log.warn( e );
            return new String[0];
        }
        finally {
            terms.close();
        }
    }

    
    public ScoreDoc[] search( String searchStr, int maxResults )
    throws CorruptIndexException, IOException, ParseException {

        QueryParser parser = new ComplexPhraseQueryParser( LKAPlugin.LUCENE_VERSION, FIELD_KEYWORDS, analyzer );
        parser.setAllowLeadingWildcard( true );
        parser.setDefaultOperator( QueryParser.AND_OPERATOR );
        Query query = parser.parse( decorateSearch( searchStr ) );
        log.info( "    ===> POI: Lucene query: " + query );

//        Sort asc = new Sort( new SortField( FIELD_TITLE, SortField.STRING ) );
        ScoreDoc[] hits = searcher.search( query, null, maxResults ).scoreDocs;
        return hits;
        
//        List<Document> result = new LinkedList();
//        for (ScoreDoc scoreDoc : hits) {
//            result.add( searcher.doc( scoreDoc.doc ) );
//        }
//        return result;
    }

    
    protected String decorateSearch( String searchStr ) {
        if (StringUtils.containsNone( searchStr, "*?~\":" )
                && !StringUtils.contains( searchStr, " OR " )
                && !StringUtils.contains( searchStr, " AND " )) {
            return searchStr + "*";
        }
        return searchStr;
    }

    
    public Document getDocument( ScoreDoc scoreDoc )
    throws CorruptIndexException, IOException {
        return searcher.doc( scoreDoc.doc );
    }
    
    
    public void reindex() {
        UIJob job = new UIJob( "Re-indexing POIs" ) {
            protected void runWithException( IProgressMonitor monitor ) throws Exception {
                log.info( "Re-indexing..." );
                IndexWriter iwriter = null;
                try {
                    log.debug( "    creating index writer for directory: " + directory + " ..." );
                    IndexWriterConfig config = new IndexWriterConfig( LKAPlugin.LUCENE_VERSION, analyzer );
                    config.setOpenMode( OpenMode.CREATE );
                    iwriter = new IndexWriter( directory, config );

                    // start jobs
                    List<LayerIndexJob> jobs = new ArrayList();
                    for (ILayer layer : provider.findLayers()) {
                        LayerIndexJob layerJob = new LayerIndexJob( iwriter, layer );
                        jobs.add( layerJob );
                        layerJob.schedule();
                    }

                    // wait for jobs to complete
                    for (LayerIndexJob layerJob : jobs) {
                        try {
                            layerJob.join();
                        }
                        catch (InterruptedException e) {
                        }
                    }
                }
                finally {
                    if (iwriter != null) { iwriter.close(); }
                }
                log.info( "...done." );

                log.info( "    creating index reader..." );
                indexReader.close();
                indexReader = IndexReader.open( directory ); // read-only=true

                //        log.info( "    creating index searcher..." );
                searcher = new IndexSearcher( indexReader ); // read-only=true
                log.info( "Index reloaded." );
            }
        };
        job.setPriority( Job.LONG );
        job.schedule();
    }

    
    /**
     * Performs indexing for one layer.
     */
    class LayerIndexJob
            extends UIJob {

        private IndexWriter     writer;
        
        private ILayer          layer;
        
        
        public LayerIndexJob( IndexWriter writer, ILayer layer ) {
            super( "Indexing: " + layer.getLabel() );
            this.writer = writer;
            this.layer = layer;
            setPriority( LONG );
        }


        @Override
        protected void runWithException( IProgressMonitor monitor )
        throws Exception {
            PipelineFeatureSource fs = PipelineFeatureSource.forLayer( layer, false );
            FeatureCollection fc = null;
            Iterator it = null;
            
            try {
                log.info( layer.getLabel() + ": indexing..." );
                // SRS
                CoordinateReferenceSystem dataCRS = fs.getSchema().getCoordinateReferenceSystem();
                String dataSRS = layer.getCRSCode();
                log.debug( "    " + layer.getLabel() + ": found FeatureSource: " + fs + ", SRS=" + dataSRS );
                
                // start indexing
                fc = fs.getFeatures();
                it = fc.iterator();
                monitor.beginTask( getName(), IProgressMonitor.UNKNOWN );
                reindex( it, dataSRS, monitor );
            }
            catch (Exception e) {
                log.warn( "Fehler beim Indizieren:" + e.getLocalizedMessage() );
                log.debug( e.getLocalizedMessage(), e );
                throw e;
            }
            finally {
                if (fc != null && it != null) {
                    fc.close( it );
                }
                monitor.done();
            }
        }


        protected void reindex( Iterator it, String dataSRS, IProgressMonitor monitor )
        throws IOException, JSONException {
            int size = 0;        
            int indexed = 0;
            String layerKeywords = layer.getKeywords() != null ? StringUtils.join( layer.getKeywords(), " " ) : "";

            GeometryJSON jsonEncoder = new GeometryJSON( 4 );

            while (it.hasNext() && !monitor.isCanceled()) {
                SimpleFeatureImpl feature = (SimpleFeatureImpl)it.next();

                Document doc = new Document();
                StringBuffer keywords = new StringBuffer( 1024 );
                Address address = new Address();
                for (Property prop : feature.getProperties()) {
                    String propName = prop.getName().getLocalPart();
                    //log.debug( "        prop: " + propName );

                    // no value
                    if (prop.getValue() == null) {
                        continue;
                    }
                    // Geometry
                    else if (Geometry.class.isAssignableFrom( prop.getValue().getClass() ) ) {
                        Geometry geom = (Geometry)prop.getValue();
                        StringWriter out = new StringWriter( 1024 );
                        jsonEncoder.write( geom, out );
                        log.debug( "    GEOM: " + geom );
                        log.debug( "    JSON: " + out.toString() );
                        doc.add( new Field( FIELD_GEOM, out.toString(),
                                Field.Store.YES, Field.Index.NO ) );

                        doc.add( new Field( FIELD_SRS, dataSRS,
                                Field.Store.YES, Field.Index.NO ) );
                    }
                    // address fields
                    else if (ArrayUtils.contains( AddressIndexer.PROP_CITY, propName.toLowerCase() )) {
                        address.setCity( prop.getValue().toString() );
                        keywords.append( prop.getValue().toString() ).append( ' ' );
                    }
                    else if (ArrayUtils.contains( AddressIndexer.PROP_STREET, propName.toLowerCase() )) {
                        address.setStreet( prop.getValue().toString() );
                        keywords.append( prop.getValue().toString() ).append( ' ' );
                    }
                    else if (ArrayUtils.contains( AddressIndexer.PROP_POSTALCODE, propName.toLowerCase() )) {
                        address.setPostalCode( prop.getValue().toString() );
                        keywords.append( prop.getValue().toString() ).append( ' ' );
                    }
                    else if (ArrayUtils.contains( AddressIndexer.PROP_NUMBER, propName.toLowerCase() )) {
                        address.setNumber( prop.getValue().toString() );
                        keywords.append( prop.getValue().toString() ).append( ' ' );
                    }
                    // other
                    else {
                        propName = propName.equalsIgnoreCase( FIELD_TITLE ) ? FIELD_TITLE : propName;
                        String propValue = prop.getValue().toString();
                        // ommit empty name field
                        if (propName.equals( FIELD_TITLE ) && propValue.length() == 0) {
                            log.warn( "Empty name field: ..." );
                            continue;
                        }
                        doc.add( new Field( propName, propValue, Field.Store.YES, Field.Index.ANALYZED ) );

                        keywords.append( prop.getValue().toString() ).append( ' ' );
                    }
                    indexed++;
                }

                keywords.append( layerKeywords );
                doc.add( new Field( FIELD_KEYWORDS, keywords.toString(),
                        Field.Store.NO, Field.Index.ANALYZED ) );

                doc.add( new Field( FIELD_ADDRESS, address.toJSON(),
                        Field.Store.YES, Field.Index.ANALYZED ) );

                String categories = layer.getKeywords() != null 
                ? StringUtils.join( layer.getKeywords(), "," ) : "";
                doc.add( new Field( FIELD_CATEGORIES, categories,
                        Field.Store.YES, Field.Index.ANALYZED ) );

                writer.addDocument( doc );
                size++;
                monitor.worked( 1 );
            }
            log.info( "    " + layer.getLabel() + ": count=" + size + " indexed=" + indexed );
        }
    }
    
}
