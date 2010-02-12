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
import java.util.ArrayList;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;

/**
 *
 * @author ecerulm
 */
public class AnalyzerUtils {

    public static Token[] tokensFromAnalysis(Analyzer analyzer, String text) throws IOException {
        TokenStream stream = analyzer.tokenStream("contents", new StringReader(text));
        ArrayList tokenList = new ArrayList();
        while (true) {
            Token token = stream.next();
            if (token == null) {
                break;
            }
            tokenList.add(token);
        }
        return (Token[]) tokenList.toArray(new Token[0]);

    }

    public static void displayTokens(Analyzer analyzer, String text) throws IOException {
        Token[] tokens = tokensFromAnalysis(analyzer, text);
        for (int i = 0; i < tokens.length; i++) {
            Token token = tokens[i];
            System.out.print("[" + token.termText() + "] ");
        }
    }

    public static void displayTokensWithFullDetails(Analyzer analyzer, String text) throws IOException {
        Token[] tokens = tokensFromAnalysis(analyzer, text);
        int position = 0;
        for (int i = 0; i < tokens.length; i++) {
            Token token = tokens[i];
            int increment = token.getPositionIncrement();
            if (increment > 0) {
                position = position + increment;
                System.out.println();
                System.out.print(position + ": ");
            }
            System.out.print("[" + token.termText() + ":" + token.startOffset() + "->" + token.endOffset() + ":" + token.type() + "] ");
        }
    }
}
