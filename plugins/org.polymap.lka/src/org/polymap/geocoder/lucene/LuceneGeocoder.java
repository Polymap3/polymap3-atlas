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
package org.polymap.geocoder.lucene;

import java.util.List;

import java.io.File;

import org.eclipse.core.runtime.IPath;

import org.polymap.core.runtime.Polymap;
import org.polymap.geocoder.Address;
import org.polymap.geocoder.Geocoder;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Braeutigam</a>
 * @version POLYMAP3 ($Revision$)
 * @since 3.0
 */
public class LuceneGeocoder
        extends Geocoder {
    
    private static LuceneGeocoder   instance;
    
    public static synchronized LuceneGeocoder instance() {
        if (instance == null) {
            instance = new LuceneGeocoder();
        }
        return instance;
    }

    
    // instance *******************************************
    
    protected AddressProvider       provider;
    
    protected AddressIndexer        indexer;
    
    
    public LuceneGeocoder() { 
        try {
            provider = new AddressProvider();
            IPath workspace = Polymap.getWorkspacePath();
            indexer = new AddressIndexer( provider, new File( workspace.toFile(), "addresses" ) );
        }
        catch (Exception e) {
            throw new RuntimeException( e.getLocalizedMessage(), e );
        }
    }


    public List<Address> find( Address fragment, int maxResults )
            throws Exception {
        return indexer.find( fragment, maxResults );
    }


    /**
     * Find 'virtual' street addresses. The result records describe one street each.
     * The do not contain numbers. The 'middle' numbers coordinate is used.

     * @param search
     * @param maxResults
     * @return Up to <code>maxResults</code> complete addresses.
     * @throws Exception
     */
    public List<Address> findStreets( Address fragment, int maxResults )
            throws Exception {
        return indexer.findStreets( fragment, maxResults );
    }

    
    /**
     * Find 'virtual' city addresses. The result records describe one city each.
     * The do not contain numbers. The 'middle' numbers coordinate is used.
     * 
     * @param search
     * @param maxResults
     * @return Up to <code>maxResults</code> complete addresses.
     * @throws CorruptIndexException
     * @throws IOException
     * @throws ParseException
     */
    public List<Address> findCities( Address fragment, int maxResults )
    throws Exception {
        return indexer.findCities( fragment, maxResults );
    }

    
    public Iterable<Address> find( String addressString, int maxResults )
            throws Exception {
        return indexer.find( addressString, maxResults );
    }

}
