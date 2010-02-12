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
package com.rubenlaguna.en4j.jpaentities;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 *
 * @author ecerulm
 */
@Entity
@Table(name = "RESOURCES")
public class Resource implements Serializable {

    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;
    @Basic
    private String hashValue;
    @Basic
    @Column(name = "dataBs", length = 52428800)
    private byte[] dataBs;
    @ManyToOne(optional = false)
    @JoinColumn(nullable = false)
    private Notes owner;

    public void setHash(String hash) {
        hashValue = hash;
    }

    public String getHash() {
        return hashValue;
    }

    public void setData(byte[] data) {
        this.dataBs = data;
    }

    public byte[] getData() {
        return this.dataBs;
    }

    public void setOwner(Notes note) {
        this.owner = note;
    }

    public Notes getOwner() {
        return this.owner;
    }
}
