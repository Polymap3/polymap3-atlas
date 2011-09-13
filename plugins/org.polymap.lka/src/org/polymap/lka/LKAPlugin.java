package org.polymap.lka;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;

import org.eclipse.core.runtime.Plugin;

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
	private static LKAPlugin       plugin;
	
    private static boolean         started;
    
    
	/**
	 * The constructor
	 */
	public LKAPlugin() {
	}


	public void start(final BundleContext context) throws Exception {
	    super.start(context);

	    // start HttpServiceRegistry
	    context.addBundleListener( new BundleListener() {
	        public void bundleChanged( BundleEvent ev ) {

	            if (!started && HttpService.class != null) {

	                //log.info("bundle event" + ev.getType() + " " + ev.getBundle() );
	                // if (ev.getType() == BundleEvent.STARTED && ev.getBundle().equals( getBundle() )) {


	                HttpService httpService;
	                // BundleContext context=
	                // CorePlugin.getDefault().getBundle().getBundleContext();
	                ServiceReference[] httpReferences = null;
	                try {
	                    httpReferences = context.getServiceReferences( HttpService.class.getName(), null );
	                } 
	                catch (InvalidSyntaxException e) {
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
                            started = true;
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


	public void stop(BundleContext context) throws Exception {
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
