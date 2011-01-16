package eu.hydrologis.jgrass.libs.iodrivers.geotools;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.media.jai.BorderExtenderConstant;
import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;

import net.refractions.udig.core.Pair;

import org.geotools.coverage.Category;
import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.ViewType;
import org.geotools.coverage.processing.OperationJAI;
import org.geotools.coverage.processing.Operations;
import org.geotools.geometry.Envelope2D;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.operation.builder.GridToEnvelopeMapper;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;

import eu.hydrologis.jgrass.libs.iodrivers.JGrassMapEnvironment;
import eu.hydrologis.jgrass.libs.iodrivers.imageio.GrassBinaryImageReader;
import eu.hydrologis.jgrass.libs.iodrivers.imageio.io.color.JGrassColorTable;
import eu.hydrologis.jgrass.libs.iodrivers.imageio.io.core.GrassBinaryRasterReadHandler;
import eu.hydrologis.jgrass.libs.iodrivers.imageio.metadata.GrassBinaryImageMetadata;
import eu.hydrologis.jgrass.libs.iodrivers.imageio.spi.GrassBinaryImageReaderSpi;
import eu.hydrologis.jgrass.libs.jai.operators.GrassFileReadDescriptor;
import eu.hydrologis.jgrass.libs.region.JGrassRegion;
import eu.hydrologis.jgrass.libs.utils.FileUtilities;
import eu.hydrologis.jgrass.libs.utils.JGrassConstants;
import eu.hydrologis.jgrass.libs.utils.JGrassUtilities;
import eu.hydrologis.jgrass.libs.utils.monitor.DummyProgressMonitor;
import eu.hydrologis.jgrass.libs.utils.monitor.IProgressMonitorJGrass;
import eu.hydrologis.jgrass.libs.utils.monitor.PrintStreamProgressMonitor;

/**
 * Coverage Reader class for reading GRASS raster maps.
 * <p>
 * The class reads a GRASS raster map from a GRASS workspace (see package documentation for further
 * info). The reading is really done via Imageio extended classes.
 * </p>
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @since 3.0
 * @see GrassBinaryImageReader
 * @see GrassBinaryRasterReadHandler
 */
public class GrassCoverageReader {
    private GrassBinaryImageReader imageReader = null;
    private String name;
    private PixelInCell cellAnchor = PixelInCell.CELL_CENTER;
    private Interpolation interpolation = Interpolation.getInstance(Interpolation.INTERP_NEAREST);
    private boolean useSubSamplingAsRequestedColsRows = false;
    private boolean castDoubleToFloating = false;
    private IProgressMonitorJGrass monitor = new DummyProgressMonitor();
    private JGrassMapEnvironment jgMapEnvironment;
    private String legendString;
    private String categoriesString;
    private double[] range;

    /**
     * Constructor for the {@link GrassCoverageReader}.
     * 
     * @param cellAnchor the object defining whether to assume the pixel value to be read in the
     *        grid's cell corner or center.
     * @param interpolation the type of interpolation to be used in the case some scaling or padding
     *        has to be done.
     * @param useSubSamplingAsColsRows a flag that gives the possibility to bypass the imageio
     *        subsampling mechanism. With GRASS maps this is often more performant in some boundary
     *        situations. In the case this flag is set to true, the subsampling values will be
     *        handled as the requested columns and rows.
     * @param castDoubleToFloating a flag that gives the possibility to force the reading of a map
     *        as a floating point map. This is necessary right now because of a imageio bug:
     *        https://jai-imageio-core.dev.java.net/issues/show_bug.cgi?id=180.
     * @param monitor a {@link IProgressMonitorJGrass monitor} for logging purposes. This can be
     *        null, in which case a dummy one will be used.
     */
    public GrassCoverageReader( PixelInCell cellAnchor, Interpolation interpolation,
            boolean useSubSamplingAsColsRows, boolean castDoubleToFloating,
            IProgressMonitorJGrass monitor ) {
        this.useSubSamplingAsRequestedColsRows = useSubSamplingAsColsRows;
        this.castDoubleToFloating = castDoubleToFloating;
        if (monitor != null)
            this.monitor = monitor;
        if (cellAnchor != null)
            this.cellAnchor = cellAnchor;
        if (interpolation != null)
            this.interpolation = interpolation;

    }

