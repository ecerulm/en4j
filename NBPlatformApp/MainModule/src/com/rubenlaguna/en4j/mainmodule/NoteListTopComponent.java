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
package com.rubenlaguna.en4j.mainmodule;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.swing.EventSelectionModel;
import ca.odell.glazedlists.swing.EventTableModel;
import ca.odell.glazedlists.swing.TableComparatorChooser;
import ca.odell.renderpack.DateTableCellRenderer;
import java.beans.PropertyChangeEvent;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JLayeredPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableModel;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
//import org.openide.util.ImageUtilities;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.util.Lookup;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;
import com.rubenlaguna.en4j.interfaces.NoteFinder;
import com.rubenlaguna.en4j.interfaces.NoteRepository;
import com.rubenlaguna.en4j.noteinterface.Note;
import java.beans.PropertyChangeListener;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.OverlayLayout;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableCellRenderer;
import org.openide.util.RequestProcessor;

/**
 * Top component which displays something.
 */
@ConvertAsProperties(dtd = "-//com.rubenlaguna.en4j.mainmodule//NoteList//EN",
autostore = false)
public final class NoteListTopComponent extends TopComponent implements ListSelectionListener, PropertyChangeListener {

    public static final int ALLNOTESREFRESHDELAY = 4000;
    public static final int DELAY = 1000;
    private static final Logger LOG = Logger.getLogger(NoteListTopComponent.class.getName());
    private static final RequestProcessor RP = new RequestProcessor("search tasks", 1);
    private static final RequestProcessor RPALLNOTESUPDATE = new RequestProcessor("all notes update", 1);
    private static NoteListTopComponent instance;
    /** path to the icon used by the component and its open action */
//    static final String ICON_PATH = "SET/PATH/TO/ICON/HERE";
    private static final String PREFERRED_ID = "NoteListTopComponent";
    private final InstanceContent ic = new InstanceContent();
    private RequestProcessor.Task currentSearchTask = null;
    private RequestProcessor.Task updateAllNotesTask = null;
    private RequestProcessor.Task undimTask = null;
    private final AtomicInteger dimCounter = new AtomicInteger(0);
    private String searchstring = "";
    private final CustomGlassPane customGlassPane = new CustomGlassPane();
    //empty it will be populated in componentOpened
    private EventList<Note> allNotes = GlazedLists.threadSafeList(new BasicEventList<Note>());
    private NoteMatcherEditor notesMatcher = new NoteMatcherEditor();
    private FilterList<Note> filteredList = new FilterList<Note>(allNotes, notesMatcher);
    private SortedList<Note> sortedList = new SortedList<Note>(filteredList);
    private EventSelectionModel selectionModel = null;
    private long lastPropertyChangeTimestamp = 0;

    public NoteListTopComponent() {
        LOG.log(Level.INFO, "creating NoteListTopComponen {0}", this.toString());
        initComponents();
        setName(NbBundle.getMessage(NoteListTopComponent.class, "CTL_NoteListTopComponent"));
        setToolTipText(NbBundle.getMessage(NoteListTopComponent.class, "HINT_NoteListTopComponent"));
        associateLookup(new AbstractLookup(ic));
        putClientProperty(PROP_CLOSING_DISABLED, true);
//        jTable1.getSelectionModel().addListSelectionListener(this);
        customGlassPane.setVisible(false);
        jLayeredPane1.add(customGlassPane, (Integer) (JLayeredPane.DEFAULT_LAYER + 50));
    }

    public long calculateDelay() {
        //cancel the last refresh so we only
        //two refresh task at a given moment
        long delay = lastPropertyChangeTimestamp + ALLNOTESREFRESHDELAY - System.currentTimeMillis();
        LOG.log(Level.INFO, "updateAllNotesTask should start in {0} ms", delay);
        if (delay < 0) {
            delay = 0;
        }
        if (delay > 2000) {
            delay = 2000;
        }
        return delay;
    }

