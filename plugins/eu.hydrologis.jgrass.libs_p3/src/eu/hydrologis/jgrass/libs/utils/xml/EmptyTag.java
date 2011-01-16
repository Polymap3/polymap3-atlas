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
 
package eu.hydrologis.jgrass.libs.utils.xml;

import java.util.Vector;
/**
 * @deprecated use the java xml api instead. This will be removed as soon as possible.
 */
public class EmptyTag extends Tag
{

    public EmptyTag()
    {
	super();
    }

    public EmptyTag( String type )
    {
	super( type );
    }

    //
    // add() - Funktionen
    //

    public Tag addTag( Tag tag )
    {
      return this;
    }

    //
    // get() - Funktionen
    //

    public Vector getTags()
    {
        return new Vector();
    }

    public String getContent()
    {
        return new String( "" );
    }

    public Vector getTagsByType( String type )
    {
	return null;
    }

    public Vector getTagsByContent( String content )
    {
	return null;
    }

    public Vector getTagsByAttribute( String attr )
    {
	return null;
    }

    public Vector getTagsByAttributeValue( String attr, String value )
    {
	return null;
    }

    //
    // set() - Funktionen
    //

    public void setContent( String content )
    {
        ;
    }

    //
    // has() - Funktionen
    //

    public boolean hasTags()
    {
        return false;
    }

    //
    // is() - Funktion ;)
    //
 
    public boolean isEmpty()
    {
        return true;
    }   

}
