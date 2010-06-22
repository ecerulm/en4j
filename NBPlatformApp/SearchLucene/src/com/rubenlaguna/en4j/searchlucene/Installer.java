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
package com.rubenlaguna.en4j.searchlucene;

import com.rubenlaguna.en4j.interfaces.NoteFinder;
import org.openide.modules.ModuleInstall;
import java.lang.management.ManagementFactory;
import javax.management.ObjectName;
import javax.management.JMException;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;

/**
 * Manages a module's lifecycle. Remember that an installer is optional and
 * often not needed at all.
 */
public class Installer extends ModuleInstall {

    final static SearchLuceneMBeanImpl mbean = new SearchLuceneMBeanImpl();

    @Override
    public void restored() {
        try { // Register MBean in Platform MBeanServer
            ManagementFactory.getPlatformMBeanServer().
                    registerMBean(mbean,new ObjectName("com.rubenlaguna.en4j.searchlucene:type=SearchLuceneMBeanImpl"));
        }catch(JMException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    @Override
    public void close() {
        Lookup.getDefault().lookup(NoteFinder.class).close();
        IndexWriterWrapper.getInstance().close();
    }


}