    public void refresh() {
        LOG.log(Level.FINE, "refresh notelist {0}. just performSearch again", new SimpleDateFormat("h:mm:ss a").format(new Date()));
        performSearch(false);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLayeredPane1 = new javax.swing.JLayeredPane();
        jLayeredPane1.setLayout(new OverlayLayout(jLayeredPane1));
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        searchJButton = new javax.swing.JButton();
        partialResultsJLabel = new javax.swing.JLabel();

        jScrollPane1.setBounds(jLayeredPane1.getVisibleRect());

        jTable1.setModel(getGlazedListTableModel());
        jTable1.setBounds(jScrollPane1.getVisibleRect());
        jTable1.setColumnSelectionAllowed(true);
        jScrollPane1.setViewportView(jTable1);
        jTable1.getColumnModel().getSelectionModel().setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

        jScrollPane1.setBounds(0, 0, 450, -1);
        jLayeredPane1.add(jScrollPane1, javax.swing.JLayeredPane.DEFAULT_LAYER);

        searchTextField.setText(org.openide.util.NbBundle.getMessage(NoteListTopComponent.class, "NoteListTopComponent.searchTextField.text")); // NOI18N
        searchTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                searchTextFieldFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                searchTextFieldFocusLost(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(searchJButton, org.openide.util.NbBundle.getMessage(NoteListTopComponent.class, "NoteListTopComponent.searchJButton.text")); // NOI18N
        searchJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchJButtonActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(partialResultsJLabel, org.openide.util.NbBundle.getMessage(NoteListTopComponent.class, "NoteListTopComponent.partialResultsJLabel.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLayeredPane1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 469, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(searchTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 304, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(partialResultsJLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(searchJButton)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(30, 30, 30)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(searchTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(searchJButton)
                    .addComponent(partialResultsJLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(10, 10, 10)
                .addComponent(jLayeredPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 286, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void searchJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchJButtonActionPerformed
        performSearch(true);
    }//GEN-LAST:event_searchJButtonActionPerformed

    private void performSearch(final boolean useDim) {

        final RequestProcessor.Task previousSearchTask = currentSearchTask;

        if (previousSearchTask != null) {
            previousSearchTask.cancel();
        }

        Runnable r = new Runnable() {

            @Override
            public void run() {
                if (previousSearchTask != null) {
                    previousSearchTask.waitFinished();
                }
                LOG.log(Level.FINE, "{0} searchstring {1}", new Object[]{this.toString(), searchstring});

                dim(useDim);

                final String text = searchstring;
                LOG.fine("searching in lucene...");
                if (text.trim().isEmpty() || text.equals(org.openide.util.NbBundle.getMessage(NoteListTopComponent.class, "NoteListTopComponent.searchTextField.text"))) {
                    LOG.log(Level.FINE, "no need to search the search box is empty {0} from thread {1}", new Object[]{text, Thread.currentThread().getName()});
                    notesMatcher.refilter(null);
                } else {
                    NoteFinder finder = Lookup.getDefault().lookup(NoteFinder.class);
                    Collection<Note> prelList = finder.find(text);
                    LOG.log(Level.FINE, "search for {0} returned {1} results.", new Object[]{text, prelList.size()});
                    notesMatcher.refilter(prelList);
                }
                final int repSize = Lookup.getDefault().lookup(NoteRepository.class).size();
                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        final String text = filteredList.size() + "/" + repSize;
                        LOG.log(Level.FINE, "Refreshing the label in the EDT with {0}", text);
                        partialResultsJLabel.setText(text);
                    }
                });

                unDim(useDim);

            }
        };

        currentSearchTask = RP.post(r, 500);
        LOG.fine("currentSearchtask posted");
    }

    private void searchTextFieldFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_searchTextFieldFocusGained
        // TODO add your handling code here:
        String initialText = org.openide.util.NbBundle.getMessage(NoteListTopComponent.class, "NoteListTopComponent.searchTextField.text");
        if (initialText.equals(searchTextField.getText())) {
            searchTextField.setText("");
        }
    }//GEN-LAST:event_searchTextFieldFocusGained

    private void searchTextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_searchTextFieldFocusLost
        // TODO add your handling code here:
        if ("".equals(searchTextField.getText())) {
            LOG.info("searchTextField was empty so reset to the default text");
            searchTextField.setText(org.openide.util.NbBundle.getMessage(NoteListTopComponent.class, "NoteListTopComponent.searchTextField.text"));
        }
    }//GEN-LAST:event_searchTextFieldFocusLost
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLayeredPane jLayeredPane1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    private javax.swing.JLabel partialResultsJLabel;
    private javax.swing.JButton searchJButton;
    public final javax.swing.JTextField searchTextField = new javax.swing.JTextField();
    // End of variables declaration//GEN-END:variables

