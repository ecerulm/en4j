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
package com.rubenlaguna.en4j.noterepository;

import com.rubenlaguna.en4j.jpaentities.Notes;
import com.rubenlaguna.en4j.jpaentities.Resource;

/**
 *
 * @author ecerulm
 */
class ResourceAdapter implements com.rubenlaguna.en4j.noteinterface.Resource {

    private final Resource adaptee;

    public byte[] getData() {
        return adaptee.getData();
    }

    public void setOwner(Notes note) {
        adaptee.setOwner(note);
    }

    public void setHash(String hash) {
        adaptee.setHash(hash);
    }

    public void setData(byte[] data) {
        adaptee.setData(data);
    }

    public Notes getOwner() {
        return adaptee.getOwner();
    }

    public String getHash() {
        return adaptee.getHash();
    }

    public ResourceAdapter(Resource resource) {
        this.adaptee = resource;
    }
}
