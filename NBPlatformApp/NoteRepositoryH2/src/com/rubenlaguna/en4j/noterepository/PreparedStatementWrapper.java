/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rubenlaguna.en4j.noterepository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.Callable;
import org.openide.util.Exceptions;

/**
 *
 * @author Ruben Laguna <ruben.laguna@gmail.com>
 */
public abstract class PreparedStatementWrapper {

    final PreparedStatement pstmt;

    public PreparedStatementWrapper(PreparedStatement ps) {
        this.pstmt = ps;
    }

    public Object get(final int id) {
        Callable<Void> callable = new Callable<Void>() {
            public Void call() throws Exception {
                pstmt.setInt(1, id);
                return null;
            }
        };
        return getAbstract(callable);
    }
    public Object get(final String id) {
        Callable<Void> callable = new Callable<Void>() {
            public Void call() throws Exception {
                pstmt.setString(1, id);
                return null;
            }
        };
        return getAbstract(callable);
    }

    private Object getAbstract(Callable<Void> callable) {
        ResultSet rs = null;
        try {
            synchronized (pstmt) {
                callable.call();
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
                }
            }
        }
        return null;

    }

    public void close() {
        if (pstmt != null) {
            try {
                pstmt.close();
            } catch (SQLException e) {
            }
        }
    }

    protected abstract Object getResultFromResulSet(ResultSet rs) throws SQLException;
}
