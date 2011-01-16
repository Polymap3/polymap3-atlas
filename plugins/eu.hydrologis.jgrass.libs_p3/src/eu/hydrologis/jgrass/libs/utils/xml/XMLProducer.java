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
 /*******************************************************************************
            FILE:  XMLProducer.java
   
     DESCRIPTION:  
  
           NOTES:  ---
          AUTHOR:  Philipp Haller, Martin Horsch
           EMAIL:  philh@javacraft.de, horsch@decagon.de
         COMPANY:  
       COPYRIGHT:  (C) 2000  Philipp Haller
         VERSION:  
         CREATED:  January 9, 2001
        REVISION:  March 22, 2001

  ******************************************************************************

    This library is free software; you can redistribute it and/or 
    modify it under the terms of the GNU Library General Public 
    License as published by the Free Software Foundation; either 
    version 2 of the License, or (at your option) any later version. 
 
    This library is distributed in the hope that it will be useful, 
    but WITHOUT ANY WARRANTY; without even the implied warranty of 
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU 
    Library General Public License for more details. 
 
    You should have received a copy of the GNU Library General Public 
    License along with this library; if not, write to the Free 
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307 
    USA 

    Redistribution and use in source and binary forms, with or without
    modification, are permitted provided that the following conditions
    are met:
    1. Redistributions of source code must retain the above copyright
       notice, this list of conditions and the following disclaimer.
    2. Redistributions in binary form must reproduce the above copyright
       notice, this list of conditions and the following disclaimer in the
       documentation and/or other materials provided with the distribution.

  ******************************************************************************

      CHANGE LOG:

         version: 
        comments: changes
          author: 
         created:  
  *****************************************************************************/

package eu.hydrologis.jgrass.libs.utils.xml;

import java.awt.Color;

/**
 * @deprecated use the java xml api instead. This will be removed as soon as possible.
 */
public class XMLProducer
{
  private String LINE_SEP = System.getProperty( "line.separator" );

  private StringBuffer doc;

  /**
   *
   */
  public XMLProducer( StringBuffer buffer )
  {
    doc = buffer;
  }

  /**
   *
   */
  public void addStartTag( Tag tag )
  {
    doc.append( "<" + tag.getType() );

    if( tag.hasAttributes() )
    {
      for( int i = 0; i < tag.getAttributes().size(); i++ )
      {
        Attribute attributeAtI = ( Attribute )tag.getAttributes().elementAt( i );
        doc.append(" " + attributeAtI.getName() + "=\"" +
                   attributeAtI.getValue().replaceAll("<","&lt;").replaceAll(">","&gt;") +
                   "\"" );
      }
    }

    if( tag.isEmpty() && !TagElementizer.PARSEHTML ) doc.append( "/" );

    doc.append( ">" );
  }

  /**
   *
   */
  public void addEndTag( Tag tag )
  {
    if( !tag.isEmpty() )
      doc.append( "</" + tag.getType() + ">" );
  }

  /**
   *
   */
  public void addNewLine()
  {
    doc.append( LINE_SEP );
  }

  /**
   *
   */
  private void formattedOutput( Tag tag, int indent )
  {
    for( int i = 0; i < indent; i++ ) doc.append( " " );  // Einrueckung

    if (tag.getType().length() != 0)  addStartTag( tag );
    if ( tag.hasTags() ) addNewLine();

    if( !tag.getContent().equals( "" ) )  // Inhalt leer?
    {
      if( tag.hasTags() )  // nur bei SubTags einruecken
        for( int i = 0; i < indent; i++ ) doc.append( " " );  // Einrueckung
      doc.append( tag.getContent().replaceAll("<","&lt;").replaceAll(">","&gt;") );  // Inhalt anfuegen
      if( tag.hasTags() ) addNewLine();  // Nur \n wenn SubTags da sind
    }

    // alle untergeordneten Tags durchgehen
    for ( int i = 0; i < tag.getTags().size(); i++ )
      formattedOutput( ( Tag )tag.getTags().elementAt( i ), indent + 2 );

    if( tag.hasTags() )  // nur bei SubTags einruecken
      for( int i = 0; i < indent; i++ ) doc.append( " " );  // Einrueckung

    if (tag.getType().length() != 0) addEndTag( tag );

    addNewLine();
  }

  /**
   *
   */
  public void addFormattedTag( Tag tag )
  {
    formattedOutput( tag, 0 );  // rekursive Methode mit Start-Einrueckung
                                // Null aufrufen
  }

  /**
   *
   */
  public static String getColorString( Color col )
  {
    String r = Integer.toHexString( col.getRed() );
    if( r.length() == 1 ) r = "0" + r;
    String g = Integer.toHexString( col.getGreen() );
    if( g.length() == 1 ) g = "0" + g;
    String b = Integer.toHexString( col.getBlue() );
    if( b.length() == 1 ) b = "0" + b;
    String s = "#" + r + g + b;
    return s;
  }
}
