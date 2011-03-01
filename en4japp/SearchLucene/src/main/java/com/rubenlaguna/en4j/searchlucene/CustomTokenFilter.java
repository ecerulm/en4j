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
import java.util.Stack;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.util.AttributeSource;

/**
 *
 * @author ecerulm
 */
class CustomTokenFilter extends TokenFilter {

    public static final String TOKEN_TYPE_SYNONYM = "SYNONYM";
    private Stack synonymStack;
    //private SynonymEngine engine;
    private TermAttribute termAttr;
    private AttributeSource save;

    public CustomTokenFilter(TokenStream ts) {
        super(ts);
        synonymStack = new Stack(); //#1
        termAttr = (TermAttribute) addAttribute(TermAttribute.class);
        save = ts.cloneAttributes();
    }

    @Override
    public boolean incrementToken() throws IOException {
        if (synonymStack.size() > 0) {	//#2
            State syn = (State) synonymStack.pop(); //#2
            restoreState(syn);	//#2
            return true;
        }

        if (!input.incrementToken()) {
            return false;
        }
        addAliasesToStack();
        return true;

    }

    private void addAliasesToStack() throws IOException {
        final String term = termAttr.term();
        if (!term.contains(".")) {
            return;
        }
        String[] synonyms = term.split("\\.");
        if (synonyms == null) {
            return;
        }
        State current = captureState();
        for (int i = 0;
                i < synonyms.length;
                i++) {	//#7
            save.restoreState(current);
            AnalyzerUtils.setTerm(save, synonyms[i]);	//#7
            AnalyzerUtils.setType(save, TOKEN_TYPE_SYNONYM); //#7
            AnalyzerUtils.setPositionIncrement(save, 0);	//#8
            synonymStack.push(save.captureState());	//#7

        }
    }
}
