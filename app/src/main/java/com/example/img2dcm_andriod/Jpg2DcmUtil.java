package com.example.img2dcm_andriod;




import org.dcm4che3.data.*;
import org.dcm4che3.imageio.codec.XPEGParser;
import org.dcm4che3.imageio.codec.jpeg.JPEGParser;
import org.dcm4che3.io.DicomOutputStream;



import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;



public class Jpg2DcmUtil {

    private static final int BUFFER_SIZE = 8162;
    private static byte[] buf = new byte[BUFFER_SIZE];

    /**
     *
     * Convert jpeg to DICOM
     *
     * @param srcFilePath The jpeg filename.
     * @param destFilePath The output DICOM filename.
     * @param metaData The DICOM metaData
     * @return none
     */
    public static void convert(Path srcFilePath, Path destFilePath, Attributes metaData) throws Exception {
        Attributes fileMetaData = new Attributes();
        fileMetaData.addAll(metaData);
        try (SeekableByteChannel channel = Files.newByteChannel(srcFilePath)) {
            DicomOutputStream dos = new DicomOutputStream(destFilePath.toFile());
            XPEGParser parser = new JPEGParser(channel);
            parser.getAttributes(fileMetaData);
            dos.writeDataset(fileMetaData.createFileMetaInformation(parser.getTransferSyntaxUID()), fileMetaData);
            dos.writeHeader(Tag.PixelData, VR.OB, -1);
            dos.writeHeader(Tag.Item, null, 0);
            copyPixelData(channel, parser.getCodeStreamPosition(), dos);
            dos.writeHeader(Tag.SequenceDelimitationItem, null, 0);
            dos.close();
        }
    }

    private static void copyPixelData(SeekableByteChannel channel, long position, DicomOutputStream dos, byte... prefix) throws IOException {
        long codeStreamSize = channel.size() - position + prefix.length;
        dos.writeHeader(Tag.Item, null, (int) ((codeStreamSize + 1) & ~1));
        dos.write(prefix);
        channel.position(position);
        copy(channel, dos);
        if ((codeStreamSize & 1) != 0) {
            dos.write(0);
        }
    }

    private static void copy(ByteChannel in, OutputStream out) throws IOException {
        ByteBuffer bb = ByteBuffer.wrap(buf);
        int read;
        while ((read = in.read(bb)) > 0) {
            out.write(buf, 0, read);
            bb.clear();
        }
    }
}

