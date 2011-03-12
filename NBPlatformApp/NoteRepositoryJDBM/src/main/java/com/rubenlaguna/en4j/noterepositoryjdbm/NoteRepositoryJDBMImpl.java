/*
 * Copyright (C) 2011 Ruben Laguna <ruben.laguna@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.rubenlaguna.en4j.noterepositoryjdbm;

import com.rubenlaguna.en4j.interfaces.NoteRepository;
import com.rubenlaguna.en4j.noteinterface.Note;
import com.rubenlaguna.en4j.noteinterface.NoteReader;
import com.rubenlaguna.en4j.noteinterface.Resource;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.ClosedByInterruptException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.btree.BTree;
import jdbm.helper.FastIterator;
import jdbm.helper.Tuple;
import jdbm.helper.TupleBrowser;
import jdbm.htree.HTree;
import org.apache.commons.collections.comparators.ComparableComparator;
import org.netbeans.api.progress.ProgressHandle;
import org.openide.util.Exceptions;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Ruben Laguna <ruben.laguna@gmail.com>
 */
@ServiceProvider(service = NoteRepository.class)
public class NoteRepositoryJDBMImpl implements NoteRepository {

    private static final String LASTNOTEID = "LASTNOTEID";
    private static final Logger LOG = Logger.getLogger(NoteRepositoryJDBMImpl.class.getName());
    private static final String DBFILE = System.getProperty("netbeans.user") + "/jdbmdb";
    private static final String NOTESBYID = "byId";
    private static final String NOTESBYGUID = "byGuid";
    private static final String RESBYPARENTGUIDPLUSHASH = "resByGuidPlusHash";
    private static final String RESBYGUID = "resByGuid";
    private final RecordManager recman;
    private final BTree notesById;
    private final HTree notesByGuid;
    private final HTree resByGuid;
    private final HTree resByPGuidAndHash;
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    public NoteRepositoryJDBMImpl() throws IOException {
        Properties props = new Properties();
        recman = RecordManagerFactory.createRecordManager(DBFILE, props);

        long recid = recman.getNamedObject(NOTESBYID);
        if (recid != 0) {
            notesById = BTree.load(recman, recid);
        } else {
            notesById = BTree.createInstance(recman, new ComparableComparator());
            recman.setNamedObject(NOTESBYID, notesById.getRecid());
        }

        recid = recman.getNamedObject(NOTESBYGUID);
        if (recid != 0) {
            notesByGuid = HTree.load(recman, recid);
        } else {
            notesByGuid = HTree.createInstance(recman);
            recman.setNamedObject(NOTESBYGUID, notesByGuid.getRecid());
        }

        recid = recman.getNamedObject(RESBYPARENTGUIDPLUSHASH);
        if (recid != 0) {
            resByPGuidAndHash = HTree.load(recman, recid);
        } else {
            resByPGuidAndHash = HTree.createInstance(recman);
            recman.setNamedObject(RESBYPARENTGUIDPLUSHASH, resByPGuidAndHash.getRecid());
        }

        recid = recman.getNamedObject(RESBYGUID);
        if (recid != 0) {
            resByGuid = HTree.load(recman, recid);
        } else {
            resByGuid = HTree.createInstance(recman);
            recman.setNamedObject(RESBYGUID, resByGuid.getRecid());
        }

    }

