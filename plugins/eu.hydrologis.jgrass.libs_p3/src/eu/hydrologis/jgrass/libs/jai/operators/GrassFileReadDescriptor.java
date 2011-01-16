
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
package eu.hydrologis.jgrass.libs.jai.operators;

import java.awt.RenderingHints;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;
import java.io.File;
import java.util.EventListener;
import java.util.Locale;

import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.media.jai.JAI;
import javax.media.jai.OperationDescriptorImpl;
import javax.media.jai.OperationRegistry;
import javax.media.jai.RenderedOp;
import javax.media.jai.TileCache;
import javax.media.jai.registry.RIFRegistry;
import javax.media.jai.registry.RenderedRegistryMode;

import eu.hydrologis.jgrass.libs.iodrivers.imageio.GrassBinaryImageReader;
import eu.hydrologis.jgrass.libs.iodrivers.imageio.spi.GrassBinaryImageReaderSpi;
import eu.hydrologis.jgrass.libs.jai.imageioimpl.GrassFileReadCRIF;
import eu.hydrologis.jgrass.libs.messages.Messages;
import eu.hydrologis.jgrass.libs.utils.monitor.IProgressMonitorJGrass;

/**
 * This class is a Descriptor for an JAI operation which read a GRASS ASCII file.
 * <p>
 * The operation is similar to ''ImageRead'' operation but this is adapted to a special case of
 * GRASS file. In dept the parameters is quite different from the original. This operation use the
 * plug-in in JGrass to read GRASS ASCII {@link GrassBinaryImageReader},
 * {@link GrassBinaryImageReaderSpi} .
 * </p>
 * 
 * @author daniele andreis
 * @since 1.1.0
 */

public class GrassFileReadDescriptor extends OperationDescriptorImpl {
    /**
     * The name of the operation.
     */
    private static final String OPERATION_NAME = "GrassFileRead";

    /**
     * The resource strings that provide the general documentation and specify the parameter list
     * for the "FileImageRead" operation.
     */
    private static final String[][] resources = {{"GlobalName", OPERATION_NAME},
            {"LocalName", OPERATION_NAME}, {"Vendor", "com.andreis.daniele"},
            {"Description", "FileImageReadDescriptor"}, {"DocURL", "http://"}, {"Version", "beta"}

    };

    /** The parameter names for the "FileImageRead" operation. */
    private static final String[] paramNames = {"Input", "ImageChoice", "ReadMetadata",
            "VerifyInput", "Listeners", "Locale", "ReadParam", "Reader", "JGrassMonitor",
            "UseSubsampling", "CastDToF"};

    private static final String[] supportedModes = {"rendered"};

    /** The parameter class types for rendered mode of "ImageRead". */
    private static final Class[] renderedParamClasses = {
            java.lang.Object.class, // Input
            java.lang.Integer.class, // ImageChoice
            java.lang.Boolean.class, // ReadMetadata
            java.lang.Boolean.class, // VerifyInput
            java.util.EventListener[].class, // Listeners
            java.util.Locale.class, // Locale
            javax.imageio.ImageReadParam.class, // ReadParam
            javax.imageio.ImageReader.class, // Reader
            eu.hydrologis.jgrass.libs.utils.monitor.IProgressMonitorJGrass.class,
            java.lang.Boolean.class, java.lang.Boolean.class};

    /** The parameter default values for rendered mode of "FileImageRead". */
    private static final Object[] renderedParamDefaults = {NO_PARAMETER_DEFAULT, // Input
            new Integer(0), // ImageChoice
            Boolean.TRUE, // ReadMetadata
            Boolean.TRUE, // VerifyInput
            null, // Listeners
            null, // Locale
            null, // ReadParam
            null, // Reader
            null, // monitor
            Boolean.FALSE, // Use subsampling
            Boolean.FALSE // cast double to float
    };

    /** Constructor. */

    public GrassFileReadDescriptor() {
        super(resources, supportedModes, null, // sourceNames
                new Class[][]{null}, // sourceClasses
                paramNames, renderedParamClasses, renderedParamDefaults, null); // validParamValues
    }

