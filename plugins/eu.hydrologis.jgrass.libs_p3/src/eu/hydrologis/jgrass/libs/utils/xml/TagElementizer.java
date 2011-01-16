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

/**
 * @deprecated use the java xml api instead. This will be removed as soon as possible.
 */
public class TagElementizer
{

  public static boolean PARSEHTML = false;

  public static Tag elementize( String tagString )
  {
    if (PARSEHTML)
      return elementizeHTML( tagString );
    else
      return elementizeXML( tagString );
  }

  public static Tag elementizeHTML( String tagString )
  {
    String tagType, attName, attValue;

    Tag tag;

    String[] htmltags = { "base", "br", "button", "hr", "img", "input", "meta", "object", "param", "select", "var" };
  	boolean realEmpty;  // leer? oder blo� br, hr, meta, ... (HTML!)

  	// Haben wir eine Processing Instruction ( <? ... ?> ) erwischt?
    if (tagString.startsWith( "?" ))
      return null;
    else
    {
      // ist das vielleicht ein leerer <tag/> ?
      if (tagString.indexOf( '/' ) != -1)
      {
        realEmpty = true;
      }
      else
      {
        realEmpty = false;
      }
    }

    // Hat Tag Attribute?
    if (tagString.indexOf( ' ' ) == -1)
    {
      // hat keine Attribute
      if (realEmpty)
      {
        tagType = tagString.substring( 0, tagString.length() - 1 );
        return new EmptyTag( tagType );
      }
      else
      {
        boolean isEmptyHTML = false;
        for (int i = 0; i < htmltags.length; i++)
          if (tagString.equals(htmltags[i])) isEmptyHTML = true;
        if (isEmptyHTML)
          return new EmptyTag(tagString);
        else
          return new Tag(tagString);
      }
    }
    else
    {
      int ix, ix2, ix3, ix4;

      // Tag-Typ extrahieren
      ix = tagString.indexOf(' ');
      tagType = tagString.substring( 0, ix );

      boolean isEmptyHTML = false;
      for (int i = 0; i < htmltags.length; i++)
        if (tagType.equals(htmltags[i])) isEmptyHTML = true;
      if (isEmptyHTML)
        tag = new EmptyTag();
      else
        tag = new Tag();

      tag.setType( tagType );

      // Attribute zusammensuchen
      ix4 = 0;
      while ((ix = tagString.indexOf(' ', ix4)) != -1)
      {
        ix2 = tagString.indexOf( '=', ix );
        if (ix2 == -1)
        {
          // Tag mit abschlie�endem Blank nach dem letzten Attribut
          break;
        }

        // jetzt Attribut-Name bekannt (z.B. img)
        attName = tagString.substring( ix + 1, ix2 ).trim();  // trim() entfernt evtl. Blanks
        ix3 = tagString.indexOf( '\"', ix2 );
        ix4 = tagString.indexOf( '\"', ix3 + 1 );
        // jetzt Attribut-Wert bekannt (z.B. http://java.sun.com/ )
        attValue = tagString.substring( ix3 + 1, ix4 );  // hier kein trim() noetig!
        tag.addAttribute( new Attribute( attName, attValue ) );
      }
    }

    return tag;
  }

  public static Tag elementizeXML( String tagString )
  {
    String tagType, attName, attValue;
    Tag tag;

    // Haben wir eine Processing Instruction ( <? ... ?> ) erwischt?
    if (tagString.startsWith( "?" ))
      return null;
    else
    {
      // ist das vielleicht ein leerer <tag/> ?
      if (tagString.endsWith("/"))
      {
        tag = new EmptyTag();
      }
      else
      {
        tag = new Tag();
      }
    }

    // Hat Tag Attribute?
    if (tagString.indexOf(' ') == -1)
    {
      // hat keine Attribute
      if (tag.isEmpty())
        tag.setType(tagString.substring(0, tagString.length() - 1));  // kleiner Bug: was, wenn mit Blank vor "/"?
      else
        tag.setType( tagString );
      return tag;
    }
    else
    {
      int ix, ix2, ix3, ix4;
      // Tag-Typ extrahieren
      ix = tagString.indexOf(' ');
      tagType = tagString.substring(0, ix);
      tag.setType( tagType.trim() );

      // Attribute zusammensuchen
      ix4 = 0;
      while ((ix = tagString.indexOf(' ', ix4)) != -1)
      {
        ix2 = tagString.indexOf( '=', ix );
        if (ix2 == -1)
        {
          // Tag mit abschlie�endem Blank nach dem letzten Attribut
          break;
        }
        // jetzt Attribut-Name bekannt (z.B. img)
        attName = tagString.substring(ix + 1, ix2).trim();  // trim() entfernt evtl. Blanks
        ix3 = tagString.indexOf( '\"', ix2 );
        ix4 = tagString.indexOf( '\"', ix3 + 1 );
        // jetzt Attribut-Wert bekannt (z.B. http://java.sun.com/ )
        attValue = tagString.substring(ix3 + 1, ix4);  // hier kein trim() noetig!
        tag.addAttribute(new Attribute(attName, attValue));
      }
    }
    return tag;
  }

}
