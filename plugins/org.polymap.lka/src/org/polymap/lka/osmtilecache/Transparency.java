/* 
 * polymap.org
 * Copyright 2012, Polymap GmbH. All rights reserved.
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
 */
package org.polymap.lka.osmtilecache;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.imageio.ImageIO;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.polymap.core.data.image.ImageTransparencyProcessor;


/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class Transparency
        extends FilterOutputStream {

    private static Log log = LogFactory.getLog( Transparency.class );


    public Transparency( OutputStream out, Color markerColor ) {
        super( out );
        throw new UnsupportedOperationException( "markerColor is not supported" );
    }

    public void flush()
            throws IOException {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }


    public void write( int b )
            throws IOException {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }


    public static byte[] process( byte[] imageData, final Color markerColor )
    throws IOException {
        long start = System.currentTimeMillis();

//        Image image = Toolkit.getDefaultToolkit().createImage( imageData );
        BufferedImage image = ImageIO.read( new ByteArrayInputStream( imageData ) );

        // filter
        BufferedImage bimage = ImageTransparencyProcessor.transparency( image, markerColor );
        
        // encode PNG
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ImageIO.write( bimage, "png", bout );

//        PngEncoder pngEncoder = new PngEncoder( result, true, null, 9 );
//        pngEncoder.encode( bout );
        bout.flush();
        
        log.debug( "Decode/Transparency/Encode done. (" + (System.currentTimeMillis()-start) + "ms)" );
        return bout.toByteArray();        
    }

}
