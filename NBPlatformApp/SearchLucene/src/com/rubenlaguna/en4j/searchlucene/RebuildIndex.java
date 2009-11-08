/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rubenlaguna.en4j.searchlucene;

import com.rubenlaguna.en4j.interfaces.NoteRepository;

import com.rubenlaguna.en4j.noteinterface.Note;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.util.Version;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;

public final class RebuildIndex implements ActionListener {

    public void actionPerformed(ActionEvent e) {

        //TODO use an Executor/ RequestProcessor here

        IndexWriter writer = null;
        try {
            // TODO implement action body
            File file = new File(System.getProperty("netbeans.user") + "en4j/luceneindex");
            Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_CURRENT);
            writer = new IndexWriter(FSDirectory.open(file),
                    analyzer, true,
                    IndexWriter.MaxFieldLength.LIMITED);
            writer.setUseCompoundFile(true);
            writer.deleteAll();
            writer.commit();
            Collection<Note> notes = getAllNotes();

            for (Note note : notes) {
                //Lucene document http://www.darksleep.com/lucene/
                Document document = new Document();

                Field idField = new Field("id",
                        note.getId().toString(),
                        Field.Store.YES,
                        Field.Index.NOT_ANALYZED);
                document.add(idField);

                Field titleField = new Field("title",
                        note.getTitle(),
                        Field.Store.YES,
                        Field.Index.ANALYZED);
                document.add(titleField);
                writer.addDocument(document);
            }
            writer.commit();
            writer.optimize();
            writer.close();


        } catch (CorruptIndexException ex) {
            Exceptions.printStackTrace(ex);
        } catch (LockObtainFailedException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (Exception ex) {
                Exceptions.printStackTrace(ex);
            }
        }


    }

    private Collection<Note> getAllNotes() {
        NoteRepository rep = Lookup.getDefault().lookup(NoteRepository.class);

        Collection<Note> toReturn = rep.getAllNotes();

        return toReturn;
        //throw new UnsupportedOperationException("Not yet implemented");
    }
}
