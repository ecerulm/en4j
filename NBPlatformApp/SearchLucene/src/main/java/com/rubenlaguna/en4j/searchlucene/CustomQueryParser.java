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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.util.Version;

/**
 *
 * @author ecerulm
 */
class CustomQueryParser extends QueryParser {

    public CustomQueryParser(String string, Analyzer analyzer) {
        super(Version.LUCENE_30, string, analyzer);
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
