
/*
 * $RCSfile: ImageReadCRIF.java,v $
 *
 * 
 * Copyright (c) 2005 Sun Microsystems, Inc. All  Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met: 
 * 
 * - Redistribution of source code must retain the above copyright 
 *   notice, this  list of conditions and the following disclaimer.
 * 
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in 
 *   the documentation and/or other materials provided with the
 *   distribution.
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of 
 * contributors may be used to endorse or promote products derived 
 * from this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any 
 * kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND 
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY
 * EXCLUDED. SUN MIDROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL 
 * NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF 
 * USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS
 * DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR 
 * ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
 * CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND
 * REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR
 * INABILITY TO USE THIS SOFTWARE, EVEN IF SUN HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGES. 
 * 
 * You acknowledge that this software is not designed or intended for 
 * use in the design, construction, operation or maintenance of any 
 * nuclear facility. 
 *
 * $Revision: 1.2 $
 * $Date: 2005/12/01 00:39:04 $
 * $State: Exp $
 */


package eu.hydrologis.jgrass.libs.jai.imageioimpl;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.EventListener;
import java.util.Iterator;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.event.IIOReadProgressListener;
import javax.imageio.event.IIOReadUpdateListener;
import javax.imageio.event.IIOReadWarningListener;
import javax.imageio.spi.ImageReaderSpi;
import javax.media.jai.CRIFImpl;
import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;

import com.sun.media.jai.imageioimpl.ImageReadCRIF;

import eu.hydrologis.jgrass.libs.iodrivers.imageio.GrassBinaryImageReader;
import eu.hydrologis.jgrass.libs.messages.Messages;
import eu.hydrologis.jgrass.libs.utils.monitor.IProgressMonitorJGrass;


/**
 * 
 * This is a factory for an operation which aim is to read a Grass ascii file. 
 * <p>
 * @see{@link {@link ImageReadCRIF}}
 * </p>
 * @author daniele
 * @since 1.1.0
 */



public class GrassFileReadCRIF extends CRIFImpl {
    public GrassFileReadCRIF() {
        super(); // Pass up the name?

    }

    /**
     * Return a File which contains the path of the Grass database. The return object is a File, so
     * this method verify if the input Object is a File. In this case input is casted to File and is
     * returned. Otherwise if input is a String a File is created from this string
     * 
     * @param input An <code>Object</code> to be used as the source, such as a <code>String</code>
     *        ,or <code>File</code>.
     * @return A <code>File</code> or <code>null</code>.
     */
    private static File getFile( Object input ) {
        // the object which the method return
        File file = null;
        // if the input is a File then cast the input to File object and return it
        try {
            if (input == null) {
                throw new RuntimeException(Messages.getString("FileImageReadCRIF.nullParameters") + " " + input);
            } else if (input instanceof File) {
                file = (File) input;
            } else if (input instanceof String) {
                // create a file from this String

                file = new File((String) input);
            } else if (input instanceof URL) {

                file = new File(((URL) input).getFile());

            }
        } catch (Exception e) {
            throw new RuntimeException(Messages.getString("FileImageReadCRIF.castProblem") + " " + input);
        }

        return file;
    }

    /**
     * Get the <code>ImageReader</code> and set its input and metadata flag. The input set on the
     * reader might not be the same object as the input passed in if the latter was replaced by
     * getImageInputStream().
     */
    static ImageReader getImageReader( ParameterBlock pb ) {
        // Get the input.
        Object input = pb.getObjectParameter(0);

        // Get the reader parameter.
        ImageReader reader = (ImageReader) pb.getObjectParameter(7);

        // Attempt to create an ImageInputStream from the input.
        File file = getFile(input);

        // If no reader passed in, try to find one.
        if (reader == null) {
            // Get all compatible readers.
            Iterator readers = (Iterator) ImageIO.getImageReaders(file != null ? file : input);

            // If any readers, take the first one whose originating
            // service provider indicates that it can decode the input.
            if (readers != null && readers.hasNext()) {
                do {
                    ImageReader tmpReader = (ImageReader) readers.next();
                    ImageReaderSpi readerSpi = tmpReader.getOriginatingProvider();
                    try {
                        if (readerSpi.canDecodeInput(file != null ? file : input)) {
                            reader = tmpReader;
                        }
                    } catch (IOException ioe) {
                        // XXX Ignore it?
                    }
                } while( reader == null && readers.hasNext() );
            }
        }

        // If reader found, set its input and metadata flag.
        if (reader != null) {
            // Get the locale parameter and set on the reader.
            Locale locale = (Locale) pb.getObjectParameter(5);
            if (locale != null) {
                reader.setLocale(locale);
            }

            // Get the listeners parameter and set on the reader.
            EventListener[] listeners = (EventListener[]) pb.getObjectParameter(5);
            if (listeners != null) {
                for( int i = 0; i < listeners.length; i++ ) {
                    EventListener listener = listeners[i];
                    if (listener instanceof IIOReadProgressListener) {
                        reader.addIIOReadProgressListener((IIOReadProgressListener) listener);
                    }
                    if (listener instanceof IIOReadUpdateListener) {
                        reader.addIIOReadUpdateListener((IIOReadUpdateListener) listener);
                    }
                    if (listener instanceof IIOReadWarningListener) {
                        reader.addIIOReadWarningListener((IIOReadWarningListener) listener);
                    }
                }
            }

            // Get the metadata reading flag.
            boolean readMetadata = ((Boolean) pb.getObjectParameter(2)).booleanValue();

            // Set the input and indicate metadata reading state.
            reader.setInput(file != null ? file : input, false, // seekForwardOnly
                    !readMetadata); // ignoreMetadata
        }

        return reader;
    }

    /**
     * Return the RenderedImage through the FileImageReadOpImage.
     * <p>
     * The first step is to find the most suitable ImageReader, then extract the parameter from the
     * ParameterBlock and crerate the Image.
     * <p>
     */
    public RenderedImage create( ParameterBlock pb, RenderingHints rh ) {

        // Value to be returned.
        RenderedImage image = null;

        // Get the reader.
        ImageReader reader = getImageReader(pb);

        // Proceed if a compatible reader was found.
        if (reader != null) {
            // Get the remaining parameters required.
            int imageIndex = pb.getIntParameter(1);
            ImageReadParam param = (ImageReadParam) pb.getObjectParameter(6);
            IProgressMonitorJGrass monitorJGrass = (IProgressMonitorJGrass) pb
                    .getObjectParameter(8);
            Boolean setSampling = (Boolean) pb.getObjectParameter(9);
            Boolean castDtoF = (Boolean) pb.getObjectParameter(10);
            // Initialize the layout.
            ImageLayout layout = (rh != null && rh.containsKey(JAI.KEY_IMAGE_LAYOUT))
                    ? (ImageLayout) rh.get(JAI.KEY_IMAGE_LAYOUT)
                    : new ImageLayout();

            try {
                // Get the parameter input.
                Object paramInput = pb.getObjectParameter(0);

                // Get the reader input.
                Object readerInput = reader.getInput();

                if (reader instanceof GrassBinaryImageReader){
                    ((GrassBinaryImageReader) reader).setMonitor(monitorJGrass);
                }
                // Create the rendering.
                image = new GrassFileReadOpImage(layout, rh, param, reader, imageIndex,
                         monitorJGrass, setSampling, castDtoF);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return image;
    }

}
