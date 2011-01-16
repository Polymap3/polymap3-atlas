package eu.hydrologis.jgrass.libs.iodrivers.test;

import java.awt.*;
import java.awt.color.*;
import java.awt.image.*;

public class ImageDump {
    public static void dump( BufferedImage image ) {
        dumpAttributes(image);
        dumpColorModel(image.getColorModel());
        dumpRaster(image.getRaster());
    }

    public static void dumpAttributes( BufferedImage image ) {
        System.out.println("BASIC BUFFEREDIMAGE ATTRIBUTES:");
        System.out.println("    instance of " + image.getClass().getName());
        System.out.println("    height=" + image.getHeight());
        System.out.println("    width=" + image.getWidth());
        System.out.println("    minX=" + image.getMinX());
        System.out.println("    minY=" + image.getMinY());
        System.out.print("  type=" + image.getType() + " (");
        System.out.print(getImageTypeString(image.getType()));
        System.out.println(')');
        System.out.println("    isAlphaPremultiplied=" + image.isAlphaPremultiplied());
    }

    public static String getImageTypeString( int type ) {
        switch( type ) {
        case BufferedImage.TYPE_CUSTOM:
            return "TYPE_CUSTOM";
        case BufferedImage.TYPE_INT_RGB:
            return "TYPE_INT_RGB";
        case BufferedImage.TYPE_INT_ARGB:
            return "TYPE_INT_ARGB";
        case BufferedImage.TYPE_INT_ARGB_PRE:
            return "TYPE_INT_ARGB_PRE";
        case BufferedImage.TYPE_INT_BGR:
            return "TYPE_INT_BGR";
        case BufferedImage.TYPE_3BYTE_BGR:
            return "TYPE_3BYTE_BGR";
        case BufferedImage.TYPE_4BYTE_ABGR:
            return "TYPE_4BYTE_ABGR";
        case BufferedImage.TYPE_4BYTE_ABGR_PRE:
            return "TYPE_4BYTE_ABGR_PRE";
        case BufferedImage.TYPE_USHORT_565_RGB:
            return "TYPE_USHORT_565_RGB";
        case BufferedImage.TYPE_USHORT_555_RGB:
            return "TYPE_USHORT_555_RGB";
        case BufferedImage.TYPE_BYTE_GRAY:
            return "TYPE_BYTE_GRAY";
        case BufferedImage.TYPE_USHORT_GRAY:
            return "TYPE_USHORT_GRAY";
        case BufferedImage.TYPE_BYTE_BINARY:
            return "TYPE_BYTE_BINARY";
        case BufferedImage.TYPE_BYTE_INDEXED:
            return "TYPE_BYTE_INDEXED";
        default:
            return "unknown type?";
        }
    }

    public static void dumpColorModel( ColorModel colorModel ) {
        dumpColorSpace(colorModel.getColorSpace());
        System.out.println("COLOR MODEL:");
        System.out.println("    instance of " + colorModel.getClass().getName());
        System.out.println("    hasAlpha=" + colorModel.hasAlpha());
        System.out.println("    isAlphaPremultiplied=" + colorModel.isAlphaPremultiplied());
        System.out.print("  transparency=" + colorModel.getTransparency() + " (");
        System.out.print(getTransparencyString(colorModel.getTransparency()));
        System.out.println(')');
        System.out.print("  transferType=" + colorModel.getTransferType() + " (");
        System.out.print(getTypeString(colorModel.getTransferType()));
        System.out.println(')');
        System.out.println("    numComponents=" + colorModel.getNumComponents());
        System.out.println("    numColorComponents=" + colorModel.getNumColorComponents());
        System.out.println("    pixelSize(bits/pixel)=" + colorModel.getPixelSize());
        for( int i = 0, ub = colorModel.getNumComponents(); i < ub; ++i )
            System.out.println("    componentSize[" + i + "]=" + colorModel.getComponentSize(i));

        if (colorModel instanceof IndexColorModel)
            dumpIndexColorModel((IndexColorModel) colorModel);
        else if (colorModel instanceof PackedColorModel)
            dumpPackedColorModel((PackedColorModel) colorModel);
    }

    public static void dumpIndexColorModel( IndexColorModel colorModel ) {
        System.out.println("    mapSize=" + colorModel.getMapSize());
        System.out.println("    isValid=" + colorModel.isValid());
        System.out.println("    transparentPixel=" + colorModel.getTransparentPixel());
    }