    /**
     * Sets the input source to use to the given {@link File file object}.
     * <p>
     * The input source must be set before any of the query or read methods are used.
     * </p>
     * 
     * @param input the {@link File} to use for future decoding.
     */
    public void setInput( File input ) {
        imageReader = new GrassBinaryImageReader(new GrassBinaryImageReaderSpi());
        imageReader.setInput(input);
        jgMapEnvironment = new JGrassMapEnvironment(input);
        name = input.getName();
    }

    /**
     * Performs the reading of the coverage.
     * <p>
     * This method read the grass file with a special image I/O class. If the image support the
     * tiling read the data with a special operation which are written for this case. The step are:
     * <li>set the region in the world</li>
     * <li>set the region to read in the JAI coordinate</li>
     * <li>read the data directly with driver or, if isTiling is true, with the operation.
     * <li>
     * <li>verify if the image cover whole the region, if not fill the rows and columns with padding
     * (with the Border operation)</li>
     * <li>scale the image to return an image with the number of columns and rows equal to the
     * requestedRegion</li>
     *<li>set the coverage (with the transformation from the JAI coordinate to the real world
     * coordinate.</li>
     * <p>
     * 
     * @param readParam the {@link GrassCoverageReadParam parameters} that influence the reading of
     *        the map. If null, the map is read in its original boundary and resolution.
     * @return the {@link GridCoverage2D read coverage}.
     * @throws IOException
     */
    public GridCoverage2D read( GrassCoverageReadParam readParam ) throws IOException {
        return read(readParam, false);
    }

