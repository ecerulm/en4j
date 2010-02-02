/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.rubenlaguna.en4j.searchlucene;

import java.io.IOException;
import java.io.Reader;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.util.Version;

/**
 *
 * @author ecerulm
 */
class CustomAnalyzer extends Analyzer{

    public CustomAnalyzer() {
    }

    @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
        final TokenStream ts = new StandardAnalyzer(Version.LUCENE_CURRENT).tokenStream(fieldName, reader);

        return new CustomTokenFilter(ts);
        //throw new UnsupportedOperationException("Not supported yet.");
    }

}
