/*
 *  Copyright (C) 2010 Ruben Laguna <ruben.laguna@gmail.com>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.rubenlaguna.en4j.jpaentities;

import java.io.Serializable;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;

/**
 * Resource.
 * @see http://www.evernote.com/about/developer/api/ref/Types.html#Struct_Resource
 * @author ecerulm
 */
@Entity
@Table(name = "RESOURCES")
public class Resource implements Serializable {

    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;
    @Basic
    @Column(length = 32)
    private String hashValue;
    @Basic
    @Column(name = "dataBs", length = 26214400)
    private byte[] dataBs;
    @ManyToOne(optional = false)
    @JoinColumn(nullable = false)
    private Notes owner;
    @Version
    private long version;
    @Basic(optional = true)
    @Column(name = "GUID", length = 36, unique = true, nullable = true)
    private String guid;
    @Basic(optional = true)
    @Column(name = "NOTEGUID", length = 36, unique = false, nullable = true)
    private String noteguid;
    @Basic(optional = true)
    @Column(name = "MIME", length = 355, unique = false, nullable = true)
    private String mime;
    @Basic(optional = true)
    @Column(name = "recognitionBs", length = 52428800, nullable = true)
    private byte[] recognitionBs;
    @Basic(optional = true)
    @Column(name = "recognitionHash", length = 16, nullable = true)
    private String recognitionHash;
    @Basic(optional = true)
    @Column(length = 52428800, nullable = true)
    private byte[] alternateDataBs;
    @Column(length = 16, nullable = true)
    @Basic(optional = true)
    private String alternateDataHash;
    @Basic(optional = true)
    @Temporal(TemporalType.TIMESTAMP)
    private Date restimestamp;
    @Basic(optional = true)
    private double longitude;
    @Basic(optional = true)
    private double latitude;
    @Basic(optional = true)
    private double altitude;
    @Column(length = 4096, unique = false, nullable = true)
    private String cameraMake;
    @Column(length = 4096, unique = false, nullable = true)
    private String cameraModel;
    @Basic(optional = true)
    private boolean clientWillIndex;
    @Column(length = 4096, unique = false, nullable = true)
    private String filename;
    @Basic(optional = true)
    private boolean premiumAttachment;


    /*
    guid
    noteGuid
    data
    mime
    recognition
    alternateData
    sourceURL
    timestamp\
    latitude
    longitude
    altitude
    cameraMake
    cameraModel
    clientWillIndex
    fileName
    attachment
     */
    public String getHash() {
        if (null == hashValue) {
            hashValue = generateHash(getData());
        }
        return hashValue;
    }

    private String generateHash(byte[] data) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            BigInteger hash = new BigInteger(1, md5.digest(data));
            String hashword = hash.toString(16);
            return hashword;
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void setData(byte[] data) {
        this.dataBs = data;
    }

    public byte[] getData() {
        return this.dataBs;
    }

    public void setOwner(Notes note) {
        this.owner = note;
    }

    public Notes getOwner() {
        return this.owner;
    }

    public byte[] getAlternateData() {
        return alternateDataBs;
    }

    public void setAlternateData(byte[] alternateDataBs) {
        this.alternateDataBs = alternateDataBs;
    }

    public String getAlternateDataHash() {
        if(null==alternateDataHash){
            alternateDataHash=generateHash(getAlternateData());
        }
        return alternateDataHash;
    }

    public String getRecognitionHash() {
        if(null==recognitionHash){
            recognitionHash=generateHash(getRecognition());
        }
        return recognitionHash;
    }

    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    public String getCameraMake() {
        return cameraMake;
    }

    public void setCameraMake(String cameraMake) {
        this.cameraMake = cameraMake;
    }

    public String getCameraModel() {
        return cameraModel;
    }

    public void setCameraModel(String cameraModel) {
        this.cameraModel = cameraModel;
    }

    public boolean isClientWillIndex() {
        return clientWillIndex;
    }

    public void setClientWillIndex(boolean clientWillIndex) {
        this.clientWillIndex = clientWillIndex;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getMime() {
        return mime;
    }

    public void setMime(String mime) {
        this.mime = mime;
    }

    public String getNoteguid() {
        return noteguid;
    }

    public void setNoteguid(String noteguid) {
        this.noteguid = noteguid;
    }

    public boolean isPremiumAttachment() {
        return premiumAttachment;
    }

    public void setPremiumAttachment(boolean premiumAttachment) {
        this.premiumAttachment = premiumAttachment;
    }

    public byte[] getRecognition() {
        return recognitionBs;
    }

    public void setRecognition(byte[] recognitionBs) {
        this.recognitionBs = recognitionBs;
    }

    public Date getTimestamp() {
        return restimestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.restimestamp = timestamp;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }
}
