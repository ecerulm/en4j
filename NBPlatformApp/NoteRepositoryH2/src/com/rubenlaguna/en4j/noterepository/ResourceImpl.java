/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rubenlaguna.en4j.noterepository;

import com.rubenlaguna.en4j.noteinterface.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.util.Exceptions;

/**
 *
 * @author Ruben Laguna <ruben.laguna@gmail.com>
 */
class ResourceImpl implements Resource, Serializable {

    private final String guid;

    public ResourceImpl(String guid) {
        this.guid = guid;
    }

    public byte[] getAlternateData() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public double getAltitude() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getCameraMake() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getCameraModel() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public byte[] getData() {
        getLogger().info("resource guid:" + guid);
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = getConnection().prepareStatement("SELECT DATA FROM RESOURCES WHERE GUID=?");
            pstmt.setString(1, guid);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                final Blob blob = rs.getBlob("DATA");
                final InputStream is = blob.getBinaryStream();
                // Create the byte array to hold the data
                final byte[] toReturn = new byte[(int) blob.length()];

                // Read in the bytes
                int offset = 0;
                int numRead = 0;
                try {
                    while (offset < toReturn.length
                            && (numRead = is.read(toReturn, offset, toReturn.length - offset)) >= 0) {
                        offset += numRead;
                    }

                    // Ensure all the bytes have been read in
                    if (offset < toReturn.length) {
                        getLogger().warning("could not completely read data for resource guid:" + guid);
                        //throw new IOException("Could not completely read file " + file.getName());
                    }
                } catch (IOException e) {
                    getLogger().log(Level.WARNING, "caught exception:", e);
                } finally {
                    // Close the input stream and return bytes
                    try {
                        is.close();
                    } catch (IOException e) {
                    }

                }
                return toReturn;
            }

        } catch (SQLException sQLException) {
            Exceptions.printStackTrace(sQLException);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                }

            }
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (SQLException e) {
                }
            }
        }

        return null;
    }

    public InputStream getDataAsInputStream() {
        getLogger().info("resource guid:" + guid);
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = getConnection().prepareStatement("SELECT DATA FROM RESOURCES WHERE GUID=?");
            pstmt.setString(1, guid);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                final Blob blob = rs.getBlob("DATA");
                final InputStream is = blob.getBinaryStream();
                return is;
            }

        } catch (SQLException sQLException) {
            Exceptions.printStackTrace(sQLException);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                }

            }
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (SQLException e) {
                }
            }
        }

        return null;
    }

    public String getDataHash() {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = getConnection().prepareStatement("SELECT HASH FROM RESOURCES WHERE GUID=?");
            pstmt.setString(1, guid);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                final String hash = rs.getString("HASH");
                return hash;
            }

        } catch (SQLException sQLException) {
            Exceptions.printStackTrace(sQLException);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                }

            }
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (SQLException e) {
                }
            }
        }

        return "";
    }

    public String getAlternateDataHash() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getFilename() {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = getConnection().prepareStatement("SELECT FILENAME FROM RESOURCES WHERE GUID=?");
            pstmt.setString(1, guid);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                final String hash = rs.getString("FILENAME");
                return hash;
            }

        } catch (SQLException sQLException) {
            Exceptions.printStackTrace(sQLException);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                }

            }
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (SQLException e) {
                }
            }
        }

        return "";
    }

    public String getGuid() {
        return guid;
    }

    public double getLatitude() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public double getLongitude() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getMime() {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = getConnection().prepareStatement("SELECT MIME FROM RESOURCES WHERE GUID=?");
            pstmt.setString(1, guid);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                final String hash = rs.getString("MIME");
                return hash;
            }

        } catch (SQLException sQLException) {
            Exceptions.printStackTrace(sQLException);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                }

            }
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (SQLException e) {
                }
            }
        }

        return "";

    }

    public String getNoteguid() {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = getConnection().prepareStatement("SELECT OWNERGUID FROM RESOURCES WHERE GUID=?");
            pstmt.setString(1, guid);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                final String hash = rs.getString("OWNERGUID");
                return hash;
            }

        } catch (SQLException sQLException) {
            Exceptions.printStackTrace(sQLException);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                }

            }
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (SQLException e) {
                }
            }
        }

        return "";

    }

    public boolean getPremiumAttachment() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public byte[] getRecognition() {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = getConnection().prepareStatement("SELECT RECOGNITION FROM RESOURCES WHERE GUID=?");
            pstmt.setString(1, guid);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                final byte[] toReturn = rs.getBytes("RECOGNITION");
                return toReturn;
            }

        } catch (SQLException sQLException) {
            Exceptions.printStackTrace(sQLException);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                }

            }
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (SQLException e) {
                }
            }
        }

        return null;
    }

    public Date getTimestamp() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private Connection getConnection() {
        return Installer.c;
    }

    private Logger getLogger() {
        return Logger.getLogger(ResourceImpl.class.getName());
    }
}
