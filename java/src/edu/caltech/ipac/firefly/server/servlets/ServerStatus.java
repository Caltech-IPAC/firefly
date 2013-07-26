package edu.caltech.ipac.firefly.server.servlets;

import edu.caltech.ipac.firefly.server.cache.EhcacheProvider;
import edu.caltech.ipac.firefly.server.packagedata.PackagingController;
import edu.caltech.ipac.firefly.server.visualize.VisStat;
import edu.caltech.ipac.util.StringUtils;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.distribution.CacheManagerPeerProvider;
import net.sf.ehcache.distribution.CachePeer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.List;

/**
 * Date: Jun 3, 2009
 *
 * @author loi
 * @version $Id: ServerStatus.java,v 1.1 2009/06/04 00:12:42 loi Exp $
 */
public class ServerStatus extends BaseHttpServlet {

    protected void processRequest(HttpServletRequest req, HttpServletResponse res) throws Exception {

        res.addHeader("content-type", "text/plain");
        PrintWriter writer = res.getWriter();
        try {
            EhcacheProvider prov = (EhcacheProvider) edu.caltech.ipac.util.cache.CacheManager.getCacheProvider();
            CacheManager cm = prov.getEhcacheManager();
            // display ehcache info
            writer.println("EHCACHE INFORMATION:");
            writer.println("-------------------:");
            writer.println("Status        : " + cm.getStatus());
            writer.println("DiskStore Path: " + cm.getDiskStorePath());
            writer.println();

            writer.println("Caches: ");
            CacheManagerPeerProvider peerProv = cm.getCacheManagerPeerProvider();
            String[] cacheNames = cm.getCacheNames();
            for(String n : cacheNames) {
                Ehcache c = cm.getCache(n);
                writer.println("\t" + c.getName());
                writer.println("\tServerStatus    : " + c.getStatus());
                writer.println("\tStatistics: " + c.getStatistics().toString());
                List peers = peerProv.listRemoteCachePeers(c);
                for(Object o : peers) {
                    CachePeer cp = (CachePeer) o;
                    writer.println("\tReplicating with: " + cp.getUrl());
                }
                writer.println();
            }

            skip(writer);
            showVisualizationStatus(writer);

            skip(writer);
            showPackagingStatus(writer);


        } finally {
            writer.flush();
            writer.close();
        }

    }

    private static void skip(PrintWriter w) { w.println("\n\n\n"); }

    private static void showVisualizationStatus(PrintWriter w) {
        w.println("Visualization Information");
        w.println("-------------------------");
        w.println(StringUtils.toString(VisStat.getInstance().getStatus(), "\n"));
    }

    private static void showPackagingStatus(PrintWriter w) {
        w.println("Packaging Controller Information");
        w.println("--------------------------------");
        w.println(StringUtils.toString(PackagingController.getInstance().getStatus(), "\n"));
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
