/*
 * Notes.java
 * 
 * Copyright (c) 2009 Ruben Laguna <ruben.laguna at gmail.com>. All rights reserved.
 * 
 * This file is part of en4j.
 * 
 * en4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 * 
 * en4j is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with en4j.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.rubenlaguna.en4j.jpaentities;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 *
 * @author Ruben Laguna <ruben.laguna at gmail.com>
 */
@Entity
@Table(name = "NOTES")
public class Notes implements Serializable {

    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;
    @Basic(optional = false)
    @Column(name = "TITLE", length = 255)
    private String title;
    @Basic(optional = false, fetch = FetchType.LAZY)
    @Column(name = "CONTENT", length = 5242880)
    private String content;
    @Basic(optional = false)
    @Column(name = "CREATED")
    @Temporal(TemporalType.TIMESTAMP)
    private Date created;
    @Basic(optional = false)
    @Column(name = "UPDATED")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updated;
    @Column(name = "SOURCEURL")
    private String sourceurl;

    @OneToMany(fetch=FetchType.LAZY,cascade=CascadeType.ALL,mappedBy="owner")
    @MapKey(name="hashValue")
    private Map<String, Resource> resources = new HashMap<String,Resource>();

    public Notes() {
    }

    public Notes(Integer id, String title, String content, Date created, Date updated) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.created = created;
        this.updated = updated;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getSourceurl() {
        return sourceurl;
    }

    public void setSourceurl(String sourceurl) {
        this.sourceurl = sourceurl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Date getUpdated() {
        return updated;
    }

    public void setUpdated(Date updated) {
        this.updated = updated;
    }

    public Resource getResource(String hash)
    {
        return resources.get(hash);
    }

    public void addResource(Resource resource) {
        if(null==resource) {
            throw new IllegalArgumentException("resource cannot be null");
        }
        if(null==resources) {
            resources = new HashMap<String, Resource>();
        }
        resource.setOwner(this);
        resources.put(resource.getHash(), resource);
    }
    

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Notes)) {
            return false;
        }
        Notes other = (Notes) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.rubenlaguna.noteentitieslibrary.Notes[id=" + id + "]";
    }


}
