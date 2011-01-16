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
import java.io.PrintStream;

/**
 * @author Andrea Antonello - www.hydrologis.com
 */
public class NativeUtilities {

    /**
     * Adds a given path to the jna native libraries search path
     * 
     * @param path the path to add (has to be a folder)
     */
    @SuppressWarnings("nls")
    public static void addPathToJna( String path ) {
        if (new File(path).isDirectory()) {
            // Add the lib directory to the jna's library path
            StringBuffer newLibraryPath = new StringBuffer();
            String jnaLibraryPath = System.getProperty("jna.library.path");
            // check if it is already there
            if (jnaLibraryPath != null) {
                String[] pathSplit = jnaLibraryPath.split(":|;");
                for( String p : pathSplit ) {
                    if (p.trim().equals(path.trim())) {
                        return;
                    }
                }
            }
            // if not add it
            if (jnaLibraryPath != null)
                newLibraryPath.append(jnaLibraryPath).append(":");
            // add the root folder for native libs
            newLibraryPath.append(path);
            System.setProperty("jna.library.path", newLibraryPath.toString());
        }
    }

    /**
     * Adds a given path to the jna native libraries platform search path
     * 
     * @param path the path to add (has to be a folder)
     */
    @SuppressWarnings("nls")
    public static void addPathToPlatformJna( String path ) {
        if (new File(path).isDirectory()) {
            // Add the lib directory to the jna's library path
            StringBuffer newLibraryPath = new StringBuffer();
            String jnaPlatformLibraryPath = System.getProperty("jna.platform.library.path");
            // check if it is already there
            if (jnaPlatformLibraryPath != null) {
                String[] pathSplit = jnaPlatformLibraryPath.split(":|;");
                for( String p : pathSplit ) {
                    if (p.trim().equals(path.trim())) {
                        return;
                    }
                }
            }
            // if not add it
            if (jnaPlatformLibraryPath != null)
                newLibraryPath.append(jnaPlatformLibraryPath).append(":");
            // add the root folder for native libs
            newLibraryPath.append(path);
            System.setProperty("jna.platform.library.path", newLibraryPath.toString());
        }
    }

    /**
     * Adds all entries from java.library.path to the jna.platform.library.path
     */
    @SuppressWarnings("nls")
    public static void addJAVAlibraryToJNAplatform() {
        StringBuffer newLibraryPath = new StringBuffer();
        String javaLibraryPath = System.getProperty("java.library.path");
        String jnaPlatformLibraryPath = System.getProperty("jna.platform.library.path");
        if (jnaPlatformLibraryPath != null)
            newLibraryPath.append(jnaPlatformLibraryPath).append(":");
        // add the root folder for native libs
        newLibraryPath.append(javaLibraryPath);
        System.setProperty("jna.platform.library.path", newLibraryPath.toString());
    }

    /**
     * Adds a given path to the jni native libraries search path
     * 
     * @param path the path to add (has to be a folder)
     */
    @SuppressWarnings("nls")
    public static void addPathToJavaLibraryPath( String path ) {
        if (new File(path).isDirectory()) {
            // Add the lib directory to the jna's library path
            StringBuffer newLibraryPath = new StringBuffer();
            String javaLibraryPath = System.getProperty("java.library.path");
            // check if it is already there
            if (javaLibraryPath != null) {
                String[] pathSplit = javaLibraryPath.split(":|;");
                for( String p : pathSplit ) {
                    if (p.trim().equals(path.trim())) {
                        return;
                    }
                }
            }
            // if not add it
            if (javaLibraryPath != null)
                newLibraryPath.append(javaLibraryPath).append(":");
            // add the root folder for native libs
            newLibraryPath.append(path);
            System.setProperty("java.library.path", newLibraryPath.toString());
        }
    }

    @SuppressWarnings("nls")
    public static void printPaths( PrintStream out ) {
        out.println("***********  JAVA LIBRARY PATH ************");
        String javaLibraryPath = System.getProperty("java.library.path");
        String[] split = javaLibraryPath.split(":|;"); //$NON-NLS-1$
        for( String s : split ) {
            out.println(s);
        }
        out.println("***********  JNA LIBRARY PATH ************");
        String jnaLibraryPath = System.getProperty("jna.library.path");
        split = jnaLibraryPath.split(":|;"); //$NON-NLS-1$
        for( String s : split ) {
            out.println(s);
        }
        out.println("***********  JNA PLATFORM LIBRARY PATH ************");
        String jnaPlatformLibraryPath = System.getProperty("jna.platform.library.path");
        split = jnaPlatformLibraryPath.split(":|;"); //$NON-NLS-1$
        for( String s : split ) {
            out.println(s);
        }
    }

}
