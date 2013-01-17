package edu.caltech.ipac.firefly.server.db.hibernate;

import edu.caltech.ipac.firefly.server.db.DbInstance;
import edu.caltech.ipac.client.ClientLog;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.util.HashMap;
import java.util.Properties;

/**
 * @author tatianag
 * @version $Id: HibernateUtil.java,v 1.1 2009/01/22 20:45:23 tatianag Exp $
 */
public class HibernateUtil {

    private static HashMap<DbInstance,SessionFactory> sessionMap = new HashMap<DbInstance,SessionFactory>(2);

    public static SessionFactory getSessionFactory(DbInstance db) {
        if (sessionMap.containsKey(db)) {
            return sessionMap.get(db);
        } else {
            SessionFactory sf = createSessionFactory(db);
            sessionMap.put(db, sf);
            return sf;
        }
    }

    private static SessionFactory createSessionFactory(DbInstance db) {
        try {
            String resource = "edu/caltech/ipac/firefly/server/db/hibernate/resources/hibernate_"+db.name()+".cfg.xml";
            Configuration config = new Configuration().configure(resource);
            Properties props = new Properties();
            if (db.isPooled) {
                props.put("hibernate.connection.datasource", db.datasourcePath);
            } else {
                props.put("hibernate.connection.driver_class", db.jdbcDriver);
                props.put("hibernate.connection.url", db.dbUrl);
                props.put("hibernate.connection.username", db.userId);
                props.put("hibernate.connection.password", db.password);
            }
            config.addProperties(props);
            // Create the SessionFactory from hibernate.cfg.xml
            return config.buildSessionFactory();
        } catch (Throwable ex) {
            // Make sure you log the exception, as it might be swallowed
            ClientLog.warning("Initial SessionFactory creation failed dor database " + db.name() +" - "+ex);
            throw new ExceptionInInitializerError(ex);
        }
    }
    
}
/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313) 
 * OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS, 
 * HOWEVER USED.
 * 
 * IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE 
 * FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL 
 * OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO 
 * PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE 
 * ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
 * 
 * RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE 
 * AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR 
 * ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE 
 * OF THE SOFTWARE. 
 */