    public static void dumpPackedColorModel( PackedColorModel colorModel ) {
        int[] masks = colorModel.getMasks();
        for( int i = 0; i < masks.length; ++i )
            System.out.println("    masks[" + i + "]=" + Integer.toHexString(masks[i]));
    }

    public static String getTransparencyString( int transparency ) {
        switch( transparency ) {
        case Transparency.OPAQUE:
            return "OPAQUE";
        case Transparency.BITMASK:
            return "BITMASK";
        case Transparency.TRANSLUCENT:
            return "TRANSLUCENT";
        default:
            return "unknown transparency?";
        }
    }

    public static String getTypeString( int type ) {
        switch( type ) {
        case DataBuffer.TYPE_BYTE:
            return "BYTE";
        case DataBuffer.TYPE_USHORT:
            return "USHORT";
        case DataBuffer.TYPE_SHORT:
            return "SHORT";
        case DataBuffer.TYPE_INT:
            return "INT";
        case DataBuffer.TYPE_FLOAT:
            return "FLOAT";
        case DataBuffer.TYPE_DOUBLE:
            return "DOUBLE";
        default:
            return "unknown type?";
        }
    }

    public static void dumpColorSpace( ColorSpace colorSpace ) {
        System.out.println("COLOR SPACE:");
        System.out.println("    instance of " + colorSpace.getClass().getName());
        System.out.println("    isCS_sRGB=" + colorSpace.isCS_sRGB());
        System.out.print("  type=" + colorSpace.getType() + " (");
        System.out.print(getColorSpaceTypeString(colorSpace.getType()));
        System.out.println(')');
        System.out.println("    numComponents=" + colorSpace.getNumComponents());
        for( int i = 0, ub = colorSpace.getNumComponents(); i < ub; ++i ) {
            System.out.print("  name[" + i + "]=" + colorSpace.getName(i));
            System.out.print(", minValue=" + colorSpace.getMinValue(i));
            System.out.println(", maxValue=" + colorSpace.getMaxValue(i));
        }
    }

    public static String getColorSpaceTypeString( int type ) {
        switch( type ) {
        case ColorSpace.TYPE_XYZ:
            return "TYPE_XYZ";
        case ColorSpace.TYPE_Lab:
            return "TYPE_Lab";
        case ColorSpace.TYPE_Luv:
            return "TYPE_Luv";
        case ColorSpace.TYPE_YCbCr:
            return "TYPE_YCbCr";
        case ColorSpace.TYPE_Yxy:
            return "TYPE_Yxy";
        case ColorSpace.TYPE_RGB:
            return "TYPE_RGB";
        case ColorSpace.TYPE_GRAY:
            return "TYPE_GRAY";
        case ColorSpace.TYPE_HSV:
            return "TYPE_HSV";
        case ColorSpace.TYPE_HLS:
            return "TYPE_HLS";
        case ColorSpace.TYPE_CMYK:
            return "TYPE_CMYK";
        case ColorSpace.TYPE_CMY:
            return "TYPE_CMY";
        case ColorSpace.TYPE_2CLR:
            return "TYPE_2CLR";
        case ColorSpace.TYPE_3CLR:
            return "TYPE_3CLR";
        case ColorSpace.TYPE_4CLR:
            return "TYPE_4CLR";
        case ColorSpace.TYPE_5CLR:
            return "TYPE_5CLR";
        case ColorSpace.TYPE_6CLR:
            return "TYPE_6CLR";
        case ColorSpace.TYPE_7CLR:
            return "TYPE_7CLR";
        case ColorSpace.TYPE_8CLR:
            return "TYPE_8CLR";
        case ColorSpace.TYPE_9CLR:
            return "TYPE_9CLR";
        case ColorSpace.TYPE_ACLR:
            return "TYPE_ACLR";
        case ColorSpace.TYPE_BCLR:
            return "TYPE_BCLR";
        case ColorSpace.TYPE_CCLR:
            return "TYPE_CCLR";
        case ColorSpace.TYPE_DCLR:
            return "TYPE_DCLR";
        case ColorSpace.TYPE_ECLR:
            return "TYPE_ECLR";
        case ColorSpace.TYPE_FCLR:
            return "TYPE_FCLR";
        default:
            return "unknown type?";
        }
    }

