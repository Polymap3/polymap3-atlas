/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package eu.hydrologis.jgrass.libs.utils;

import java.io.File;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;

import eu.hydrologis.jgrass.libs.JGrassLibsPlugin;

/**
 * @author Andrea Antonello - www.hydrologis.com
 */
public class CommandUtilities {

    /**
     * Execute native commands of which the executable il located inside a plugin. Executes the
     * command and waits for it to come back.
     * 
     * @param os the operating system identifier (ex. Platform.OS_LINUX)
     * @param pluginid the plugin id (ex. JGrassLibsPlugin.PLUGIN_ID)
     * @param command the total command. Note that the first element of the array has to be the path
     *        of the exe inside the plugin, using url-like file separators, i.e. /
     * @return true if it worked well
     */
    public boolean executeNativeInPlugin( String os, String pluginid, String[] command ) {

        try {
            String exePath = FileUtilities.pluginPathToAbsolutepath(pluginid, command[0]);

            // if (os.equals(Platform.OS_WIN32)) {
            // } else if (os.equals(Platform.OS_LINUX)) {
            // } else if (os.equals(Platform.OS_MACOSX)) {
            // } else {
            // return false;
            // }

            // substitute the exe with the full path to it
            command[0] = exePath;

            StringBuilder sB = new StringBuilder();
            for( String string : command ) {
                sB.append(string).append(" "); //$NON-NLS-1$
            }

            if (new File(exePath).exists()) {
                System.out.println("Executing native command: " + sB.toString());
            } else {
                System.out.println("Could not find native exe: " + exePath);
                return false;
            }

            ProcessBuilder pB = new ProcessBuilder(command);
            Process process = null;
            process = pB.start();
            process.waitFor();

            System.out.println("Done");
            return true;
        } catch (Exception e) {
            JGrassLibsPlugin
                    .log(
                            "JGrassLibsPlugin problem: eu.hydrologis.jgrass.libs.utils#CommandUtilities#executeNativeInPlugin", e); //$NON-NLS-1$
            e.printStackTrace();
            return false;
        }
    }
}