    /**
     * Performs the reading of the coverage.
     * <p>
     * This method read the grass file with a special image I/O class. If the image support the
     * tiling read the data with a special operation which are written for this case. The step are:
     * <li>set the region in the world</li>
     * <li>set the region to read in the JAI coordinate</li>
     * <li>read the data directly with driver or, if isTiling is true, with the operation.
     * <li>
     * <li>verify if the image cover whole the region, if not fill the rows and columns with padding
     * (with the Border operation)</li>
     * <li>scale the image to return an image with the number of columns and rows equal to the
     * requestedRegion</li>
     *<li>set the coverage (with the transformation from the JAI coordinate to the real world
     * coordinate.</li>
     * <p>
     * 
     * @param readParam the {@link GrassCoverageReadParam parameters} that influence the reading of
     *        the map. If null, the map is read in its original boundary and resolution.
     * @param isTiled set parameter to true if the operation should use an image with tile, false
     *        otherwise.
     * @return the {@link GridCoverage2D read coverage}.
     * @throws IOException
     */
    @SuppressWarnings("nls")
    public GridCoverage2D read( GrassCoverageReadParam readParam, boolean isTiled )
            throws IOException {
        /*
         * retrieve original map region and crs
         */
        HashMap<String, String> metaDataTable = ((GrassBinaryImageMetadata) imageReader
                .getImageMetadata(0)).toHashMap();
        double fileNorth = Double.parseDouble(metaDataTable.get(GrassBinaryImageMetadata.NORTH));
        double fileSouth = Double.parseDouble(metaDataTable.get(GrassBinaryImageMetadata.SOUTH));
        double fileEast = Double.parseDouble(metaDataTable.get(GrassBinaryImageMetadata.EAST));
        double fileWest = Double.parseDouble(metaDataTable.get(GrassBinaryImageMetadata.WEST));
        int fileRows = Integer.parseInt(metaDataTable.get(GrassBinaryImageMetadata.NROWS));
        int fileCols = Integer.parseInt(metaDataTable.get(GrassBinaryImageMetadata.NCOLS));

        CoordinateReferenceSystem crs = DefaultGeographicCRS.WGS84;
        try {
            crs = CRS.parseWKT(metaDataTable.get(GrassBinaryImageMetadata.CRS));
        } catch (FactoryException e) {
            throw new IOException(e.getLocalizedMessage());
        }
        // where to put the region to read and if use subSampling
        ImageReadParam imageReadParam = new ImageReadParam();

        /*
         * the envelope that was requested, i.e. what has to be
         * given back in terms of bounds and resolution. 
         */
        Envelope requestedRegionEnvelope = null;
        /*
         * the read region, i.e. the requested region
         * without the parts east and south of the file region.
         * (since they would produce negative origin)
         */
        Rectangle sourceRegion = null;

        int requestedRows = 0;
        int requestedCols = 0;
        double requestedWest = -1;
        double requestedEast = -1;
        double requestedSouth = -1;
        double requestedNorth = -1;
        double requestedXres = -1;
        double requestedYres = -1;

        int subSamplingX = 1;
        int subSamplingY = 1;

        int xPaddingSx = 0;
        int yPaddingBottom = 0;

        if (readParam != null) {
            /*
             * the user requested a particular read region. and that is 
             * exactly the region we have to give back.
             */
            JGrassRegion requestedRegion = readParam.getRequestedWorldRegion();
            requestedRows = requestedRegion.getRows();
            requestedCols = requestedRegion.getCols();
            requestedWest = requestedRegion.getWest();
            requestedEast = requestedRegion.getEast();
            requestedSouth = requestedRegion.getSouth();
            requestedNorth = requestedRegion.getNorth();
            requestedXres = requestedRegion.getWEResolution();
            requestedYres = requestedRegion.getNSResolution();
            /*
             * define the raster space region
             */
            double scaleX = fileCols / (fileEast - fileWest);
            double scaleY = fileRows / (fileNorth - fileSouth);
            double EPS = 0.0;// 1E-6;

            // awt space seen in the world view (north is ymax)
            int xmin = (int) Math.floor((requestedWest - fileWest) * scaleX + EPS);
            int xmax = (int) Math.ceil((requestedEast - fileWest) * scaleX - EPS);
            int ymax = (int) Math.floor((requestedNorth - fileSouth) * scaleY + EPS);
            int ymin = (int) Math.ceil((requestedSouth - fileSouth) * scaleY - EPS);

            /*
             * clip away region that is west and south of the image bounds. 
             * This is important because the imageio source region can't 
             * be < 0. Later the clipped parts will be resolved by translation 
             * of the image.
             */
            if (xmin < 0) {
                xPaddingSx = xmin;
                xmin = 0;
            }
            if (ymin < 0) {
                yPaddingBottom = ymin;
                ymin = 0;
            }

            /*
             * the pixel space region that will be extracted.
             * Since the origin is always 0,0, we can continue 
             * to see this in world view.
             */
            sourceRegion = new Rectangle(xmin, ymin, (xmax - xmin), ymax - ymin);
            requestedRegionEnvelope = new Envelope2D(crs, requestedWest, requestedSouth,
                    requestedEast - requestedWest, requestedNorth - requestedSouth);

            /*
             * define the subsampling values. This done starting from the original's image
             * resolution.
             */
            if (!useSubSamplingAsRequestedColsRows) {
                /*
                 * in this case we respect the original subsampling contract.
                 */
                JGrassRegion tmpRegion = new JGrassRegion(requestedRegion);
                tmpRegion.setWEResolution((fileEast - fileWest) / (double) fileCols);
                tmpRegion.setNSResolution((fileNorth - fileSouth) / (double) fileRows);
                subSamplingX = (int) Math.floor((double) tmpRegion.getCols()
                        / (double) requestedCols);
                subSamplingY = (int) Math.floor((double) tmpRegion.getRows()
                        / (double) requestedRows);
                if (subSamplingX == 0)
                    subSamplingX = 1;
                if (subSamplingY == 0)
                    subSamplingY = 1;
                if (subSamplingX != subSamplingY) {
                    if (subSamplingX < subSamplingY) {
                        subSamplingY = subSamplingX;
                    } else {
                        subSamplingX = subSamplingY;
                    }
                }
            } else {
                /*
                 * in this case the subsampling values are interpreted 
                 * as columns and row numbers to be used to calculate 
                 * the resolution from the given boundaries.
                 */
                double sourceCols = (requestedEast - requestedWest)
                        / (1 + (xPaddingSx / (xmax - xmin))) / requestedXres;
                double sourceRows = (requestedNorth - requestedSouth)
                        / (1 + (yPaddingBottom / (ymax - ymin))) / requestedYres;
                /*
                 * the padding has to be removed since inside the reader
                 * the padding is ignored and non present in the sourceRegion that
                 * is passed.
                 */
                sourceCols =  sourceCols + xPaddingSx;
                sourceRows = sourceRows + yPaddingBottom;
                subSamplingX = (int) Math.round(sourceCols);
                subSamplingY = (int) Math.round(sourceRows);
            }

        } else {
            /*
             * if no region has been requested, the source and requested region 
             * are the same, i.e. the whole raster is read and passed.
             */
            requestedRows = fileRows;
            requestedCols = fileCols;
            requestedWest = fileWest;
            requestedEast = fileEast;
            requestedSouth = fileSouth;
            requestedNorth = fileNorth;
            double scaleX = fileCols / (fileEast - fileWest);
            double scaleY = fileRows / (fileNorth - fileSouth);
            double EPS = 1E-6;
            int xmin = (int) Math.floor((requestedWest - fileWest) * scaleX + EPS);
            int xmax = (int) Math.ceil((requestedEast - fileWest) * scaleX - EPS);
            int ymin = (int) Math.floor((fileNorth - requestedNorth) * scaleY + EPS);
            int ymax = (int) Math.ceil((fileNorth - requestedSouth) * scaleY - EPS);
            sourceRegion = new Rectangle(xmin, ymin, (xmax - xmin), ymax - ymin);
            requestedRegionEnvelope = new Envelope2D(crs, requestedWest, requestedSouth,
                    requestedEast - requestedWest, requestedNorth - requestedSouth);
        }
        // now we have enough info to create the ImageReadParam
        imageReadParam.setSourceRegion(sourceRegion);
        imageReadParam.setSourceSubsampling(subSamplingX, subSamplingY, 0, 0);

        PlanarImage image = null;
        if (isTiled) {
            // get the dimension of size
            int tileSize = 512;
            if (readParam != null) {
                tileSize = readParam.getTileSize();
            }
            // set a new layout to use tiling.
            ImageLayout layout = new ImageLayout();
            layout.setTileWidth(tileSize);
            layout.setTileHeight(tileSize);
            RenderingHints hints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, layout);
            int nTileH;
            // verify the number of tile in order to set the exact % progress in the read operation.
            if (sourceRegion.width == tileSize) {
                nTileH = 1;
            } else {
                nTileH = (int) sourceRegion.width / tileSize + 1;
            }
            monitor.beginTask(name + " in tiles of size " + tileSize, sourceRegion.height * nTileH);
            // extract the value of the file to the imageReader because this is the first parameter
            // of the operation.
            File f = jgMapEnvironment.getCELL();
            image = GrassFileReadDescriptor.create(f, 0, false, false, null, null, imageReadParam,
                    imageReader, monitor, false, false, hints);
        } else {
            image = imageReader.read(0, imageReadParam, useSubSamplingAsRequestedColsRows,
                    castDoubleToFloating, monitor);
        }

