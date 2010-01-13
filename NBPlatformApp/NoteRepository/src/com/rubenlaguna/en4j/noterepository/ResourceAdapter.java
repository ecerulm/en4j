/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
