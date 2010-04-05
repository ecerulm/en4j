/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rubenlaguna.en4j.sync;

import com.rubenlaguna.en4j.noteinterface.NoteReader;
import com.rubenlaguna.en4j.noteinterface.Resource;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;
import org.openide.util.Exceptions;
import org.primitiveworker.jgram.Calculator;
import org.primitiveworker.jgram.Generator;
import org.primitiveworker.jgram.SimpleCalculator;

/**
 *
 * @author Ruben Laguna <ruben.laguna@gmail.com>
 */
class FakeEDAM implements EDAMIf {

    private static final Logger LOG = Logger.getLogger(FakeEDAM.class.getName());
    private static final Random random = new Random();
    private static FakeEDAM theInstance = null;
    private final String location;
    private final Calculator calculator = new SimpleCalculator();
    private Generator gen = null;

    FakeEDAM(String fakeEDAM) {
        this.location = fakeEDAM;

    }

    @Override
    public Collection<NoteInfo> getSyncChunk(int highestUSN, int numnotes, boolean isFirstSync) {
        Collection<NoteInfo> toReturn = new ArrayList<NoteInfo>();

        for (int i = highestUSN + 1; (i <= highestUSN + numnotes) && (i <= getUpdateCount()); i++) {
            NoteInfo ni = new NoteInfo();
            ni.usn = i;
            ni.guid = "guid" + ni.usn;
            toReturn.add(ni);
        }
        return toReturn;
    }

    @Override
    public NoteReader getNote(final String noteGuid, boolean b, boolean b0, boolean b1, boolean b2) throws Exception {
        Thread.sleep(10 + random.nextInt(40));
        final File file = new File(location + "/0001.pdf");
        final FileInputStream fileis = new FileInputStream(file);
        final int fileLength = new Long(file.length() + 500000).intValue();
        //BufferedInputStream inFile = new BufferedInputStream(fileis);
        ByteBuffer bb = ByteBuffer.allocate(fileLength);
        final FileChannel channel = fileis.getChannel();
        int numRead = 0;
        while (numRead >= 0) {
            // Read bytes from the channel
            numRead = channel.read(bb);

        }
        // The read() method also moves the position so in order to
        // read the new bytes, the buffer's position must be set back to 0
        bb.rewind();
        final byte[] barray = new byte[fileLength];
        bb.get(barray);
        bb.clear();

        channel.close();

        final Resource res = new Resource() {

            private final byte[] data = barray;

            public byte[] getData() {
                return data;
            }

            public byte[] getAlternateData() {
                return null;
            }

            public double getAltitude() {
                return 0;
            }

            public double getLatitude() {
                return 0;
            }

            public double getLongitude() {
                return 0;
            }

            public boolean getPremiumAttachment() {
                return true;
            }

            public String getCameraMake() {
                return "";
            }

            public String getCameraModel() {
                return "";
            }

            public String getFilename() {
                return "0001.pdf";
            }

            public String getGuid() {
                return noteGuid + "res1";
            }

            public String getMime() {
                return "aplication/pdf";
            }

            public String getNoteguid() {
                return noteGuid;
            }

            public byte[] getRecognition() {
                return null;
            }

            public Date getTimestamp() {
                return new Date();
            }

            public String getDataHash() {
                return generateHash(barray);
            }

            public String getAlternateDataHash() {
                return null;
            }
        };
        final String hashword = generateHash(barray);
        NoteReader toReturn = new NoteReader() {

            public Reader getContentAsReader() {

                StringBuffer sb = new StringBuffer();
                sb.append("<?xml version=\"1.0\" encoding=\"UTF - 8\"?>");
                sb.append("<!DOCTYPE en-note SYSTEM \"http://xml.evernote.com/pub/enml.dtd\">");
                sb.append("        <en-note>");
                sb.append(noteGuid).append("\n");
                sb.append("<en-media width=\"125\" height=\"125\" hash=\"" + hashword + "\" type=\"application/pdf\" alt=\"0001.pdf\"/>").append("\n");

                try {
                    BufferedReader inFile = new BufferedReader(new FileReader(location + "/0002.txt"));
                    CharBuffer cb = CharBuffer.allocate(200000);
                    while (inFile.ready()) {
                        cb.clear();
                        inFile.read(cb);
                        cb.flip();
                        sb.append(cb).append("\n");
                    }
                    inFile.close();
                } catch (IOException iOException) {
                    Exceptions.printStackTrace(iOException);
                }
                sb.append("</en-note>");
                return new StringReader(sb.toString());
//                throw new UnsupportedOperationException("Not supported yet.");
            }

            public Date getCreated() {
                return new Date();
            }

            public String getSourceurl() {
                return "http://example.com";
            }

            public Date getUpdated() {
                return new Date();
            }

            public String getTitle() {
                return noteGuid;
            }

            public Resource getResource(String hash) {
                return res;
            }

            public Collection<Resource> getResources() {
                return Collections.singleton(res);
//                return Collections.EMPTY_LIST;
            }

            public String getGuid() {
                return noteGuid;
            }

            public Date getDeleted() {
                return new Date();
            }

            public boolean isActive() {
                return true;
            }

            public int getUpdateSequenceNumber() {
                final String substring = noteGuid.substring(4);
                return Integer.parseInt(substring);
            }
        };

        return toReturn;
    }

    private String generateHash(final byte[] barray) throws RuntimeException {
        String tempHashword = "";
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            BigInteger hash = new BigInteger(1, md5.digest(barray));
            tempHashword = hash.toString(16);
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
        return tempHashword;
    }

    @Override
    public int getUpdateCount() {
        return 10000;
    }

    @Override
    public boolean checkVersion() {
        return true;
    }

//    public static synchronized FakeEDAM getInstance() {
//        if (theInstance == null) {
//            theInstance = new FakeEDAM();
//        }
//        return theInstance;
//    }
    private void start() throws IOException {
        StringBuffer outLine = null;
        List outList = new ArrayList();
        for (int i = 0; i < 50; i++) {
            // get a sequence from the generator
            outList = gen.getSequence();
            outLine = new StringBuffer();
            // append the strings for output
            for (int j = 0; j < outList.size(); j++) {
                outLine.append(outList.get(j)).append(" ");
            }
            // print it
            System.out.print(outLine.toString());
        }
        System.out.println("");
    }
}