    /**
     * Gets default instance. Do not use directly: reserved for *.settings files only,
     * i.e. deserialization routines; otherwise you could get a non-deserialized instance.
     * To obtain the singleton instance, use {@link #findInstance}.
     */
    public static synchronized NoteListTopComponent getDefault() {
        if (instance == null) {
            instance = new NoteListTopComponent();
        }

        return instance;
    }

    /**
     * Obtain the NoteListTopComponent instance. Never call {@link #getDefault} directly!
     */
    public static synchronized NoteListTopComponent findInstance() {
        TopComponent win = WindowManager.getDefault().findTopComponent(PREFERRED_ID);
        if (win == null) {
            Logger.getLogger(NoteListTopComponent.class.getName()).warning(
                    "Cannot find " + PREFERRED_ID + " component. It will not be located properly in the window system.");
            return getDefault();
        }
        if (win instanceof NoteListTopComponent) {
            return (NoteListTopComponent) win;
        }
        Logger.getLogger(NoteListTopComponent.class.getName()).warning(
                "There seem to be multiple components with the '" + PREFERRED_ID
                + "' ID. That is a potential source of errors and unexpected behavior.");
        return getDefault();
    }

    @Override
    public int getPersistenceType() {
        return TopComponent.PERSISTENCE_NEVER;
    }

    @Override
    public void componentOpened() {
        allNotes.addAll(getAllNotesInDb());
        selectionModel = new EventSelectionModel(sortedList);
        jTable1.setSelectionModel(selectionModel);
        jTable1.getSelectionModel().addListSelectionListener(this);

        TableCellRenderer dateRenderer = new DateTableCellRenderer("yyyy-MM-dd HH:mm,E");
        jTable1.getColumnModel().getColumn(1).setCellRenderer(dateRenderer);
        jTable1.getColumnModel().getColumn(2).setCellRenderer(dateRenderer);

        TableComparatorChooser tableSorter = TableComparatorChooser.install(
                jTable1, sortedList, TableComparatorChooser.SINGLE_COLUMN);


        NoteRepository rep = Lookup.getDefault().lookup(NoteRepository.class);
        rep.addPropertyChangeListener(this);
        Lookup.getDefault().lookup(NoteFinder.class).addPropertyChangeListener(this);
        LOG.log(Level.INFO, "{0} registered as listener to NoteRepositor and NoteFinder", this.toString());
        searchTextField.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e) {
                search();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                search();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                search();
            }

