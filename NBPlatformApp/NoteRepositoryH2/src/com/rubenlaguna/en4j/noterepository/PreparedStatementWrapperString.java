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
abstract class PreparedStatementWrapperString extends PreparedStatementWrapper {

    PreparedStatementWrapperString(PreparedStatement pstmt) {
        super(pstmt);
    }

    @Override
    protected void setInputParametersOfThePreparedSt(PreparedStatement pstmt,Object key) throws SQLException {
        pstmt.setString(1, (String)key);
    }

    public Object get(String id) {
        return getAbstract(id);
    }
}
