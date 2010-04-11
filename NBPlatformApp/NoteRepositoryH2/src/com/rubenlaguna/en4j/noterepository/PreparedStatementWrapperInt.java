/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.rubenlaguna.en4j.noterepository;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 *
 * @author Ruben Laguna <ruben.laguna@gmail.com>
 */
abstract class PreparedStatementWrapperInt extends PreparedStatementWrapper {

    PreparedStatementWrapperInt(PreparedStatement pstmt) {
        super(pstmt);
    }

    @Override
    protected void setInputParametersOfThePreparedSt(PreparedStatement pstmt,Object key) throws SQLException {
        pstmt.setInt(1, (Integer)key);
    }

    public Object get(int id) {
        return getAbstract(id);
    }
}
