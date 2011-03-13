/*
 * Copyright (C) 2011 Ruben Laguna <ruben.laguna@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.rubenlaguna.en4j.noterepositoryjdbm;

import java.lang.management.ManagementFactory;
import javax.management.JMException;
import javax.management.ObjectName;
import org.openide.modules.ModuleInstall;
import org.openide.util.Lookup;

/**
 *
 * @author Ruben Laguna <ruben.laguna@gmail.com>
 */
public class Installer extends ModuleInstall {

    public static final JdbmMgmt mbean = new JdbmMgmt();

    @Override
    public void close() {
        Lookup.getDefault().lookup(NoteRepositoryJDBMImpl.class).close();
    }

    @Override
    public void restored() {
        try { // Register MBean in Platform MBeanServer
            ManagementFactory.getPlatformMBeanServer().
                    registerMBean(mbean, new ObjectName("com.rubenlaguna.en4j.noterepositoryjdbm:type=JdbmMgmt"));
        } catch (JMException ex) {
            // TODO handle exception
        }
    }
}