        /*
         * N.B. image region that has been read is the intersection between
         * the requested region and the file region.
         */
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();
        /*
         * Now there are different situations:
         * 1) the requested region is completely contained in the file region,
         *    in which case no padding and scaling is needed, since the 
         *    reader already reads the proper resolution anyway. So the bounds
         *    are the right ones and the resolution also.
         * 2) the requested region has parts outside the right and top border,
         *    in which case 1) applies again.
         * 3) the requested region has parts outside the left or bottom bounds
         *    which introduces negative values in the source region, therefore
         *    bringing in the need for removing those negative parts prior to 
         *    reading and then padding those. The resolution is assumed to be 
         *    anyway the requested, so padding has to occur. 
         * 4) the case in which subsampling is used the real imageio way,
         *    also scaling has to occurr, since the region may be different from 
         *    the exact requested. 
         */

        // case 1 and 2
        PlanarImage finalImage = image;
        // case 3 and 4
        if (requestedSouth < fileSouth || requestedWest < fileWest || requestedRows != imageHeight
                || requestedCols != imageWidth) {
            /*
             * Pad and translate to left and bottom because 
             * some padding may have been occurred.
             */
            ParameterBlock block = new ParameterBlock();
            block.addSource(image);
            block.add(Math.abs(xPaddingSx));
            block.add(Math.abs(0));
            block.add(Math.abs(0));
            block.add(Math.abs(yPaddingBottom));
            block.add(new BorderExtenderConstant(new double[]{Double.NaN}));
            RenderedOp paddedImage = JAI.create("Border", block);

            block = new ParameterBlock();
            block.addSource(paddedImage);
            block.add((float) -xPaddingSx);
            block.add((float) -yPaddingBottom);
            RenderedOp translatedImage = JAI.create("translate", block);

            /*
             * scale the image in order to return the image with the 
             * resolution of the requestedRegion.
             */
            // interpolation = Interpolation.getInstance(Interpolation.INTERP_BILINEAR);
            block = new ParameterBlock();
            block.addSource(translatedImage);
            block.add((float) requestedCols / (float) translatedImage.getWidth());
            block.add((float) requestedRows / (float) translatedImage.getHeight());
            block.add(0F);
            block.add(0F);
            block.add(interpolation);
            finalImage = JAI.create("scale", block);
        }