            private void search() {
                searchstring = searchTextField.getText();
                LOG.log(Level.INFO, "searchTextField changed: {0}", searchTextField.getText());
                performSearch(true);
            }
        });
        performSearch(false); // to populate the table
    }

    @Override
    public void componentClosed() {
        // TODO add custom code on component closing
        NoteRepository rep = Lookup.getDefault().lookup(NoteRepository.class);
        rep.removePropertyChangeListener(this);
        Lookup.getDefault().lookup(NoteFinder.class).removePropertyChangeListener(this);
        LOG.log(Level.INFO, "{0} removed as listener to NoteRepositor and NoteFinder", this.toString());
    }

    @Override
    protected void componentActivated() {
        searchTextField.requestFocusInWindow();
    }

    void writeProperties(java.util.Properties p) {
        // better to version settings since initial version as advocated at
        // http://wiki.apidesign.org/wiki/PropertyFiles
        p.setProperty("version", "1.0");
        // TODO store your settings
    }

    Object readProperties(java.util.Properties p) {
        NoteListTopComponent singleton = NoteListTopComponent.getDefault();
        singleton.readPropertiesImpl(p);
        return singleton;
    }

    private void readPropertiesImpl(java.util.Properties p) {
        String version = p.getProperty("version");
        // TODO read your settings according to their version
    }

    @Override
    protected String preferredID() {
        return PREFERRED_ID;
    }

    private Collection<Note> getAllNotesInDb() {
        NoteRepository rep = Lookup.getDefault().lookup(NoteRepository.class);
        return rep.getAllNotes();
    }

    @Override
    /**
     * @see         javax.swing.event.ListSelectionListener#valueChanged
     */
    public void valueChanged(ListSelectionEvent arg0) {
        if (!arg0.getValueIsAdjusting()) {
            if (selectionModel != null) {
                EventList<Note> selectionList = selectionModel.getSelected();
                if ((selectionList != null) && (!selectionList.isEmpty())) {
                    Object value = selectionList.get(0);
                    if (value != null) {
                        LOG.log(Level.FINE, "selection changed: {0}", value.toString());
                        ic.set(Collections.singleton(value), null);
                        return;
                    }
                }
            }
            ic.set(Collections.emptySet(), null);
            LOG.log(Level.FINE, "selection changed: nothing selected");
        }
    }

    public synchronized void unDim(boolean reallyUnDim) {
        if (!reallyUnDim) {
            return;
        }
        final int valAfterDecrement = dimCounter.decrementAndGet();

        if (valAfterDecrement < 1) {

            if (undimTask != null) {
                undimTask.schedule(500);
            } else {
                Runnable runnable = new Runnable() {

                    public void run() {
                        if (dimCounter.get() < 1) {
                            setGlasspane(false);
                        }
                    }
                };
                undimTask = RP.post(runnable, 500);
            }
        }
        LOG.log(Level.INFO, "undimmed: {0}", valAfterDecrement);

        while (dimCounter.get() < 0) {
            // if dimcounter when under zero, something wrong
            // try to reset it to zero
            final int cur = dimCounter.get();
            if (cur < 0) {
                LOG.log(Level.SEVERE, "dimCounter is less that zero  ({0})", cur);
                dimCounter.compareAndSet(cur, 0);
            }
        }
    }

    public synchronized void dim(boolean reallyDim) {
        if (!reallyDim) {
            return;
        }
        if (dimCounter.get() < 0) {
            throw new IllegalStateException("dimCounter < 0 (" + dimCounter.get() + ")");
        }
        setGlasspane(true);
        final int value = dimCounter.incrementAndGet();
        LOG.log(Level.INFO, "dimmed: {0}", value);
    }

    public void setGlasspane(final boolean visible) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                customGlassPane.setVisible(visible);
            }
        });
    }

    @Override
    public void propertyChange(final PropertyChangeEvent evt) {
        LOG.info("change in noterepository / index");
        if (null != updateAllNotesTask) {
            long delay = calculateDelay();
            updateAllNotesTask.schedule((int) delay);
            LOG.log(Level.INFO, "updateAllNotesTask scheduled in {0} ms", delay);
            return;
        }

        Runnable runnable = new Runnable() {

            @Override
            public void run() {
                final long delta = System.currentTimeMillis() - lastPropertyChangeTimestamp;
                LOG.log(Level.INFO, "time since last clear and repopulate: {0}", delta);
                //make sure that we don't refresh more often that ALLNOTESREFRESHDELAY
                if (delta < ALLNOTESREFRESHDELAY) {
                    try {
                        final long waitFor = ALLNOTESREFRESHDELAY - delta + 500;
                        LOG.log(Level.INFO, "wait for: {0}", waitFor);
                        Thread.sleep(waitFor);
                    } catch (InterruptedException ex) {
                    }
                }

                long start = System.currentTimeMillis();

                if ("notes".equals(evt.getPropertyName())) {
                    final Collection<Note> allNotesInDb = getAllNotesInDb();
                    if (allNotes.isEmpty()) {
                        //if it's the first do it outsithe the critical section
                        //it takes a while to get allNotes to the reach the
                        //capacity.
                        allNotes.addAll(allNotesInDb);
                    }
                    long startLockList = System.currentTimeMillis();
                    if (!allNotes.equals(allNotesInDb)) {
                        LOG.info("clear and repopulate allNotes list");
                        //allNotes.getReadWriteLock().writeLock().lock();
                        try {
                            allNotes.clear();
                            allNotes.addAll(allNotesInDb);
                        } finally {
                            //allNotes.getReadWriteLock().writeLock().unlock();
                        }
                    }
                    LOG.log(Level.INFO, "We locked the eventlist for {0} ms", System.currentTimeMillis() - startLockList);
                } else {
                    LOG.log(Level.INFO, "Event {0} doesn't require update of allNotes", evt.getPropertyName());
                }

                NoteListTopComponent.this.refresh();
                lastPropertyChangeTimestamp = System.currentTimeMillis();
                LOG.log(Level.INFO, "clear and repopulate took {0} ms", lastPropertyChangeTimestamp - start);

            }
        };
        updateAllNotesTask = RPALLNOTESUPDATE.post(runnable, DELAY);
    }

    private TableModel getGlazedListTableModel() {


        String[] propertyNames = {"title", "created", "updated"};
        String[] columnLabels = {"Title", "Created", "Last modified"};
        TableFormat<Note> tf = GlazedLists.tableFormat(Note.class, propertyNames, columnLabels);
        EventTableModel<Note> etm = new EventTableModel<Note>(sortedList, tf);

        return etm;
    }
}
