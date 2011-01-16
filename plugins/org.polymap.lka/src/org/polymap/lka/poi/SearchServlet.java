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

import java.io.IOException;
import java.io.ObjectOutput;
import java.net.URLDecoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geotools.referencing.CRS;
import org.json.JSONArray;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

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

    public static final int             DEFAULT_MAX_SEARCH_RESULTS = 300;

    public static CoordinateReferenceSystem DEFAULT_WORLD_CRS;

    private SearchDispatcher            dispatcher;
    
    
    static {
        try {
            DEFAULT_WORLD_CRS = CRS.decode( "EPSG:900913" );
        }
        catch (Exception e) {
            throw new RuntimeException( e );
        }
    }
    

    public SearchServlet()
            throws Exception {
        super();
        log.info( "Initializing SearchServlet ..." );
        dispatcher = new SearchDispatcher();
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

	            for (String record : dispatcher.autocomplete( searchStr, 10 )) {
	                //result.put( StringEscapeUtils.escapeHtml( record ) );
	                result.put( record );
	            }
	            
                log.info( "Response: " + result.toString() );
                response.setContentType( "text/html" );
                response.getWriter().println( result.toString() );
            }
            catch (Exception e) {
                log.info( "Response: " + "Fehler: " + e.getMessage(), e );
                response.setContentType( "text/html" );
                response.getWriter().println( "Fehler: " + e.getMessage() );
            }
	   	}
	   	
	   	// content request (GeoRSS/KML) *******************
	   	else if (request.getParameter( "search" ) != null) {
            String searchStr = request.getParameter( "search" );
            searchStr = URLDecoder.decode( searchStr, "UTF-8" );
            log.info( "    searchStr= " + searchStr );
            String outputType = request.getParameter( "outputType" );
            // FIXME figure the real client URL (without reverse proxies)
            //String baseURL = StringUtils.substringBeforeLast( request.getRequestURL().toString(), "/" ) + "/index.html";
            String baseURL = "http://mittelsachsen-atlas.de/index3.html";
            log.info( "    baseURL: " + baseURL );
            int maxResults = request.getParameter( "maxResults" ) != null
                    ? Integer.parseInt( request.getParameter( "maxResults" ) )
                    : DEFAULT_MAX_SEARCH_RESULTS;
            
            String srsParam = request.getParameter( "srs" );
            CoordinateReferenceSystem worldCRS = DEFAULT_WORLD_CRS;
            if (srsParam != null) {
                try {
                    worldCRS = CRS.decode( srsParam );
                } catch (Exception e) {
                    worldCRS =  DEFAULT_WORLD_CRS;
                }
            }

            try {
                ObjectOutput out = null;
                if ("KML".equalsIgnoreCase( outputType )) {
                    out = new KMLEncoder( response.getOutputStream(), CRS.decode( "EPSG:900913" ) );
                    response.setContentType( "application/vnd.google-earth.kml+xml" );
                }
                else {
                    out = new GeoRssEncoder( response.getOutputStream(), worldCRS, baseURL );
                    response.setContentType( "application/rss+xml" );
                }

                List<SearchResult> results = dispatcher.search( searchStr, maxResults );
                for (SearchResult record : results) {
                    out.writeObject( record );
                }
                out.flush();
            }
            catch (Exception e) {
                log.error( e.getLocalizedMessage(), e );
            }
	   	}
 	}
   
}


