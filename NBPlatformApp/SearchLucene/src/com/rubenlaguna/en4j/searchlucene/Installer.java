/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
