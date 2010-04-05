/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rubenlaguna.en4j.noterepository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.modules.ModuleInstall;
import org.openide.util.Exceptions;

/**
 * Manages a module's lifecycle. Remember that an installer is optional and
 * often not needed at all.
 */
public class Installer extends ModuleInstall {

    private static final Logger LOG = Logger.getLogger(Installer.class.getName());
    public static Connection c = null;

    @Override
    public void restored() {
        try {
            Class.forName("org.h2.Driver").newInstance();
            String connectionURL = "jdbc:h2:" + System.getProperty("netbeans.user") + "/en4jh2db/db";
            c = DriverManager.getConnection(connectionURL);
            c.setAutoCommit(true);
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
        try {
            c.createStatement().execute("SET CACHE_SIZE 1024");
            c.createStatement().execute("SET MAX_LOG_SIZE 0");
            c.createStatement().execute("SET MAX_MEMORY_ROWS 0");
            c.createStatement().execute("SET MAX_MEMORY_UNDO 0");
            c.createStatement().execute("SET MAX_OPERATION_MEMORY 0");
            c.createStatement().execute("SET UNDO_LOG 0");
            ResultSet rs = c.getMetaData().getTables(null, null, "%", null);
            boolean notesTableAlreadyExists = false;
//            boolean resourcesTableAlreadyExists = false;

            while (rs.next()) {
                if ("NOTES".equalsIgnoreCase(rs.getString("TABLE_NAME"))) {
                    LOG.info("NOTES table already exists.");
                    notesTableAlreadyExists = true;
                }
            }

            if (!notesTableAlreadyExists) {
                LOG.info("CREATE NOTES table.");
                c.createStatement().execute("CREATE TABLE NOTES ("
                        + "ID IDENTITY, "
                        + "ISACTIVE BIT NOT NULL, "
                        + "GUID CHAR(36) NOT NULL,"
                        + "SERIALIZEDOBJECT OTHER, "
                        + "CONTENT CLOB(5242880) NOT NULL,"
                        //                        + "PRIMARY KEY (ID), "
                        + "CONSTRAINT UNQ_GUID UNIQUE (GUID))");
                LOG.info("CREATE RESOURCES table.");

                c.createStatement().execute("CREATE TABLE RESOURCES ("
                        + "GUID CHAR(36) NOT NULL,"
                        + "SERIALIZEDOBJECT OTHER, "
                        + "OWNERGUID CHAR(36), "
                        + "HASH CHAR(32), "
                        + "DATA BLOB, "
                        + "CONSTRAINT UNQ_GUID_RES UNIQUE (GUID))");
                c.createStatement().execute("CREATE INDEX I_RSOURCS_OWNER ON RESOURCES (OWNERGUID)");
            } else {
                LOG.info("NOTES table is already there no need to execute CREATE statement");
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "exception ", e);
        }
    }

    @Override
    public void close() {
        try {
            c.createStatement().execute("SHUTDOWN");
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "exception caught while doing db shutdown", e);
        }
    }
}
