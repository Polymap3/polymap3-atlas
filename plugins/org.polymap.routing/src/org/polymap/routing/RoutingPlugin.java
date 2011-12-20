package org.polymap.routing;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class RoutingPlugin extends AbstractUIPlugin {

	public static final String PLUGIN_ID = "org.polymap.routing";

	private static RoutingPlugin plugin;
	
	
	public RoutingPlugin() {
	}


	public void start(BundleContext context) throws Exception {
		super.start(context);
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
	public static RoutingPlugin getDefault() {
		return plugin;
	}

}
