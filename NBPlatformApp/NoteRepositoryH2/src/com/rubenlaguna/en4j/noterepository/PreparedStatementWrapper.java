/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rubenlaguna.en4j.noterepository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.openide.util.Exceptions;

/**
 *
 * @author Ruben Laguna <ruben.laguna@gmail.com>
 */
public abstract class PreparedStatementWrapper<T,E> {

    final PreparedStatement pstmt;

    public PreparedStatementWrapper(PreparedStatement ps) {
        this.pstmt = ps;
    }


    public E get(T key) {
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

    protected abstract E getResultFromResulSet(ResultSet rs) throws SQLException;
    protected  void setInputParametersOfThePreparedSt(PreparedStatement pstmt,T key) throws SQLException {
        if (key == null) {
            throw new IllegalArgumentException("key can't be null");
        }
        if (key instanceof Integer) {
            pstmt.setInt(1, (Integer)key);
        } else if (key instanceof String) {
            pstmt.setString(1, (String) key);
        } else {
            throw new IllegalArgumentException("only keys of type Integer or String are supported. key = "+key);
        }
    }
}
