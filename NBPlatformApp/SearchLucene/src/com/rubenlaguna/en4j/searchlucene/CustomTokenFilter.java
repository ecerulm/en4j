/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rubenlaguna.en4j.searchlucene;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;

/**
 *
 * @author ecerulm
 */
class CustomTokenFilter extends TokenFilter {

    private final List<Token> stack = new ArrayList<Token>();

    public CustomTokenFilter(TokenStream ts) {
        super(ts);
    }

    @Override
    public Token next() throws IOException {
        if (!stack.isEmpty()) {
            return stack.remove(0);
        }
        Token token = input.next();
        if (null == token) {
            return null;
        }
        if (token.term().contains(".")){
            String[] extraTokens = token.term().split("\\.");
            for (int i = 0; i < extraTokens.length; i++) {
                String extraToken = extraTokens[i];
                final Token token1 = new Token(extraToken, token.startOffset(), token.endOffset(), "EXTRATOKEN");
                token1.setPositionIncrement(0);
                stack.add(token1);

            }
        }
        return token;
    }
}
