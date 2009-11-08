/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rubenlaguna.en4j.noteinterface;

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
}