    public static void dumpRaster( WritableRaster raster ) {
        System.out.println("RASTER:");
        System.out.println("    instance of " + raster.getClass().getName());
        System.out.println("    height=" + raster.getHeight());
        System.out.println("    width=" + raster.getWidth());
        System.out.println("    minX=" + raster.getMinX());
        System.out.println("    minY=" + raster.getMinY());
        System.out.println("    sampleModelTranslateX=" + raster.getSampleModelTranslateX());
        System.out.println("    sampleModelTranslateY=" + raster.getSampleModelTranslateY());
        System.out.println("    numBands=" + raster.getNumBands());
        System.out.println("    numDataElements=" + raster.getNumDataElements());
        System.out.print("  transferType=" + raster.getTransferType() + " (");
        System.out.print(getTypeString(raster.getTransferType()));
        System.out.println(')');
        System.out.println("    parent is null=" + (null == raster.getParent()));
        dumpDataBuffer(raster.getDataBuffer());
        dumpSampleModel(raster.getSampleModel());
    }

    public static void dumpDataBuffer( DataBuffer dataBuffer ) {
        System.out.println("DATA BUFFER:");
        System.out.println("    instance of " + dataBuffer.getClass().getName());
        System.out.print("  dataType=" + dataBuffer.getDataType() + " (");
        System.out.print(getTypeString(dataBuffer.getDataType()));
        System.out.println(')');
        System.out.println("    numBanks=" + dataBuffer.getNumBanks());
        System.out.println("    size=" + dataBuffer.getSize());
        for( int i = 0, ub = dataBuffer.getNumBanks(); i < ub; ++i )
            System.out.println("    offset[" + i + "]=" + dataBuffer.getOffsets()[i]);
    }

    public static void dumpSampleModel( SampleModel sampleModel ) {
        System.out.println("SAMPLE MODEL:");
        System.out.println("    instance of " + sampleModel.getClass().getName());
        System.out.println("    height=" + sampleModel.getHeight());
        System.out.println("    width=" + sampleModel.getWidth());
        System.out.print("  transferType=" + sampleModel.getTransferType() + " (");
        System.out.print(getTypeString(sampleModel.getTransferType()));
        System.out.println(')');
        System.out.print("  dataType=" + sampleModel.getDataType() + " (");
        System.out.print(getTypeString(sampleModel.getDataType()));
        System.out.println(')');
        System.out.println("    numBands=" + sampleModel.getNumBands());
        System.out.println("    numDataElements=" + sampleModel.getNumDataElements());
        int[] sampleSize = sampleModel.getSampleSize();
        for( int i = 0, ub = sampleSize.length; i < ub; ++i )
            System.out.println("    sampleSize[" + i + "]=" + sampleSize[i]);
        if (sampleModel instanceof SinglePixelPackedSampleModel)
            dumpSinglePixelPackedSampleModel((SinglePixelPackedSampleModel) sampleModel);
        else if (sampleModel instanceof MultiPixelPackedSampleModel)
            dumpMultiPixelPackedSampleModel((MultiPixelPackedSampleModel) sampleModel);
        else if (sampleModel instanceof ComponentSampleModel)
            dumpComponentSampleModel((ComponentSampleModel) sampleModel);
    }

    public static void dumpSinglePixelPackedSampleModel( SinglePixelPackedSampleModel sampleModel ) {
        System.out.println("    scanlineStride=" + sampleModel.getScanlineStride());
        int[] bitMasks = sampleModel.getBitMasks();
        for( int i = 0; i < bitMasks.length; ++i )
            System.out.println("    bitmasks[" + i + "]=" + Integer.toHexString(bitMasks[i]));
    }

    public static void dumpMultiPixelPackedSampleModel( MultiPixelPackedSampleModel sampleModel ) {
        System.out.println("    scanlineStride=" + sampleModel.getScanlineStride());
        System.out.println("    pixelBitStride=" + sampleModel.getPixelBitStride());
    }

    public static void dumpComponentSampleModel( ComponentSampleModel sampleModel ) {
        System.out.println("    scanlineStride=" + sampleModel.getScanlineStride());
        System.out.println("    pixelStride=" + sampleModel.getPixelStride());
        int[] bandOffsets = sampleModel.getBandOffsets();
        for( int i = 0; i < bandOffsets.length; ++i )
            System.out.println("    bandOffsets[" + i + "]=" + bandOffsets[i]);
        int[] bankIndices = sampleModel.getBankIndices();
        for( int i = 0; i < bankIndices.length; ++i )
            System.out.println("    bankIndices[" + i + "]=" + bankIndices[i]);
    }
}