    @Override
    public Collection<Note> getAllNotes() {
        long start = System.currentTimeMillis();
        Collection<Note> toReturn = new ArrayList<Note>();
        try {
            TupleBrowser browse = notesById.browse();
            Tuple tuple = new Tuple();

            while (browse.getNext(tuple)) {
                toReturn.add((Note) tuple.getValue());
            }
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "cannot iterate over notes", ex);
        }
        Installer.mbean.sampleGetAllNotes(System.currentTimeMillis() - start);
        return toReturn;
    }

    @Override
    public void importEntries(InputStream in, ProgressHandle ph) throws InterruptedException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Note get(int id) {
        try {
            long start = System.currentTimeMillis();
            final Note note = (Note) notesById.find(Integer.valueOf(id));
            long delta = System.currentTimeMillis() - start;
            Installer.mbean.sampleGetById(delta);
            return note;
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "caught exception", ex);
        }
        return null;
    }

    @Override
    public Note get(int id, boolean withContents) {
        return get(id);
    }

    @Override
    public Note getByGuid(String guid, boolean withContents) {
        try {

            long start = System.currentTimeMillis();
            Integer id = (Integer) notesByGuid.get(guid);
            if (id == null) {
                return null;
            }
            final Note note = (Note) notesById.find(id);
            long delta = System.currentTimeMillis() - start;
            Installer.mbean.sampleGetById(delta);
            return note;
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "caught exception", ex);
        }
        return null;
    }

    @Override
    public Resource getResource(String parentNoteGuid, String hash) {
        if (parentNoteGuid == null) {
            throw new IllegalArgumentException("guid can't be null");
        }
        if (hash == null) {
            throw new IllegalArgumentException("hash can't be null");
        }
        if (hash.length() != 32) {
            throw new IllegalArgumentException("hashes are 32 bytes long. This was " + hash + " (" + hash.length() + ").");
        }
        if (parentNoteGuid.length() != 36) {
            throw new IllegalArgumentException("GUIDs are 36 bytes long. This was " + parentNoteGuid);
        }

        try {
            return (Resource) resByPGuidAndHash.get(parentNoteGuid + hash);
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "caught exception", ex);
        }
        return null;
    }

    @Override
    public boolean isNoteUpToDate(String guid, int usn) {
        final Note note = getByGuid(guid, true);
        if (null == note) {
            return false;
        }
        return note.getUpdateSequenceNumber() >= usn;
    }

    @Override
    public boolean isResourceUpToDate(String guid, int usn) {
        final Resource res = getResourceByGuid(guid);
        if (null == res) {
            return false;
        }
        return res.getUpdateSequenceNumber() >= usn;
    }

    @Override
    public boolean add(NoteReader note) {
        try {
            if (null == note) {
                return false;
            }
            if (null == note.getGuid()) {
                LOG.log(Level.WARNING, "Refuse to store a corrupted note without guid into the db: ({0})", note.getTitle());
            }
            //first iterate over resources and
            for (Resource resource : note.getResources()) {
                if (!insertResource(resource)) {
                    return false;
                }
            }
            // and then add the note
            if (!insertNote(note)) {
                return false;
            }

            this.pcs.firePropertyChange("notes", null, null);
            recman.commit();
            return true;
        } catch (IOException ex) {
            LOG.log(Level.WARNING, "caught exception:", ex);
        } 
        return false;

    }

    @Override
    public boolean deleteNoteByGuid(String noteguid) {
        try {
            Integer noteid = (Integer) notesByGuid.get(noteguid);
            if (null == noteid) {
                return false;
            }
            notesById.remove(noteid);
            notesByGuid.remove(noteguid);
            recman.commit();
            return true;
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "caught exception:", ex);
            return false;
        }
    }

    @Override
    public boolean add(Resource r) {
        try {
            boolean toReturn = insertResource(r);
            this.pcs.firePropertyChange("notes", null, null);
            recman.commit();
            return toReturn;
        } catch (IOException ex) {
            LOG.log(Level.WARNING, "caught exception:", ex);
            return false;
        }
    }

    @Override
    public int size() {
        return notesById.size();
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        this.pcs.addPropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        this.pcs.removePropertyChangeListener(listener);
    }

    private Resource getResourceByGuid(String guid) {
        if (guid == null) {
            throw new IllegalArgumentException("guid can't be null");
        }
        if (guid.length() != 36) {
            throw new IllegalArgumentException("GUIDs are 36 bytes long. This was " + guid);
        }

        try {
            String guid_and_hash = (String) resByGuid.get(guid);
            if (null == guid_and_hash) {
                return null;
            }
            return (Resource) resByPGuidAndHash.get(guid_and_hash);
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "caught exception", ex);
        }
        return null;
    }

    private boolean insertResource(Resource resource) throws IOException {
        if (null == resource) {
            return false;
        }
        ResourceImpl impl = new ResourceImpl(resource);
        final String guid_and_hash = resource.getNoteguid() + resource.getDataHash();
        resByPGuidAndHash.put(guid_and_hash, impl);
        final String guid = resource.getGuid();
        if (null != guid) {
            resByGuid.put(guid, guid_and_hash);
        }
        return true;
    }

    private boolean insertNote(NoteReader note) throws IOException {
        if (null == note) {
            return false;
        }
        long start = System.currentTimeMillis();
        long recid = recman.getNamedObject(LASTNOTEID);
        if (recid == 0) {
            recid = recman.insert(Integer.valueOf(0));
            recman.setNamedObject(LASTNOTEID, recid);
        }
        Integer noteid = (Integer) recman.fetch(recid);
        noteid++;
        NoteImpl n = new NoteImpl(note, noteid);
        notesById.insert(noteid, n, false);
        notesByGuid.put(n.getGuid(), noteid);
        recman.update(recid, noteid);
        Installer.mbean.sampleInsertNote(System.currentTimeMillis() - start);
        return true;
    }

    void close() {
        try {
            recman.commit();
            recman.close();
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "caught exception", ex);
        }
    }
}
