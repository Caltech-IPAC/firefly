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
/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
* INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
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
