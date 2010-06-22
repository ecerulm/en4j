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
