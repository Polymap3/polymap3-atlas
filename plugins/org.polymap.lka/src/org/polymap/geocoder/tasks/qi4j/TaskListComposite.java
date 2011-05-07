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

package org.polymap.geocoder.tasks.qi4j;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.qi4j.api.common.Optional;
import org.qi4j.api.common.UseDefaults;
import org.qi4j.api.entity.EntityComposite;
import org.qi4j.api.entity.association.ManyAssociation;
import org.qi4j.api.injection.scope.This;
import org.qi4j.api.mixin.Mixins;

import org.polymap.core.model.AssocCollection;
import org.polymap.core.model.Entity;
import org.polymap.core.model.ModelProperty;
import org.polymap.core.qi4j.AssocCollectionImpl;
import org.polymap.core.qi4j.QiEntity;
import org.polymap.geocoder.tasks.ITask;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Braeutigam</a>
 * @version POLYMAP3 ($Revision$)
 * @since 3.0
 */
@Mixins( {
    TaskListComposite.Mixin.class, 
    QiEntity.Mixin.class
} )
public interface TaskListComposite
        extends Entity, EntityComposite {

    public static final String      PROP_TASKS = "tasks";

    @Optional
    @UseDefaults
    ManyAssociation<ITask> tasks();

    
    public AssocCollection<ITask> getTasks();

    @ModelProperty(PROP_TASKS)
    public boolean addTask( ITask task );
    
    @ModelProperty(PROP_TASKS)
    public boolean removeTask( ITask task );
    

    /**
     * Transient fields and methods. 
     */
    public static abstract class Mixin
            implements TaskListComposite {
        
        private static Log log = LogFactory.getLog( Mixin.class );

        @This TaskListComposite          composite;
        

        public AssocCollection<ITask> getTasks() {
            return new AssocCollectionImpl( tasks() );
        }

        public boolean addTask( ITask task ) {
            return tasks().add( task );
        }
        
        public boolean removeTask( ITask task ) {
            return tasks().remove( task );
        }

    }
    
}
