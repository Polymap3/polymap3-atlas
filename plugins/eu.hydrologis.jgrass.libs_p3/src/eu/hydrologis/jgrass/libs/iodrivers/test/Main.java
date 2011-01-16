package eu.hydrologis.jgrass.libs.iodrivers.test;

import javax.imageio.ImageIO;

public class Main {

    public static void main(String[] args) {
        System.out.println("READERS");
        String[] readerFormatNames = ImageIO.getReaderFormatNames();
        for (String name : readerFormatNames) {
            System.out.println(name);
        }
        String[] readerMimeTypes = ImageIO.getReaderMIMETypes();
        for (String mimes : readerMimeTypes) {
            System.out.println(mimes);
        }
        System.out.println("WRITERS");
        String[] writerFormatNames = ImageIO.getWriterFormatNames();
        for (String name : writerFormatNames) {
            System.out.println(name);
        }
        String[] writerMimeTypes = ImageIO.getWriterMIMETypes();
        for (String mimes : writerMimeTypes) {
            System.out.println(mimes);
        }
    }
}
