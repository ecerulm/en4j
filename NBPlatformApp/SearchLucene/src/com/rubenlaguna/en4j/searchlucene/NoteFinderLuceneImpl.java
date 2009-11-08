/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rubenlaguna.en4j.searchlucene;

import com.rubenlaguna.en4j.interfaces.NoteFinder;
import com.rubenlaguna.en4j.interfaces.NoteRepository;
import com.rubenlaguna.en4j.jpaentities.Notes;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;

/**
 *
 * @author ecerulm
 */
public class NoteFinderLuceneImpl implements NoteFinder {

    public Collection<Notes> find(String searchText) {
        Collection<Notes> toReturn = new ArrayList<Notes>();

        try {
            File file = new File(System.getProperty("netbeans.user") + "en4j/luceneindex");
            Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_CURRENT);
            IndexReader reader = IndexReader.open(FSDirectory.open(file), true);

            IndexSearcher searcher = new IndexSearcher(reader);

            QueryParser parser = new QueryParser("title", analyzer);
            Query query = parser.parse(searchText);
            //search the query

            TopDocs topdocs = searcher.search(query, null, 5);
            for (ScoreDoc sd : topdocs.scoreDocs) {
                int scoreId = sd.doc;
                Document document = searcher.doc(scoreId);
                int docId = Integer.parseInt(document.getField("id").stringValue());

                NoteRepository rep = Lookup.getDefault().lookup(NoteRepository.class);
                toReturn.add(rep.get(docId));
            }


        } catch (ParseException ex) {
            Exceptions.printStackTrace(ex);
        } catch (CorruptIndexException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        return toReturn;
    }
}
