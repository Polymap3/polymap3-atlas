/*                                                                                           
 * polymap.org                                                                               
 * Copyright 2010, 2012 Polymap GmbH. All rights reserved.                                   
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
 */
package org.polymap.lka;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;

import org.apache.lucene.util.Version;

import org.eclipse.core.runtime.Plugin;

import org.polymap.core.runtime.DefaultSessionContext;
import org.polymap.core.runtime.DefaultSessionContextProvider;
import org.polymap.core.runtime.SessionContext;

import org.polymap.lka.osmtilecache.OsmTileCacheServlet;
import org.polymap.lka.poi.SearchServlet;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class LKAPlugin extends Plugin {

	// The plug-in ID
	public static final String     PLUGIN_ID = "org.polymap.lka";
	
	/** The Lucene version we are using, current 3.4 from core.libs. */
	public final static Version    LUCENE_VERSION = Version.LUCENE_34;

	// The shared instance
	private static LKAPlugin       plugin;
	
    /** The session context shared by the servlets. */
    private DefaultSessionContext  serviceContext;
    
    public DefaultSessionContextProvider contextProvider;

    private HttpServicesInstaller  servicesInstaller;
    
    
	public LKAPlugin() {
	}


	public void mapServiceContext() {
        contextProvider.mapContext( serviceContext.getSessionKey(), true );    
    }

    
    public void unmapServiceContext() {
        contextProvider.unmapContext();
    }
    

	public void start( final BundleContext context ) 
	throws Exception {
        plugin = this;
	    super.start( context );

	    // serviceContext
        serviceContext = new DefaultSessionContext( "lka-services" );
        contextProvider = new DefaultSessionContextProvider() {
            protected DefaultSessionContext newContext( String sessionKey ) {
                return serviceContext;
            }
        };
        SessionContext.addProvider( contextProvider );

	    // register servlets
        servicesInstaller = new HttpServicesInstaller( context );
        servicesInstaller.open();        
	}


	public void stop( BundleContext context )
	throws Exception {
	    servicesInstaller.close();
	    servicesInstaller = null;
	    
		plugin = null;
		super.stop( context );
	}

	
	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static LKAPlugin getDefault() {
		return plugin;
	}

	
	/**
	 * 
	 */
	class HttpServicesInstaller 
	        extends ServiceTracker {

	    public HttpServicesInstaller( BundleContext context ) {
            super( context, HttpService.class.getName(), null );
	    }

	    public Object addingService( ServiceReference reference ) {
	        HttpService httpService = (HttpService)super.addingService( reference );
	        
	        if (httpService != null) {
                String port = context.getProperty( "org.osgi.service.http.port" );
                String hostname = context.getProperty( "org.osgi.service.http.hostname" );
                System.out.println( "Found http service on hostname:" + hostname + "/ port:" + port );

                try {
                    httpService.registerServlet( "/lka/search", new SearchServlet(), null, null );
                    httpService.registerServlet( "/lka/data", new DataServlet(), null, null );
                    httpService.registerServlet( "/lka/osmcache", new OsmTileCacheServlet(), null, null );
                    httpService.registerResources( "/lka", "/lka", null );
                }
                catch (Exception e) {
                    throw new RuntimeException( e );
                }
            }
	        
	        return httpService;
	    }

	}
	
}
