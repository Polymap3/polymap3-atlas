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

import java.io.*;

/**
 * @deprecated use the java xml api instead. This will be removed as soon as possible.
 */
public class TagExtractor
{

  // wird fuer rekursive Funktion scan() benoetigt
  public static String readerToString( Reader inReader )
  {
    StringBuffer s = new StringBuffer(1024);
    int ch;

    try
    {
      while( ( ch = inReader.read() ) != -1 )
      {
        s.append( ( char )ch );
      }
    }
    catch( IOException e ) {}

    return s.toString();
  }

  private static void scan( Tag parentTag, String scanString )
  {
    int ix, ix2, ix3, ix4;  // Suchindizes
    String tagString;

    // Gibts hier ueberhaupt noch Tags?
    if( scanString.indexOf( '<' ) == -1 )
    {
      // keine Tags mehr drin
      parentTag.setContent( scanString.trim() );
    }
    else
    {
      // Tags zusammensuchen
      ix4 = 0;
      while( ( ix = scanString.indexOf( '<', ix4 ) ) != -1 )
      {
        if( ix > ix4 + 1 )
        {
          String toAdd = scanString.substring( ix4, ix );
          if( !toAdd.trim().equals( "" ) )
          {
            String cont = parentTag.getContent();
            if( cont.equals( "" ) )
              parentTag.setContent( toAdd.trim() );
            else
              parentTag.setContent( cont + " " + toAdd.trim() );
          }
        }
        ix2 = scanString.indexOf( '>', ix );
        tagString = scanString.substring( ix + 1, ix2 );  // ALLES zwischen < u. >
        Tag tag = TagElementizer.elementize( tagString );

        if( tag == null )
        {
          ix4 = ix2 + 1;
        }
        else
        {
          if( tag.isEmpty() )
			    {
            ix4 = ix2 + 1;
            parentTag.addTag( tag );
          }
          else
          {
            int in0 = ix2 + 1;
            int numClosing = 0;
            int anz;
            int inClosing = in0 - 1;
            do
            {
              // Position des n�chsten </tag> finden.
              inClosing = scanString.indexOf( "</" + tag.getType() + ">", inClosing + 1 );
              numClosing++;
              // Anzahl der <tag> dazwischen ermitteln
              anz = 0;
              int inOpening = scanString.indexOf( "<" + tag.getType(), in0 );
              // es muss eigentlich noch gepr�ft werden, ob es nicht ein leeres Element ist (und kein opening)
              while( ( inOpening < inClosing ) && ( inOpening != -1 ) )
              {
                anz++;
                inOpening = scanString.indexOf( "<" + tag.getType(), inOpening + 1 );
              }
            }
            while( numClosing < anz + 1 );

            ix4 = scanString.indexOf( '>', inClosing );
            ix4++;  // damit gleiche Bedingungen wie bei x4 == 0!
            parentTag.addTag( tag );

            String searchString = scanString.substring( in0, inClosing );
            if( !searchString.equals( "" ) ) scan( tag, searchString );
          }
        }
      }

      // evtl. muss noch content ergaenzt werden
      if( (ix4 < scanString.length() - 1) )
      {
        String moreContent = scanString.substring( ix4, scanString.length() );
        if( !moreContent.trim().equals( "" ) )
        {
          String cont = parentTag.getContent();
          if( cont.equals( "" ) )
            parentTag.setContent( moreContent.trim() );
          else
            parentTag.setContent( cont + " " + moreContent.trim() );
        }
      }
    }
  }

  public static Tag extractTags( Reader reader )
  {
    Tag topLevelTag = new Tag();
    scan( topLevelTag, readerToString( reader ) );
    return topLevelTag;
  }

  public static Tag extractTags( String str )
  {
    Tag topLevelTag = new Tag();
    scan( topLevelTag, str );
    return topLevelTag;
  }
}
