/* 
 * polymap.org
 * Copyright 2009-2012, Polymap GmbH. All rights reserved.
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
package org.polymap.lka.osmtilecache;

import java.util.Date;

import java.awt.Color;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.util.DateUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.polymap.core.runtime.Polymap;

/**
 * Simple proxy servlet for OSM mapnik (and other) tile servers.
 * 
 * @author <a href="http://www.polymap.de">Falko Braeutigam</a>
 * @since 2.0
 */
public class OsmTileCacheServlet2
    extends HttpServlet {

    private static final Log  log = LogFactory.getLog( OsmTileCacheServlet2.class );

    private HttpClient          httpClient;

    private File                baseDir;
    

    public OsmTileCacheServlet2()
    throws Exception {
        log.info( "Initializing OSM TileCache ..." );

        // HTTPClient
        httpClient = new HttpClient( new MultiThreadedHttpConnectionManager() );
        httpClient.getHttpConnectionManager().getParams().setConnectionTimeout( 8000 ); 
        httpClient.getHttpConnectionManager().getParams().setDefaultMaxConnectionsPerHost( 4 ); 
        httpClient.getParams().setParameter( HttpMethodParams.RETRY_HANDLER, 
                new DefaultHttpMethodRetryHandler( 3, true ));
        
        baseDir = new File( Polymap.getWorkspacePath().toFile(), "osmcache" );
        baseDir.mkdirs();
        log.info( "CACHE: " + baseDir.getAbsolutePath() + 
                "\n        disk: " + FileUtils.byteCountToDisplaySize( FileUtils.sizeOfDirectory( baseDir ) ) +
                "\n        tiles: " + FileUtils.listFiles( baseDir, null, true ).size() );
    }


    protected void doGet( HttpServletRequest request, HttpServletResponse response )
    throws ServletException, IOException {
        String targetPath = request.getPathInfo();
        log.debug( "Request: " + targetPath + " - " + request.getQueryString() );
        
        // params
        String targetBaseURL = request.getParameter( "targetBaseURL" );
        targetBaseURL = targetBaseURL != null ? targetBaseURL : "http://tile.openstreetmap.org";
        
        long expires = request.getParameter( "expires" ) != null
                ? Long.parseLong( request.getParameter( "expires" ) ) * 1000
                : 7*24*3600*1000;  // OSM default: 7 days

        Color markerColor = request.getParameter( "transparent" ) != null
                ? new Color( Integer.parseInt( request.getParameter( "transparent" ), 16 ) )
                : null;

        // response
        response.setHeader( "Server", "POLYMAP3-OSM-Cache" );

        // test HTTP 304
//        response.setHeader( "Etag", "etag" + System.currentTimeMillis() );
        
//        // see http://www.mnot.net/cache_docs/
//        response.setDateHeader( "Expires", System.currentTimeMillis() + 100000000 );
//        response.setHeader( "Cache-Control", "max-age=3600, must-revalidate" );

        long modifiedSince = request.getDateHeader( "If-Modified-Since" );
        log.debug( targetPath + " :: If-modified-Since: " + modifiedSince );

        // check/get cache
        File cacheFile = checkCache( targetBaseURL, targetPath, expires );
        long lastModified = cacheFile.lastModified();

        if (lastModified <= modifiedSince) {
            log.debug( targetPath + " :: 304!" );
            response.setStatus( 304 );
        }
        else {
            response.setDateHeader( "Last-Modified", lastModified );
            response.setHeader( "Cache-Control", "no-cache,must-revalidate" );

            OutputStream out = response.getOutputStream();
            if (markerColor != null) {
                log.debug( targetPath + " :: markerColor: " + markerColor );
                out = new Transparency( out, markerColor );
            }
            BufferedInputStream cached = new BufferedInputStream( new FileInputStream( cacheFile ) );
            try {
                IOUtils.copy( cached, out );
                out.flush();
            }
            finally {
                IOUtils.closeQuietly( cached );
            }
        }
        response.flushBuffer();
    }
    
    
    protected File checkCache( String targetBaseURL, String targetPath, long expires )
    throws IOException {
        File cacheFile = new File( baseDir.getAbsolutePath() + targetPath );
        
        long now = System.currentTimeMillis();
        if (cacheFile.exists()) {
            log.debug( targetPath + " :: cacheFile: expired: " + (cacheFile.lastModified() + expires) + ", now:" + now );
        }

        // exists and not expired
        if (cacheFile.exists() && cacheFile.lastModified()+expires > now) {
            log.debug( targetPath + " :: HIT" );
            return cacheFile;
        }
        
        GetMethod get = new GetMethod( targetBaseURL + targetPath );
        log.debug( targetPath + " :: upstream request: " + get.getURI().toString() );

        // cache miss
        if (!cacheFile.exists()) { 
            log.debug( targetPath + " :: MISS");
        }
        else {
            String lastModified = DateUtil.formatDate( new Date( cacheFile.lastModified() ) );
            log.debug( targetPath + " :: EXPIRED   (lastModified: " + lastModified + ")" );
            get.setRequestHeader( "If-Modified-Since", lastModified );
        }
        
        OutputStream cached = null;
        try {
            int resultCode = httpClient.executeMethod( get );

            // not changed
            if (get.getStatusCode() == 304) {
                log.debug( targetPath + " :: NOT CHANGED upstream!" );
                FileUtils.touch( cacheFile );
            }
            // read data
            else if (get.getStatusCode() == 200) {
                if (!cacheFile.getParentFile().exists()) {
                    cacheFile.getParentFile().mkdirs();
                }
                cached = new BufferedOutputStream( new FileOutputStream( cacheFile ) ); 
                InputStream in = get.getResponseBodyAsStream();

                IOUtils.copy( in, cached );
                cached.flush();
            }
        }
        catch (Exception e) {
            IOUtils.closeQuietly( cached );
            cacheFile.delete();
        }
        finally {
            //get.releaseConnection();
            IOUtils.closeQuietly( cached );
        }
        return cacheFile;
    }
    
}
