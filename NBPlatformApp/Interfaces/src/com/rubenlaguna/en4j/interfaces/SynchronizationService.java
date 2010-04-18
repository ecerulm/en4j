/*
 *  Copyright (C) 2010 ecerulm
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
package com.rubenlaguna.en4j.interfaces;

import java.beans.PropertyChangeListener;
import java.util.concurrent.Future;

/**
 *
 * @author ecerulm
 */
public interface SynchronizationService {

    final String PROP_PENDINGREMOTEUPDATENOTES = "PendingRemoteUpdateNotes";

    boolean sync();

    Future<Boolean> downloadNote(String noteguid);

    public int getPendingRemoteUpdateNotes();

    public void addPropertyChangeListener(PropertyChangeListener listener);

    public void removePropertyChangeListener(PropertyChangeListener listener);

    void close();
}
