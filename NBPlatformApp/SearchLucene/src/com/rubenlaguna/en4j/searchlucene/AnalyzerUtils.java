/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rubenlaguna.en4j.searchlucene;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.util.AttributeSource;

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
