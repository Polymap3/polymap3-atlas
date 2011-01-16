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
 package eu.hydrologis.jgrass.libs.utils.dialogs;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Shell;

public class WindowUtilities {

    public static Point placeDialogInCenterOfParent( Shell parent, Shell shell ) {
        Rectangle parentSize = parent.getBounds();
        Rectangle mySize = shell.getBounds();

        int locationX, locationY;
        locationX = (parentSize.width - mySize.width) / 2 + parentSize.x;
        locationY = (parentSize.height - mySize.height) / 2 + parentSize.y;

        shell.setLocation(new Point(locationX, locationY));
        return new Point(locationX, locationY);
    }

    public static Point placeDialogInCenterOfParent( Rectangle parentBounds, Shell shell ) {
        Rectangle mySize = shell.getBounds();

        int locationX, locationY;
        locationX = (parentBounds.width - mySize.width) / 2 + parentBounds.x;
        locationY = (parentBounds.height - mySize.height) / 2 + parentBounds.y;

        shell.setLocation(new Point(locationX, locationY));
        return new Point(locationX, locationY);
    }

}
