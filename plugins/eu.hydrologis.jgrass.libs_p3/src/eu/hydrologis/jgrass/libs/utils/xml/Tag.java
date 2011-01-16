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
public class Tag extends Object
{

  protected String type;
  protected Vector attributes;
  protected Vector subTags;
  protected String content;
  protected Tag parentTag;

  public Tag()
  {
      type = "";
      attributes = new Vector();
      subTags = new Vector();
      content = "";
      parentTag = null;
  }

  public Tag( String type )
  {
      this.type = type;
      attributes = new Vector();
      subTags = new Vector();
      content = "";
      parentTag = null;
  }

  public Tag(String type, String content)
  {
    this.type = type;
    attributes = new Vector();
    subTags = new Vector();
    this.content = content;
    parentTag = null;
  }

  //
  // add() - Funktionen
  //

  public Tag addAttribute( Attribute attribute )
  {
    attributes.addElement( attribute );
    return this;
  }

  public Tag addAttribute( String name, String value )
  {
    attributes.addElement( new Attribute( name, value ) );
    return this;
  }

  public Tag addTag( Tag tag )
  {
    tag.setParentTag( this );
    subTags.addElement( tag );
    return this;
  }

  //
  // get() - Funktionen
  //
  public String getType()
  {
    return type;
  }

  public Vector getAttributes()
  {
      return attributes;
  }

  public Attribute getAttribute( String name )
  {
for( int i = 0; i < attributes.size(); i++ )
    {
  if( ( ( Attribute )attributes.elementAt( i ) ).getName().equals( name ) )
      return ( Attribute )attributes.elementAt( i );
    }
return null;
  }

  public Vector getTags()
  {
    return subTags;
  }

  public String getContent()
  {
    return content.replaceAll("&lt;", "<").replaceAll("&gt;", ">");
  }

  public Tag getParentTag()
  {
    return parentTag;
  }

  public Vector getTagsByType( String type )
  {
    Vector tags = new Vector();
    for( int i = 0; i < subTags.size(); i++ )
    {
      Tag tag = ( Tag )subTags.elementAt( i );
      if( tag.getType().equals( type ) )
        tags.addElement( tag );
    }
    return tags;
  }

  public Vector getTagsByContent( String content )
  {
Vector tags = new Vector();
for( int i = 0; i < subTags.size(); i++ )
    {
  Tag tag = ( Tag )subTags.elementAt( i );
  if( tag.getContent().equals( content ) )
      tags.addElement( tag );
    }
return tags;
  }

  public Vector getTagsByAttribute( String attr )
  {
Vector tags = new Vector();
for( int i = 0; i < subTags.size(); i++ )
    {
  Tag tag = ( Tag )subTags.elementAt( i );
  Vector atts = tag.getAttributes();
  for( int k = 0; k < atts.size(); k++ )
      {
    Attribute a = ( Attribute )atts.elementAt( k );
    if( a.getName().equals( attr ) )
        tags.addElement( tag );
      }
    }
return tags;
  }

  public Vector getTagsByAttributeValue( String attr, String value )
  {
Vector tags = new Vector();
for( int i = 0; i < subTags.size(); i++ )
    {
  Tag tag = ( Tag )subTags.elementAt( i );
  Vector atts = tag.getAttributes();
  for( int k = 0; k < atts.size(); k++ )
      {
    Attribute a = ( Attribute )atts.elementAt( k );
    if( a.getName().equals( attr ) && a.getValue().equals( value ) )
        tags.addElement( tag );
      }
    }
return tags;
  }

  //
  // set() - Funktionen
  //

  public void setType( String type )
  {
      this.type = type;
  }

  public void setContent( String content )
  {
      this.content = content;
  }

  public void setParentTag( Tag parentTag )
  {
      this.parentTag = parentTag;
  }

  public void setSubTags( Vector subTags )
  {
      this.subTags = subTags;
  }

  public void setAttributes(Vector attributes)
  {
    this.attributes = attributes;
  }

  //
  // has() - Funktionen
  //

  public boolean hasAttributes()
  {
      return (attributes.size() != 0);
  }

  public boolean hasParentTag()
  {
      return (parentTag != null);
  }

  public boolean hasTags()
  {
      return (subTags.size() != 0);
  }

  //
  // is() - Funktion ;)
  //

  public boolean isEmpty()
  {
    return false;//!hasParentTag() && content.length() == 0 && !hasTags();// ? false;//content.length() == 0;
  }

  /**
   * Returns the path from the root to this node.
   *
   * @returns String path to this node from the root.
   */
  public String getPathToRoot()
  {
    StringBuffer path = new StringBuffer(30);
    path.append(type);
    Tag t = this;
    while (t.hasParentTag())
    {
      t = t.getParentTag();
      path.insert(0, "/").insert(0, t.getType());
    }
    return path.substring(1);
  }

  public String dumpTag()
  {
    String str = type;
    if (this.hasTags())
      str += " [has SUBTAGS]";
    else
      str += " [has NO SUBTAGS]";
    str += " -";
    for( int i = 0; i < attributes.size(); i++ )
    {
      str += " n:" + ( ( Attribute )attributes.elementAt( i ) ).getName();
      str += " v:" + ( ( Attribute )attributes.elementAt( i ) ).getValue();
    }
    return str;
  }

  /**
   * Returns the string representation of the tag. The format is
   * specific for use in a JTree node.
   *
   * @returns String Tag name
   */
  public String toString()
  {
//      return "<"+getType()+">";
    StringBuffer str = new StringBuffer(256);
    str.append("<").append(type);
    if (this.hasAttributes())
    {
      for (int i=0; i<attributes.size(); i++)
      {
        Attribute att = (Attribute)attributes.elementAt(i);
        str.append(" ").append(att.getName()).append("=").append(att.getValue());
      }
   }
    str.append(">");
    return str.toString();
  }

  public String dumpAllTags()
  {
    StringBuffer doc = new StringBuffer(2048);
    XMLProducer prod = new XMLProducer( doc );
    prod.addFormattedTag( this );
    return doc.toString();
  }
}