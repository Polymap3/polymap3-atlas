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
package org.polymap.lka.poi.lucene;

import java.util.ArrayList;
import java.util.List;

import java.io.IOException;

import org.apache.commons.collections.ListUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geotools.data.FeatureSource;

import org.polymap.core.data.PipelineFeatureSource;
import org.polymap.core.data.pipeline.PipelineIncubationException;
import org.polymap.core.project.ILayer;
import org.polymap.core.project.IMap;
import org.polymap.core.project.ProjectRepository;

/**
 * Provides the logic to determine which layers and {@link FeatureSource}s
 * are to be accessed by the {@link PoiIndexer}.
 * 
 * @author <a href="http://www.polymap.de">Falko Braeutigam</a>
 * @version POLYMAP3 ($Revision$)
 * @since 3.0
 */
class PoiProvider {

    private static final Log log = LogFactory.getLog( PoiProvider.class );
    

    /**
     * The {@link FeatureSource}s of this provider.
     * <p>
     * This method may block execution while accessing the back-end service.
     * 
     * @throws IOException 
     * @throws PipelineIncubationException 
     * @throws Exception
     */
    public Iterable<FeatureSource> findFeatureSources() 
    throws PipelineIncubationException, IOException {
        List<FeatureSource> result = new ArrayList();
        for (ILayer layer : findLayers()) {
            result.add( PipelineFeatureSource.forLayer( layer, false ) );
        }
        return result;
    }


    /**
     * This is just for testing. Find the "atlas" map in the global domain, find
     * all layers starting with "csv". 
     */
    protected List<ILayer> findLayers() {
        IMap rootMap = ProjectRepository.globalInstance().getRootMap();
        
        for (IMap map : rootMap.getMaps()) {
            if (map.getLabel().equalsIgnoreCase( "atlas" )) {
                log.info( "    Atlas map found: ..." );
                List<ILayer> layers = new ArrayList();
                
                // find all vector layers
                for (ILayer layer : map.getLayers()) {
                    //if (layer.getLayerType() == ILayer.LAYER_VECTOR) {
                        log.info( "    vector layer: " + layer.getLabel() );
                        layers.add( layer );
                    //}
                }
                return layers;
            }
        }
        log.warn( "No \"Atlas\" map found or no \"csv*\" layers." );
        return ListUtils.EMPTY_LIST;
    }


//    /**
//     * Returns the entities ({@link IMap} and {@link ILayer}) that were
//     * used by the last call of {@link #findFeatureSources()} to build the
//     * FeatureSources.
//     */
//    public List<Entity> getEntities() {
//        // XXX Auto-generated method stub
//        throw new RuntimeException( "not yet implemented." );
//    }

}
