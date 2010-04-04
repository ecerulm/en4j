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

import java.util.Date;

/**
 *
 * @author ecerulm
 */
public interface Resource {

    byte[] getData();

    String getDataHash();

    byte[] getAlternateData();

    String getAlternateDataHash();

    double getAltitude();

    public double getLatitude();

    public double getLongitude();

    public boolean getPremiumAttachment();

    String getCameraMake();

    String getCameraModel();

    String getFilename();

    String getGuid();

    String getMime();

    String getNoteguid();

    byte[] getRecognition();

    Date getTimestamp();
}
