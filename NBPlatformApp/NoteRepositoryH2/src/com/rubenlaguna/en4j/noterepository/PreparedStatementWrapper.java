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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.util.Exceptions;

/**
 *
 * @author Ruben Laguna <ruben.laguna@gmail.com>
 */
public abstract class PreparedStatementWrapper<T, E> {

    private static final Logger LOG = Logger.getLogger(PreparedStatementWrapper.class.getName());
    final PreparedStatement pstmt;

    public PreparedStatementWrapper(PreparedStatement ps) {
        this.pstmt = ps;
    }

    public E get(T key) {
        ResultSet rs = null;
        try {
            synchronized (pstmt) {
                setInputParametersOfThePreparedSt(pstmt, key);
                rs = pstmt.executeQuery();
                if (rs.next()) {
                    return getResultFromResulSet(rs);
                }
            }
        } catch (Exception sQLException) {
            Exceptions.printStackTrace(sQLException);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    LOG.log(Level.WARNING, "exception while trying to close ResultSet", e);
                    ;
                }
            }
        }
        return null;
    }

    public void close() {
        if (pstmt != null) {
            try {
                pstmt.close();
                LOG.log(Level.OFF, "PreparedStatement {0} closed", pstmt);
            } catch (SQLException e) {
                LOG.log(Level.WARNING, "exception caught while trying to close PreparedStatement", e);
            }
        }
    }

    protected abstract E getResultFromResulSet(ResultSet rs) throws SQLException;

    protected void setInputParametersOfThePreparedSt(PreparedStatement pstmt, T key) throws SQLException {
        if (key == null) {
            throw new IllegalArgumentException("key can't be null");
        }
        if (key instanceof Integer) {
            pstmt.setInt(1, (Integer) key);
        } else if (key instanceof String) {
            pstmt.setString(1, (String) key);
        } else {
            throw new IllegalArgumentException("only keys of type Integer or String are supported. key = " + key);
        }
    }
}
