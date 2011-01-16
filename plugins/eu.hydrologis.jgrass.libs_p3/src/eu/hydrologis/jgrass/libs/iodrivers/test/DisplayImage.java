package eu.hydrologis.jgrass.libs.iodrivers.test;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferDouble;
import java.io.File;
import java.io.IOException;

import javax.imageio.IIOImage;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.swing.JFrame;

import eu.hydrologis.jgrass.libs.iodrivers.imageio.GrassBinaryImageReader;
import eu.hydrologis.jgrass.libs.iodrivers.imageio.spi.GrassBinaryImageReaderSpi;
import eu.hydrologis.jgrass.libs.iodrivers.imageio.spi.GrassBinaryImageWriterSpi;

/**
 * displayImage.java -- displays an image or a series of images contained at the URL provided on the
 * command line.
 */
public class DisplayImage extends JFrame implements MouseMotionListener {
    private BufferedImage bi;
    private Insets insets = null;
    private GrassBinaryImageReader reader = null;
    private int imageIndex = 0;
    private File file = null;

    private static String infile = null;
    private static String outfile = null;

    public DisplayImage( String filePath ) {

        try {
            /*
             * get ImageReaders which can decode the given ImageInputStream
             */
            GrassBinaryImageReaderSpi readerSpi = new GrassBinaryImageReaderSpi();
            reader = (GrassBinaryImageReader) readerSpi.createReaderInstance();
            file = new File(filePath);
            reader.setInput(file);
            IIOMetadata imageMetadata = reader.getImageMetadata(0);
            System.out.println(imageMetadata.toString());
            addMouseMotionListener(this);
            addNotify();
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setVisible(true);
            imageIndex = 0;
            Rectangle rectangle;
            ImageReadParam imageReadParam = reader.getDefaultReadParam();
            rectangle = new Rectangle(5,
            5,10,10);
            imageReadParam.setSourceRegion(rectangle);

            try {
                bi = reader.read(imageIndex, imageReadParam);
            } catch (IOException e) {
                e.printStackTrace();
            }
            setSize(bi.getWidth(), bi.getHeight());
            repaint();

            System.out.println("*******************************************");
            // ImageDump.dump(bi);
            System.out.println("*******************************************");

            DataBufferDouble dataBuffer = (DataBufferDouble) bi.getData().getDataBuffer();
            double[] ds = dataBuffer.getBankData()[0];

            if (outfile == null) {
                return;
            }
            GrassBinaryImageWriterSpi writerSpi = new GrassBinaryImageWriterSpi();
            ImageWriter writer = writerSpi.createWriterInstance();
            File file = new File(outfile);
            writer.setOutput(file);
            IIOImage img = new IIOImage(bi, null, imageMetadata);
            writer.write(null, img, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
     * simple image paint routine which double buffers display
     */
    public void paint( Graphics g ) {
        Graphics2D g2d = (Graphics2D) g;

        if (bi != null) {
            g2d.drawImage(bi, 0, 0, null);
        }
    }

    public static void main( String[] args ) {

        // infile = "/home/moovida/grass/grassdb/flangitest/prova/cell/flowy";
        // infile = "/home/moovida/grass/grassdb/flangitest/prova/cell/flowino";
        // infile = "/home/moovida/grass/grassdb/flangitest/prova/cell/aspect";
        //infile = "/home/moovida/rcpdevelopment/WORKSPACES/eclipseGanimede/jai_tests/spearfish/PERMANENT/cell/geology";
        // infile = "/Users/moovida/data/spearfish60/PERMANENT/cell/elevation.dem";

        infile = "/home/daniele/Jgrassworkspace/trentino/provaPitfiller/cell/testDem";

        // outfile = "/home/moovida/grass/grassdb/flangitest/prova/cell/writetest";

        // to test with svnrepo dataset uncomment the following and supply the
        // root folder
        // String jaiTestFolder =
        // "/home/moovida/rcpdevelopment/WORKSPACES/eclipseGanimede/jai_tests/";
        // infile = jaiTestFolder + File.separator + "spearfish/PERMANENT/cell/elevation.dem";
        // outfile = jaiTestFolder + File.separator + "spearfish/PERMANENT/cell/writetest";
        new DisplayImage(infile);
    }

    public void mouseDragged( MouseEvent e ) {
        // TODO Auto-generated method stub
    }

    public void mouseMoved( MouseEvent me ) {
        int x = me.getX(); // Get the mouse coordinates.
        int y = me.getY();
        if ((x >= bi.getWidth()) || (y >= bi.getHeight())) // Avoid exceptions, consider only
        { // pixels within image bounds.
            setTitle("No data!");
            return;
        }
        double[] dpixel = bi.getRaster().getPixel(x, y, (double[]) null); // Read the original
        // pixel value.
        setTitle("(DEM data) " + x + "," + y + ": " + dpixel[0]);
    }
}
