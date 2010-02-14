/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
// com.rubenlaguna.en4j.sync.SyncServiceBean
package com.rubenlaguna.en4j.sync;

import com.rubenlaguna.en4j.interfaces.SynchronizationService;
import java.beans.*;
import java.io.Serializable;
import org.openide.util.Lookup;

/**
 *
 * @author Ruben Laguna <ruben.laguna@gmail.com>
 */
public class SyncServiceBean implements Serializable, PropertyChangeListener {

    private int pendingRemoteUpdateNotes = 11200;
    public static final String PROP_PENDINGREMOTEUPDATENOTES = "pendingRemoteUpdateNotes";
    public static final String PROP_SAMPLE_PROPERTY = "sampleProperty";
    private String sampleProperty;
    private PropertyChangeSupport propertySupport;

    public SyncServiceBean() {
        propertySupport = new PropertyChangeSupport(this);
        if (!Beans.isDesignTime()) {
            Lookup.getDefault().lookup(SynchronizationService.class).addPropertyChangeListener(this);
        }
    }

    public String getSampleProperty() {
        return sampleProperty;
    }

    public void setSampleProperty(String value) {
        String oldValue = sampleProperty;
        sampleProperty = value;
        propertySupport.firePropertyChange(PROP_SAMPLE_PROPERTY, oldValue, sampleProperty);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.removePropertyChangeListener(listener);
    }

    /**
     * Get the value of pendingRemoteUpdateNotes
     *
     * @return the value of pendingRemoteUpdateNotes
     */
    public int getPendingRemoteUpdateNotes() {
        return pendingRemoteUpdateNotes;
    }

    /**
     * Set the value of pendingRemoteUpdateNotes
     *
     * @param pendingRemoteUpdateNotes new value of pendingRemoteUpdateNotes
     */
    public void setPendingRemoteUpdateNotes(int pendingRemoteUpdateNotes) {
        int oldPendingRemoteUpdateNotes = this.pendingRemoteUpdateNotes;
        this.pendingRemoteUpdateNotes = pendingRemoteUpdateNotes;
        propertySupport.firePropertyChange(PROP_PENDINGREMOTEUPDATENOTES, oldPendingRemoteUpdateNotes, pendingRemoteUpdateNotes);
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(SynchronizationService.PROP_PENDINGREMOTEUPDATENOTES)) {
            setPendingRemoteUpdateNotes((Integer) evt.getNewValue());
        }
    }
}
