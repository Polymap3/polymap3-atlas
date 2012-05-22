/*                                                                                           
 * polymap.org                                                                               
 * Copyright 2010, Polymap GmbH, and individual contributors as indicated                    
 * by the @authors tag.                                                                      
 *                                                                                           
 * This is free software; you can redistribute it and/or modify it                           
 * under the terms of the GNU Lesser General Public License as                               
 * published by the Free Software Foundation; either version 3 of                            
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
 */

package org.polymap.lka.poi;

import java.util.List;
import java.util.zip.GZIPOutputStream;

import java.io.IOException;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.net.URLDecoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.geotools.referencing.CRS;
import org.json.JSONArray;
import org.opengis.metadata.Identifier;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import org.apache.commons.io.output.CountingOutputStream;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.polymap.core.runtime.Polymap;
import org.polymap.core.security.SecurityUtils;
import org.polymap.core.security.UserPrincipal;

import org.polymap.lka.LKAPlugin;

/**
 * This servlet handles search and autocomplete requests. It provides data
 * as GeoRSS or KML.
 *
 * @author <a href="http://www.polymap.de">Falko Braeutigam</a>
 * @version POLYMAP3 ($Revision$)
 * @since 3.0
 */
public class SearchServlet
        extends HttpServlet {

    private static final long serialVersionUID = -8134384328844482965L;

    private static final Log  log = LogFactory.getLog( SearchServlet.class );

    public static final int DEFAULT_MAX_SEARCH_RESULTS = 300;

    public static final CoordinateReferenceSystem DEFAULT_WORLD_CRS;

    public static final CoordinateReferenceSystem WGS84;

    private SearchDispatcher            dispatcher;
    
    
    static {
        try {
            DEFAULT_WORLD_CRS = CRS.decode( "EPSG:900913" );
            WGS84 = CRS.decode( "EPSG:4326" );
        }
        catch (Exception e) {
            throw new RuntimeException( e );
        }
    }
    

    public SearchServlet()
    throws Exception {
        try {
            log.info( "Initializing SearchServlet ..." );
            LKAPlugin.getDefault().mapServiceContext();

            // allow the indexers to access all maps and layers
            // during startup
            Polymap.instance().addPrincipal( new UserPrincipal( SecurityUtils.ADMIN_USER ) {
                public String getPassword() {
                    throw new RuntimeException( "not yet implemented." );
                }
            });

            dispatcher = new SearchDispatcher();
        }
        finally {
            LKAPlugin.getDefault().unmapServiceContext();
        }
    }


    protected void doGet( HttpServletRequest request, HttpServletResponse response )
    throws ServletException, IOException {
        log.info( "Request: " + request.getQueryString() );

	   	// completion request *****************************
	   	if (request.getParameter( "term" ) != null) {
            String searchStr = request.getParameter( "term" );
            searchStr = StringEscapeUtils.unescapeHtml( searchStr );
	        
	        try {
	            JSONArray result = new JSONArray();

	            for (String record : dispatcher.autocomplete( searchStr, 7 )) {
	                //result.put( StringEscapeUtils.escapeHtml( record ) );
	                result.put( record );
	            }
	            
                log.info( "Response: " + result.toString() );
                response.setContentType( "application/json; charset=UTF-8" );
                response.setCharacterEncoding( "UTF-8" );
                response.getWriter().println( result.toString() );
            }
            catch (Exception e) {
                log.info( "Response: " + "Fehler: " + e.getMessage(), e );
                response.setContentType( "text/html" );
                response.getWriter().println( "Fehler: " + e.getMessage() );
            }
	   	}
	   	
	   	// content request (GeoRSS/KML/GeoJSON) ***********
	   	else if (request.getParameter( "search" ) != null) {
            String searchStr = request.getParameter( "search" );
            searchStr = URLDecoder.decode( searchStr, "UTF-8" );
            log.info( "    searchStr= " + searchStr );
            String outputType = request.getParameter( "outputType" );

            int maxResults = request.getParameter( "maxResults" ) != null
                    ? Integer.parseInt( request.getParameter( "maxResults" ) )
                    : DEFAULT_MAX_SEARCH_RESULTS;
            
            String srsParam = request.getParameter( "srs" );
            CoordinateReferenceSystem worldCRS = DEFAULT_WORLD_CRS;
            if (srsParam != null) {
                try {
                    worldCRS = CRS.decode( srsParam );
                } 
                catch (Exception e) {
                    worldCRS =  DEFAULT_WORLD_CRS;
                }
            }
            log.debug( "worldCRS: " + worldCRS );

            // XXX do this encoding via fast, simple, scaleable pipeline and
            // (existing) processors
            try {
                // gzipped response?
                CountingOutputStream cout = new CountingOutputStream( response.getOutputStream() );
                OutputStream bout = cout;
                if (request.getHeader( "Accept-Encoding" ).toLowerCase().contains( "gzip" )) {
                    response.setHeader( "Content-Encoding", "gzip" );
                    bout = new GZIPOutputStream( bout, true );
                }

                ObjectOutput out = null;
                if ("KML".equalsIgnoreCase( outputType )) {
                    out = new KMLEncoder( bout, CRS.decode( "EPSG:900913" ) );
                    response.setContentType( "application/vnd.google-earth.kml+xml" );
                }
                else if ("JSON".equalsIgnoreCase( outputType )) {
                    response.setContentType( "application/json; charset=UTF-8" );
                    response.setCharacterEncoding( "UTF-8" );
                    out = new GeoJsonEncoder( bout, worldCRS );
                }
                else {
                    // FIXME figure the real client URL (without reverse proxies)
                    //String baseURL = StringUtils.substringBeforeLast( request.getRequestURL().toString(), "/" ) + "/index.html";
                    String baseURL = (String)System.getProperties().get( "org.polymap.atlas.feed.url" );
                    log.info( "    baseURL: " + baseURL );                    
                    String title = (String)System.getProperties().get( "org.polymap.atlas.feed.title" );
                    String description = (String)System.getProperties().get( "org.polymap.atlas.feed.description" );
                    
                    out = new GeoRssEncoder( bout, worldCRS, baseURL, title, description );
                    response.setContentType( "application/rss+xml" );
                }

                List<SearchResult> results = dispatcher.search( searchStr, maxResults );
                for (SearchResult record : results) {
                    out.writeObject( record );
                }

                out.flush();
                response.flushBuffer();
                log.info( "    written: " + cout.getCount() + " bytes" );
            }
            catch (Exception e) {
                log.error( e.getLocalizedMessage(), e );
            }
	   	}
	   	response.flushBuffer();
 	}

    
    public static String toSRS( CoordinateReferenceSystem crs ) {
        if (crs.toString().indexOf( "GCS_WGS_1984" ) > -1) {
            return "EPSG:4326";
        }
        // from http://lists.wald.intevation.org/pipermail/schmitzm-commits/2009-July/000228.html
        // If we can determine the EPSG code for this, let's save it as
        // "EPSG:12345" to the file.
        else if (!crs.getIdentifiers().isEmpty()) {
            Object next = crs.getIdentifiers().iterator().next();
            if (next instanceof Identifier) {
                Identifier identifier = (Identifier) next;
                return identifier.toString();
            }
        }
        throw new RuntimeException( "No SRS found for CRS: " + crs );
    }

}


