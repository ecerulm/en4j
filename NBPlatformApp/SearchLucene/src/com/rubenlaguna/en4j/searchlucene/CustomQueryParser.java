/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.rubenlaguna.en4j.searchlucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;

/**
 *
 * @author ecerulm
 */
class CustomQueryParser extends QueryParser{

    public CustomQueryParser(String string, Analyzer analyzer) {
    super(string,analyzer);
    }
//
//    @Override
//    protected org.apache.lucene.search.Query getFieldQuery(String field, String queryText) throws ParseException {
//        if(queryText.startsWith("\"")){
//            return super.getFieldQuery(field, queryText);
//        }
//        return getPrefixQuery(field, queryText);
//
//    }



}
