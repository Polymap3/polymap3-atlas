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

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.util.Version;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import org.polymap.core.project.ProjectRepository;
import org.polymap.core.runtime.DefaultSessionContext;
import org.polymap.core.runtime.DefaultSessionContextProvider;
import org.polymap.core.runtime.Polymap;
import org.polymap.core.runtime.SessionContext;
import org.polymap.core.runtime.entity.EntityStateEvent;
import org.polymap.core.security.SecurityUtils;
import org.polymap.core.security.UserPrincipal;

import org.polymap.lka.osmtilecache.OsmTileCacheServlet2;
import org.polymap.lka.poi.SearchServlet;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class LKAPlugin 
        extends Plugin {

    private static final Log log = LogFactory.getLog( LKAPlugin.class );

	// The plug-in ID
	public static final String     PLUGIN_ID = "org.polymap.lka";
	
	/** The Lucene version we are using, current 3.4 from core.libs. */
	public final static Version    LUCENE_VERSION = Version.LUCENE_34;

	// The shared instance
	private static LKAPlugin       plugin;
	
    /** The session context shared by all servlets. */
    private DefaultSessionContext  serviceContext;
    
    public DefaultSessionContextProvider contextProvider;

    private HttpServicesInstaller  servicesInstaller;
    
    
	public LKAPlugin() {
	}


	public void mapServiceContext() {
        contextProvider.mapContext( serviceContext.getSessionKey(), true );

        if (Polymap.instance().getPrincipals().isEmpty()) {
            // allow the indexers to access all maps and layers
            // during startup
            Polymap.instance().addPrincipal( new UserPrincipal( SecurityUtils.ADMIN_USER ) {
                public String getPassword() {
                    throw new RuntimeException( "not yet implemented." );
                }
            });
        }
    }
    
	
    public void unmapServiceContext() {
        contextProvider.unmapContext();
    }


    /**
     * Destroys the current {@link SessionContext} and creates a new one.
     * <p/>
     * XXX This allows to indexers the reload their content from the
     * {@link ProjectRepository} after a {@link EntityStateEvent}. It is a hack since
     * it drops the context without the chance for other indexers to remove their
     * listeners.
     */
    public void dropServiceContext() {
        if (serviceContext != null) {
            contextProvider.destroyContext( serviceContext.getSessionKey() );
        }
        serviceContext = new DefaultSessionContext( "atlas-services" );
        mapServiceContext();
    }
    

	public void start( final BundleContext context ) 
	throws Exception {
        plugin = this;
	    super.start( context );

	    // serviceContext
	    assert serviceContext == null && contextProvider == null;
        serviceContext = new DefaultSessionContext( "atlas-services" );
        
        contextProvider = new DefaultSessionContextProvider() {
            protected DefaultSessionContext newContext( String sessionKey ) {
                return serviceContext;
            }
        };
        SessionContext.addProvider( contextProvider );
        
	    // register servlets
        servicesInstaller = new HttpServicesInstaller( context );
        servicesInstaller.open();
        
        // XXX force routing to start; no other (declarative) way?
        for (Bundle bundle : context.getBundles()) {
            if (bundle.getSymbolicName().equals( "routing-service-osgi" )
                    && bundle.getState() != Bundle.ACTIVE) {
                bundle.start();
            }
        }
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
	        final HttpService httpService = (HttpService)super.addingService( reference );
	        
	        if (httpService != null) {
//                String port = context.getProperty( "org.osgi.service.http.port" );
//                String hostname = context.getProperty( "org.osgi.service.http.hostname" );
//                System.out.println( "Found http service on hostname:" + hostname + "/ port:" + port );

                // delayed starting services in separate thread
                new Job( "ServiceStarter" ) {
                    protected IStatus run( IProgressMonitor monitor ) {
                        log.info( "starting services..." );
                        try {
                            httpService.registerServlet( "/lka/search", new SearchServlet(), null, null );
                            httpService.registerServlet( "/lka/data", new DataServlet(), null, null );
                            httpService.registerServlet( "/lka/osmcache", new OsmTileCacheServlet2(), null, null );
                            httpService.registerResources( "/lka", "/lka", null );
                        }
                        catch (Exception e) {
                            throw new RuntimeException( e );
                        }
                        return Status.OK_STATUS;
                    }
                }.schedule( 5000 );
            }
	        return httpService;
	    }

	}
	
}
