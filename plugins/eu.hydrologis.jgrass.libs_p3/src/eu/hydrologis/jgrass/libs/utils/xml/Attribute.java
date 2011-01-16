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
public class Attribute
{
  private String name;
  private StringBuffer value;

  public Attribute( String name, String value )
  {
    this.name = name;
    this.value = new StringBuffer(value);
  }

  public String getName()
  {
    return name;
  }

  public String getValue()
  {
    int idx;
    if ((idx=value.indexOf("&gt;")) != -1)
      value.replace(idx, idx+4, ">");
    else if ((idx=value.indexOf("&lt;")) != -1)
      value.replace(idx, idx+4, "<");
    /* Replace coded characters */
    return value.toString();
  }

  public void setName( String name )
  {
    this.name = name;
  }

  public void setValue( String value )
  {
    this.value = new StringBuffer(value);
  }
}
