/*
 * Copyright (C) 2011 Ruben Laguna <ruben.laguna@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.rubenlaguna.en4j.noterepositoryjdbm;

import com.rubenlaguna.en4j.noteinterface.Resource;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Ruben Laguna <ruben.laguna@gmail.com>
 */
final class ResourceImpl implements Resource, Serializable {

    static final long serialVersionUID = 1L;
    private byte[] data;
    private byte[] alternateData;
    private static final Logger LOG = Logger.getLogger(ResourceImpl.class.getName());
    private String hashword;
    private String altDataHashword;
    private double altitude;
    private String cameraMake;
    private String cameraModel;
    private String filename;
    private String guid;
    private double lat;
    private String noteguid;
    private double lon;
    private boolean premiumAtt;
    private byte[] recog;
    private Date tstamp;
    private String mime;
    private int usn;

    ResourceImpl() { // Serializable needs a no-arguments constructor
    }

    ResourceImpl(Resource resource) throws IOException {
        data = getData(resource.getDataAsInputStream());
        alternateData = resource.getAlternateData();
        altitude = resource.getAltitude();
        cameraMake = resource.getCameraMake();
        cameraModel = resource.getCameraModel();
        filename = resource.getFilename();
        guid = resource.getGuid();
        lat = resource.getLatitude();
        lon = resource.getLongitude();
        mime = resource.getMime();
        noteguid = resource.getNoteguid();
        premiumAtt = resource.getPremiumAttachment();
        recog = resource.getRecognition();
        tstamp = resource.getTimestamp();
        usn = resource.getUpdateSequenceNumber();

    }

    @Override
    public InputStream getDataAsInputStream() {
        return new ByteArrayInputStream(Arrays.copyOf(data, data.length));
    }

    @Override
    public String getDataHash() {
        if (null == hashword) {
            hashword = generateHash(data);
        }
        return hashword;
    }

    @Override
    public byte[] getAlternateData() {
        return Arrays.copyOf(alternateData, alternateData.length);
    }

    @Override
    public String getAlternateDataHash() {
        if (null == altDataHashword) {
            altDataHashword = generateHash(alternateData);
        }
        return altDataHashword;
    }

    @Override
    public double getAltitude() {
        return altitude;
    }

    @Override
    public double getLatitude() {
        return lat;
    }

    @Override
    public double getLongitude() {
        return lon;
    }

    @Override
    public boolean getPremiumAttachment() {
        return premiumAtt;
    }

    @Override
    public String getCameraMake() {
        return cameraMake;
    }

    @Override
    public String getCameraModel() {
        return cameraModel;
    }

    @Override
    public String getFilename() {
        return filename;
    }

    @Override
    public String getGuid() {
        return guid;
    }

    @Override
    public String getMime() {
        return mime;
    }

    @Override
    public String getNoteguid() {
        return noteguid;
    }

    @Override
    public byte[] getRecognition() {
        return recog;
    }

    @Override
    public Date getTimestamp() {
        return (Date) tstamp.clone();
    }

    @Override
    public int getUpdateSequenceNumber() {
        return usn;
    }

    @Override
    public int getDataLength() {
        return data.length;
    }

    private byte[] getData(InputStream inputStream) throws IOException {
        BufferedInputStream is = new BufferedInputStream(inputStream);
        ReadableByteChannel isc = Channels.newChannel(is);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        WritableByteChannel osc = Channels.newChannel(os);
        ByteBuffer bb = ByteBuffer.allocate(32000);
        while (isc.read(bb) != -1) {
            bb.flip();
            osc.write(bb);
            bb.clear();
        }
        return os.toByteArray();
    }

    private String generateHash(byte[] data) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            BigInteger hash = new BigInteger(1, md5.digest(data));
            String hashword = String.format("%032x", hash);

            LOG.info("generated hash = " + hashword);
            if (hashword.length() != 32) {
                throw new RuntimeException("generated incorrect hash " + hashword + " length:" + hashword.length());
            }
            return hashword;
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    }
}
