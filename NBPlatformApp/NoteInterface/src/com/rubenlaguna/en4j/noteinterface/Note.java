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
package com.rubenlaguna.en4j.noteinterface;

import java.util.Collection;
import java.util.Date;

/**
 *
 * @author ecerulm
 */
public interface Note {

    public String getContent();

    public void setContent(String content);

    public Date getCreated();

    public void setCreated(Date created);

    public Integer getId();

    public void setId(Integer id);

    public String getSourceurl();

    public void setSourceurl(String sourceurl);

    public String getTitle();

    public void setTitle(String title);

    public Date getUpdated();

    public void setUpdated(Date updated);

    public int getUpdateSequenceNumber();

    public Resource getResource(String hash);

    public Collection<Resource> getResources();
}