    /**
     * Create the RenderedOp.
     * <p>
     * This method verify if the operation is already registered, if it isn't registered then
     * register the operation, then call it in order to obtain the image.
     * <p>
     * 
     * @param input The input source.
     * @param imageChoice The index of the image to read.
     * @param readMetadata Whether metadata should be read if available.
     * @param verifyInput Whether to verify the validity of the input source.
     * @param listeners EventListeners to be registered with the ImageReader.
     * @param locale The Locale for the ImageReader to use.
     * @param readParam Java Image I/O read parameter instance.
     * @param reader Java Image I/O reader instance.
     * @param monitor
     * @param setSampling
     * @param castToFloat
     * @param hints Hints possibly including an <code>ImageLayout</code>.
     * @return an image derived from the input source.
     */
    public static RenderedOp create( Object input, Integer imageChoice, Boolean readMetadata,
            Boolean verifyInput, EventListener[] listeners, Locale locale,
            ImageReadParam readParam, ImageReader reader, IProgressMonitorJGrass monitor,
            Boolean setSampling, Boolean castToFloat, RenderingHints hints ) {
        if (!isRegistered) {
            register();
            isRegistered = true;
        }
        
        if(reader instanceof GrassBinaryImageReader){
           ((GrassBinaryImageReader) reader).setCastDoubleToFloating(castToFloat);
           ((GrassBinaryImageReader) reader).setUseSubSamplingAsRequestedRowcols(setSampling);

        }
        TileCache tileCache = JAI.createTileCache();
        tileCache.setMemoryCapacity(24L*1024L*1024L);
        
        ParameterBlock pb = new ParameterBlock();

        pb.add(input);
        pb.add(imageChoice);
        pb.add(readMetadata);
        pb.add(verifyInput);
        pb.add(listeners);
        pb.add(locale);
        pb.add(readParam);
        pb.add(reader);
        pb.add(monitor);
        pb.add(setSampling);
        pb.add(castToFloat);

        return JAI.create(OPERATION_NAME, pb, hints);
    }

    /**
     * Validates the parameters in the supplied <code>ParameterBlock</code>.
     * <p>
     * If it is request, in addition to the usual check over the class of the parameter block,
     * verify if the file of the input exist.
     * </p>
     * 
     * @param modeName The operation mode.
     * @param args The source and parameters of the operation.
     * @param msg A container for any error messages.
     * @return Whether the supplied parameters are valid.
     */
    protected boolean validateParameters( String modeName, ParameterBlock args, StringBuffer msg ) {
        if (!super.validateParameters(modeName, args, msg)) {
            return false;
        }

        // Check "ImageChoice" for negative value(s).
        // RenderedRegistryMode.MODE_NAME in this case is equal to "rendered"
        if (modeName.equalsIgnoreCase(RenderedRegistryMode.MODE_NAME)) {
            if (args.getIntParameter(1) < 0) {
                // there is some problem with the index of the image
                msg.append(Messages.getString("imageIndex"));
                return false;
            }
        }

        // Check the input if so requested by "VerifyInput".
        Boolean verifyInput = (Boolean) args.getObjectParameter(3);
        if (verifyInput.booleanValue()) {
            // Get the Input parameter.
            Object input = args.getObjectParameter(0);

            if (input instanceof File || input instanceof String) {
                // Set file and path variables.
                File file = null;
                String path = null;
                if (input instanceof File) {
                    file = (File) input;
                    path = file.getPath();
                } else if (input instanceof String) {
                    path = (String) input;
                    file = new File(path);
                }

                // If input is a verify that it exists and is readable.
                if (file != null && file.exists() && file.canRead()) {
                    return true;
                } else {
                    msg.append("\"" + path + "\": "
                            + Messages.getString("FileImageReadDescriptor.cantReadFile"));
                    return false;
                }
            } else {
                // if input isn't a File or a String
                return false;
            }
        }

        return true;
    }
    private static boolean isRegistered = false;

    private static void register() {

        // register the operation
        OperationRegistry or;
        or = JAI.getDefaultInstance().getOperationRegistry();
        or.registerDescriptor(new GrassFileReadDescriptor());
        RenderedImageFactory rif = new GrassFileReadCRIF();
        RIFRegistry.register(or, OPERATION_NAME, "example", rif);
    }

}