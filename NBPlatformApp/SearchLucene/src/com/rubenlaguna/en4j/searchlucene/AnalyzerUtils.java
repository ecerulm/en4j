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
package com.rubenlaguna.en4j.searchlucene;

import java.io.IOException;
import java.io.StringReader;
import java.util.logging.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.util.AttributeSource;

/**
 *
 * @author ecerulm
 */
public class AnalyzerUtils {

    private static final Logger LOG = Logger.getLogger(AnalyzerUtils.class.getName());

//    public static Token[] tokensFromAnalysis(Analyzer analyzer, String text) throws IOException {
//        TokenStream stream = analyzer.tokenStream("contents", new StringReader(text));
//        ArrayList tokenList = new ArrayList();
//        while (true) {
//            Token token = stream.next();
//            if (token == null) {
//                break;
//            }
//            tokenList.add(token);
//        }
//        return (Token[]) tokenList.toArray(new Token[0]);
//
//    }
    public static void displayTokens(Analyzer analyzer, String text) throws IOException {
        displayTokens(analyzer.tokenStream("contents", new StringReader(text))); //A
    }

    public static void displayTokens(TokenStream stream) throws IOException {
        TermAttribute term = (TermAttribute) stream.addAttribute(TermAttribute.class);
        while (stream.incrementToken()) {
            System.out.print("[" + term.term() + "] ");	//B
        }
    }

    public static void setPositionIncrement(AttributeSource source, int posIncr) {
        PositionIncrementAttribute attr = (PositionIncrementAttribute) source.addAttribute(PositionIncrementAttribute.class);
        attr.setPositionIncrement(posIncr);
    }

    public static void setTerm(AttributeSource source, String term) {
        TermAttribute attr = (TermAttribute) source.addAttribute(TermAttribute.class);
        attr.setTermBuffer(term);
    }

    public static void setType(AttributeSource source, String type) {
        TypeAttribute attr = (TypeAttribute) source.addAttribute(TypeAttribute.class);
        attr.setType(type);
    }
}
