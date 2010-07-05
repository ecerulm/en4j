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
package com.rubenlaguna.en4j.noterepository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.modules.ModuleInstall;
import org.openide.util.Exceptions;
import java.lang.management.ManagementFactory;
import javax.management.ObjectName;
import javax.management.JMException;
import com.rubenlaguna.en4j.noterepository.NoteRepositoryH2Data;

/**
 * Manages a module's lifecycle. Remember that an installer is optional and
 * often not needed at all.
 */
public class Installer extends ModuleInstall {

    private static final Logger LOG = Logger.getLogger(Installer.class.getName());
    public static Connection c = null;
    public static final NoteRepositoryH2Data mbean = new NoteRepositoryH2Data();

    @Override
    public void restored() {
        try {
            Class.forName("org.h2.Driver").newInstance();
            String connectionURL = "jdbc:h2:" + System.getProperty("netbeans.user") + "/en4jh2db/db;DB_CLOSE_ON_EXIT=FALSE";
            c = DriverManager.getConnection(connectionURL, "", "");

            c.setAutoCommit(true);
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
        try {
            c.createStatement().execute("SET CACHE_SIZE 16384");
            c.createStatement().execute("SET MAX_LOG_SIZE 32");
            c.createStatement().execute("SET MAX_MEMORY_ROWS 10000");
            c.createStatement().execute("SET MAX_MEMORY_UNDO 50000");
            c.createStatement().execute("SET MAX_OPERATION_MEMORY 100000");
            c.createStatement().execute("SET UNDO_LOG 1");
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
//                        + "SERIALIZEDOBJECT OTHER, "
                        + "CONTENT CLOB(5242880) NOT NULL,"
                        + "TITLE VARCHAR NOT NULL,"
                        + "SOURCEURL VARCHAR,"
                        + "CREATED TIMESTAMP,"
                        + "UPDATED TIMESTAMP,"
                        + "USN INT NOT NULL,"
                        + "CONSTRAINT UNQ_GUID UNIQUE (GUID))");
                LOG.info("CREATE RESOURCES table.");

                c.createStatement().execute("CREATE TABLE RESOURCES ("
                        + "GUID CHAR(36) NOT NULL PRIMARY KEY,"
//                        + "SERIALIZEDOBJECT OTHER, "
                        + "OWNERGUID CHAR(36), "
                        + "HASH CHAR(32), "
                        + "DATA BLOB, "
                        + "FILENAME VARCHAR, "
                        + "MIME VARCHAR, "
                        + "RECOGNITION BINARY, "
                        + "USN INT NOT NULL, "
                        + "CONSTRAINT UNQ_GUID_RES UNIQUE (GUID))");
                c.createStatement().execute("CREATE INDEX I_RSOURCS_OWNER ON RESOURCES (OWNERGUID)");
                c.createStatement().execute("CREATE INDEX I_NOTES_GUID ON NOTES (GUID)");
            } else {
                LOG.info("NOTES table is already there no need to execute CREATE statement");
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "exception ", e);
        }
        try { // Register MBean in Platform MBeanServer
            ManagementFactory.getPlatformMBeanServer().
                    registerMBean(mbean,new ObjectName("com.rubenlaguna.en4j.noterepository:type=NoteRepositoryH2Data"));
        }catch(JMException ex) {
            // TODO handle exception
        }
    }

    @Override
    public void close() {
        try {
            DbPstmts.getInstance().close();
            c.createStatement().execute("SHUTDOWN");
            c.close();
            c=null;
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "exception caught while doing db shutdown", e);
        }
    }
}
