package edu.caltech.ipac.firefly.server.cache;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.apache.log4j.PropertyConfigurator;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

/**
 * Date: Jul 17, 2008
 *
 * @author loi
 * @version $Id: EhcacheProvider.java,v 1.28 2012/09/25 23:50:59 loi Exp $
 */
public class EhcacheTest {


    private CacheManager cman;
    

    public EhcacheTest() {
            URL url = null;
            File f = new File("ehcache.xml");
            if (f.canRead()) {
                try {
                    url = f.toURI().toURL();
                } catch (MalformedURLException e) {
                    System.out.println("bad ehcache file location: " + f.getAbsolutePath());
                    e.printStackTrace();
                }
            } else {
                System.out.println("bad ehcache file location: " + f.getAbsolutePath());
            }
        cman = new CacheManager(url);
    }

    public CacheManager getCman() {
        return cman;
    }

    public static void main(String[] args) {
        File cfg = new File("log4j.properties");
        System.out.println("Initializing Log4J using file:" + cfg.getAbsolutePath());
        PropertyConfigurator.configureAndWatch(cfg.getAbsolutePath());

        CacheManager cman = new EhcacheTest().getCman();
        Cache cache = cman.getCache("PERM_SMALL");
        System.out.println("size:" + cache.getStatistics().getSize());

        do {
            try {
                cache.put(new Element(System.currentTimeMillis(), "just a test: " + new Date()));
                Thread.currentThread().sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } while (true);
    }
}
