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


    protected Object getAbstract(Object key) {
        ResultSet rs = null;
        try {
            synchronized (pstmt) {
                setInputParametersOfThePreparedSt(pstmt,key);
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
    protected abstract void setInputParametersOfThePreparedSt(PreparedStatement pstmt,Object key) throws SQLException;
}