        range = imageReader.getRasterReader().getRange();

        /*
         * create the categories from the color rules
         */
        GridSampleDimension band = createGridSampleDimension(metaDataTable, range);
        band = band.geophysics(true);

        // create a relationship between the real region in the world and the jai space. N.B. the
        // image dimension is only the real dimension which can be extract to a file.
        GridToEnvelopeMapper g2eMapper = new GridToEnvelopeMapper();
        g2eMapper.setEnvelope(requestedRegionEnvelope);
        g2eMapper.setGridRange(new GridEnvelope2D(0, 0, requestedCols, requestedRows));

        g2eMapper.setPixelAnchor(cellAnchor);
        MathTransform gridToEnvelopeTransform = g2eMapper.createTransform();
        GridCoverageFactory factory = CoverageFactoryFinder.getGridCoverageFactory(null);

        GridCoverage2D coverage2D = factory.create(name, finalImage, crs, gridToEnvelopeTransform,
                new GridSampleDimension[]{band}, null, null);
        return coverage2D;

    }

    public static void printImage( RenderedImage renderedImage, PrintStream out ) {
        RectIter iter = RectIterFactory.create(renderedImage, null);
        do {
            do {
                out.print(iter.getSampleDouble() + " ");
            } while( !iter.nextPixelDone() );
            iter.startPixels();
            out.println();
        } while( !iter.nextLineDone() );
    }

    private GridSampleDimension createGridSampleDimension( HashMap<String, String> metaDataTable,
            double[] range ) throws IOException {
        legendString = metaDataTable.get(GrassBinaryImageMetadata.COLOR_RULES_DESCRIPTOR);
        categoriesString = metaDataTable.get(GrassBinaryImageMetadata.CATEGORIES_DESCRIPTOR);
        String[] colorRulesSplit;
        if (!legendString.matches(".*Infinty.*|.*NaN.*")) { //$NON-NLS-1$
            colorRulesSplit = legendString.split(GrassBinaryImageMetadata.RULESSPLIT);
        } else {
            List<String> defColorTable = JGrassColorTable.createDefaultColorTable(range, 255);
            colorRulesSplit = (String[]) defColorTable.toArray(new String[defColorTable.size()]);
            // make it also persistent
            File colrFile = jgMapEnvironment.getCOLR();
            JGrassUtilities.makeColorRulesPersistent(colrFile, defColorTable, range, 255);
        }

        int rulesNum = colorRulesSplit.length;

        int COLORNUM = 60000;

        if (colorRulesSplit != null) {
            if (colorRulesSplit.length > COLORNUM) {
                COLORNUM = colorRulesSplit.length + 1;
            }
            if (COLORNUM > 65500) {
                COLORNUM = 65500;
            }

            List<Category> catsList = new ArrayList<Category>();

            double[][] values = new double[rulesNum][2];
            Color[][] colors = new Color[rulesNum][2];
            for( int i = 0; i < rulesNum; i++ ) {
                String colorRule = colorRulesSplit[i];
                JGrassColorTable.parseColorRule(colorRule, values[i], colors[i]);

                // System.out.println("Processing colorrule: " + colorRule);
            }
            Color empty = new Color(Color.WHITE.getRed(), Color.WHITE.getGreen(), Color.WHITE
                    .getBlue(), 0);

            double a = (values[values.length - 1][1] - values[0][0]) / (double) (COLORNUM - 1);
            double pmin = 1.0;
            double scale = a;
            if (scale == 0) {
                scale = 1;
            }
            double offSet = values[0][0] - scale * pmin;

            // TODO case of int maps
            // if (true) {
            // pmin = 2;
            // offSet = Integer.MAX_VALUE - scale * pmin;
            // Category noDataInt = new Category("intnovalue", new Color[]{empty, empty}, 0, 1,
            // scale, offSet);
            // catsList.add(noDataInt);
            // } else {
            // the NaN nodata category
            Category noData = new Category("notdata", empty, 0);
            catsList.add(noData);
            // }

            int previousUpper = -Integer.MAX_VALUE;
            for( int i = 0; i < rulesNum; i++ ) {
                StringBuilder sB = new StringBuilder();
                sB.append(name);
                sB.append("_"); //$NON-NLS-1$
                sB.append(i);

                double tmpLower = values[i][0];
                double tmpUpper = values[i][1];
                int lower = (int) ((tmpLower - values[0][0]) / scale + pmin);
                int upper = (int) ((tmpUpper - values[0][0]) / scale + pmin);
                if (lower <= previousUpper) {
                    lower = previousUpper + 1;
                }
                if (lower >= upper) {
                    upper = lower + 1;
                }
                previousUpper = upper;

                Category dataCategory = new Category(sB.toString(), colors[i], lower, upper, scale,
                        offSet);

                catsList.add(dataCategory);

                // if (i == rulesNum - 1) {
                // /*
                // * this is used to make the int maps use the Integer.MAX_VALUE
                // * as the novalue in visualization. It doesn't affect the values.
                // */
                // Category intdataCategory = new Category("intnodata", new Color[]{empty, empty},
                // upper, upper + 1, scale, offSet);
                // catsList.add(intdataCategory);
                // }
            }

            Category[] array = (Category[]) catsList.toArray(new Category[catsList.size()]);
            return new GridSampleDimension(name, array, null);
        } else {
            return new GridSampleDimension(name, new Category[]{}, null);
        }
    }
    /**
     * Getter for the legend string.
     * 
     * @return the legendstring.
     */
    public String getLegendString() {
        return legendString;
    }

    /**
     * Getter for the categories string.
     * 
     * @return the categories string.
     */
    public String getCategoriesString() {
        return categoriesString;
    }

    /**
     * Gets the range.
     * 
     * <b>Note that the range is available only if the raster was read once.</b> 
     * 
     * @return the range non considering novalues.
     */
    public double[] getRange() {
        return range;
    }

    @SuppressWarnings("nls")
    public static void main( String[] args ) {
        try {
            String mapPath = "/home/daniele/Jgrassworkspace/testLettura/provaL/cell/testa";
            // ";
            // String mapPath =
            // "/home/moovida/rcpdevelopment/WORKSPACES/eclipseGanimede/jai_tests/spearfish/PERMANENT/cell/elevation.dem"
            // ;

            /*
             * test1 - read the whole data
             */
            Interpolation interpolation = Interpolation.getInstance(Interpolation.INTERP_NEAREST);
            GrassCoverageReader tmp = new GrassCoverageReader(PixelInCell.CELL_CENTER,
                    interpolation, false, false, new PrintStreamProgressMonitor(System.out));
            tmp.setInput(new File(mapPath));

            JGrassRegion readRegion = JGrassRegion
                    .getActiveRegionFromMapset("/home/daniele/Jgrassworkspace/testLettura/provaL");
            GrassCoverageReadParam gcReadParam = new GrassCoverageReadParam(readRegion);

            GridCoverage2D coverage2D = tmp.read(gcReadParam);
            Raster fileImage = coverage2D.getRenderedImage().getData();
            for( int j = 0; j < fileImage.getHeight(); j++ ) {
                for( int i = 0; i < fileImage.getWidth(); i++ ) {
                    System.out.print(fileImage.getSampleDouble(i, j, 0) + "\t");
                }
                System.out.println();
            }

            if (true) {
                System.exit(0);
            }

            Point2D point = new Point2D.Double(608940.0, 4914330.0);
            double[] buffer = new double[1];
            System.out.println(coverage2D.evaluate(point, buffer)[0]);

            interpolation = Interpolation.getInstance(Interpolation.INTERP_BILINEAR);
            GridCoverage2D interpolated = (GridCoverage2D) Operations.DEFAULT.interpolate(
                    coverage2D, interpolation);
            System.out.println(interpolated.evaluate(point, buffer)[0]);

            GridCoverage2D integerView = coverage2D.view(ViewType.RENDERED);
            System.out.println(integerView.evaluate(point, buffer)[0]);

            RenderedImage renderedImage = integerView.getRenderedImage();
            OutputStream oStream = new FileOutputStream(new File("/Users/moovida/Desktop/test.png")); //$NON-NLS-1$
            ImageIO.write(renderedImage, "png", oStream); //$NON-NLS-1$

            // GridCoverage2D geographic = (GridCoverage2D)
            // Operations.DEFAULT.resample(coverage2D,
            // DefaultGeographicCRS.WGS84);
            // // GridGeometry2D g2d = new GridGeometry2D()
            // // with GridGeometry I can control the target grid
            //        
            // ImageIO.write(geographic.view(ViewType.RENDERED).getRenderedImage(),
            // "png", new File(
            // "/home/moovida/Desktop/test2.png"));
            //
            /*
             * test2 - read the map at lower resolution
             */
            tmp = new GrassCoverageReader(null, null, false, false, new PrintStreamProgressMonitor(
                    System.out));
            tmp.setInput(new File(mapPath));

            readRegion = new JGrassRegion(589980.0, 609000.0, 4913690.0, 4928030.0, 478, 200);
            gcReadParam = new GrassCoverageReadParam(readRegion);

            coverage2D = tmp.read(gcReadParam, true);
            integerView = coverage2D.view(ViewType.RENDERED);
            renderedImage = integerView.getRenderedImage();

            oStream = new FileOutputStream(new File("/Users/moovida/Desktop/test1.png")); //$NON-NLS-1$
            ImageIO.write(renderedImage, "png", oStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
