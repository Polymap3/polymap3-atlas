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
package org.polymap.lka.osmtilecache;

import java.util.Date;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.DiskStoreConfiguration;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;

import org.polymap.core.data.image.ImageTransparencyProcessor;
import org.polymap.core.runtime.Polymap;

/**
 * Simple proxy servlet for OSM mapnik (and other) tile servers.
 * <p/>
 * The cache is based on EHCache currently. We need a general service for
 * caching.
 * 
 * @author <a href="http://www.polymap.de">Falko Braeutigam</a>
 * @version POLYMAP3 ($Revision$)
 * @since 3.0
 */
public class OsmTileCacheServlet
    extends HttpServlet {

    private static final Log  log = LogFactory.getLog( OsmTileCacheServlet.class );

    private HttpClient          httpClient;

    private CacheManager        ehcm;
    
    private Cache               cache;
    
    private int                 cachePutCount = 0;
    

    public OsmTileCacheServlet()
    throws Exception {
        super();
        log.info( "Initializing OSM TileCache ..." );

        // HTTPClient
        httpClient = new HttpClient( new MultiThreadedHttpConnectionManager() );
        httpClient.getHttpConnectionManager().getParams().setConnectionTimeout( 8000 ); 
        httpClient.getHttpConnectionManager().getParams().setDefaultMaxConnectionsPerHost( 4 ); 
        httpClient.getParams().setParameter( HttpMethodParams.RETRY_HANDLER, 
                new DefaultHttpMethodRetryHandler( 3, true ));
        
        // EHCache
        Configuration config = new Configuration();
        
        DiskStoreConfiguration diskStore = new DiskStoreConfiguration();
        File f = new File( Polymap.getWorkspacePath().toFile(), "osmcache" );
        log.info( "    EHCache diskStorePath: " + f.getAbsolutePath() );
        diskStore.setPath( f.getAbsolutePath() );
        config.addDiskStore( diskStore );

        CacheConfiguration defaultCache = new CacheConfiguration();
        config.addDefaultCache( defaultCache );
        
        ehcm = new CacheManager( config );
        
        // Cache
        cache = ehcm.getCache( "osmtilecache" );
        if (cache == null) {
            cache = new Cache( "osmtilecache", 
                    500,
                    MemoryStoreEvictionPolicy.LRU, 
                    true,                   // overflow to disk 
                    f.getAbsolutePath(), 
                    false,                  // eternal
                    3600*24*7,             // timeToLive 
                    3600*24*7, 
                    true,                   // diskPersistent
                    30,                    // disk expire thread interval 
                    null,
                    null,                   // bootstrap cache loader
                    20000,                  // maxElementsOnDisk 
                    1);                     // disk spool buf MB
            cache.setDiskStorePath( f.getAbsolutePath() );
            ehcm.addCache( cache );
            cache.flush();
        }
        log.info( "    CACHE: " + cache.getName() + 
                    " - memory: " + cache.getMemoryStoreSize() +
                    ", disk: " + cache.getDiskStoreSize() );
        
        log.info( "    CACHE: evicting expired elements..." );
        cache.evictExpiredElements();

        log.info( "    CACHE: " + cache.getName() + 
                " - memory: " + cache.getMemoryStoreSize() +
                ", disk: " + cache.getDiskStoreSize() );
    }


    public void destroy() {
        super.destroy();
        if (cache != null) {
            log.info( "destroy: flushing cache..." );
            cache.flush();
            cache.dispose();
            cache = null;
        }
    }


    protected void doGet( HttpServletRequest request, HttpServletResponse response )
    throws ServletException, IOException {
        log.debug( "Request: " + request.getPathInfo() + " - " + request.getQueryString() );
        
        // params
        String targetBaseURL = request.getParameter( "targetBaseURL" );
        targetBaseURL = targetBaseURL != null ? targetBaseURL : "http://tile.openstreetmap.org";
        
        long expires = request.getParameter( "expires" ) != null
                ? Long.parseLong( request.getParameter( "expires" ) )
                : 1*24*60*60*1000;  // default: 1 day

        Color markerColor = request.getParameter( "transparent" ) != null
                ? new Color( Integer.parseInt( request.getParameter( "transparent" ), 16 ) )
                : null;

        String targetPath = request.getPathInfo();

        // response
        response.setDateHeader( "Expires", new Date().getTime() + expires );
        response.setHeader( "Server", "POLYMAP3 OSM Cache" );

        // check cache
        String cacheKey = targetBaseURL + "_" + targetPath;
        Element cacheElm = cache.get( cacheKey );
        if (cacheElm != null) {
            log.debug( "    cache - HIT: " + cacheKey );
            byte[] cached = (byte[])cacheElm.getObjectValue();
            if (markerColor != null) {
                log.debug( "    markerColor: " + markerColor );
                cached = transparency( cached, markerColor );
            }
            response.getOutputStream().write( cached );
            response.getOutputStream().flush();
        }
        // go upstream
        else {
            log.debug( "    cache - MISS: " + cacheKey );
        
            GetMethod get = new GetMethod( targetBaseURL + targetPath );
            log.debug( "    upstream request: " + get.getURI().toString() );
            ServletOutputStream out = null;
            try {
                int resultCode = httpClient.executeMethod( get );
                log.debug( "    result code: " + resultCode );

                ByteArrayOutputStream cached = new ByteArrayOutputStream();
                out = response.getOutputStream();
                InputStream in = get.getResponseBodyAsStream();
                
                byte[] buf = new byte[4096];
                for (int c=in.read( buf ); c>0; c=in.read( buf )) {
                    //out.write( buf, 0, c );
                    cached.write( buf, 0, c );
                }
                cache.put( new Element( cacheKey, cached.toByteArray() ) );
                if (++cachePutCount % 100 == 0) {
                    log.info( "Cache put count: " + cachePutCount + " - flushing cache..." );
                    cache.flush();
                }
                log.debug( "### Cache - memory: " + cache.getMemoryStoreSize()
                        + ", disk: " + cache.getDiskStoreSize() );
                
                byte[] result = markerColor != null
                        ? transparency( cached.toByteArray(), markerColor )
                        : cached.toByteArray();
                out.write( result );
            }
            finally {
                if (out != null) {
                    out.flush();
                }
                get.releaseConnection();
            }
        }
    }

    
    protected byte[] transparency( byte[] imageData, final Color markerColor )
    throws IOException {
        long start = System.currentTimeMillis();

//        Image image = Toolkit.getDefaultToolkit().createImage( imageData );
        BufferedImage image = ImageIO.read( new ByteArrayInputStream( imageData ) );

        // filter
        BufferedImage bimage = ImageTransparencyProcessor.transparency( image, markerColor );
        
        // encode PNG
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ImageIO.write( (RenderedImage)bimage, "png", bout );

//        PngEncoder pngEncoder = new PngEncoder( result, true, null, 9 );
//        pngEncoder.encode( bout );
        bout.flush();
        
        log.debug( "Decode/Transparency/Encode done. (" + (System.currentTimeMillis()-start) + "ms)" );
        return bout.toByteArray();
        
    }
    
}
