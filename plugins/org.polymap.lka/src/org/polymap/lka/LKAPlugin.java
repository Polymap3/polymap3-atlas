package org.polymap.lka;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;

import org.eclipse.core.runtime.Plugin;

import org.polymap.core.runtime.DefaultSessionContext;
import org.polymap.core.runtime.DefaultSessionContextProvider;
import org.polymap.core.runtime.SessionContext;

import org.polymap.lka.osmtilecache.OsmTileCacheServlet;
import org.polymap.lka.poi.SearchServlet;

/**
 * The activator class controls the plug-in life cycle
 * 
 * 
 */
public class LKAPlugin extends Plugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.polymap.lka";

	// The shared instance
	private static LKAPlugin        plugin;
	
    private static boolean          started;
    
    /** The session context shared by the servlets. */
    private DefaultSessionContext   serviceContext;
    
    public DefaultSessionContextProvider contextProvider;

    
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
	    context.addBundleListener( new BundleListener() {
	        public void bundleChanged( BundleEvent ev ) {

	            if (!started && HttpService.class != null) {
                    // do it first to prevent reentrant calls
	                started = true;

	                //log.info("bundle event" + ev.getType() + " " + ev.getBundle() );
	                // if (ev.getType() == BundleEvent.STARTED && ev.getBundle().equals( getBundle() )) {


	                HttpService httpService;
	                ServiceReference[] httpReferences = null;
	                try {
	                    httpReferences = context.getServiceReferences( HttpService.class.getName(), null );
	                } 
	                catch (InvalidSyntaxException e) {
	                    started = false;
	                    e.printStackTrace();
	                }

                    if (httpReferences != null) {
                        String port = context.getProperty( "org.osgi.service.http.port" );
                        String hostname = context.getProperty( "org.osgi.service.http.hostname" );

                        System.out.println( "Found http service on hostname:" + hostname + "/ port:" + port );

                        httpService = (HttpService)context.getService( httpReferences[0] );
                        try {
                            httpService.registerResources( "/lka", "/lka", null );

                            httpService.registerServlet( "/lka/search", new SearchServlet(), null, null );
                            httpService.registerServlet( "/lka/data", new DataServlet(), null, null );
                            //httpService.registerServlet( "/lka/poi", new PoiTypeProviderServlet(), null, null );

                            httpService.registerServlet( "/lka/osmcache", new OsmTileCacheServlet(), null, null );
                        }
                        catch (Exception e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
	                }
	            }
	            // stop
	            else if (ev.getType() == BundleEvent.STOPPED && ev.getBundle().equals( getBundle() )) {

	            }
	        }
	    });

	    plugin = this;
	}


	public void stop( BundleContext context )
	throws Exception {
		plugin = null;
		super.stop(context);
	}

	
	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static LKAPlugin getDefault() {
		return plugin;
	}

}
