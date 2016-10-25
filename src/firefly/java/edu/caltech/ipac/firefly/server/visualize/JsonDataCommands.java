/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize;
/**
 * User: roby
 * Date: 3/5/12
 * Time: 12:26 PM
 */


import edu.caltech.ipac.firefly.server.ServCommand;
import edu.caltech.ipac.util.download.URLDownload;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheKey;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.StringKey;

import java.io.File;
import java.net.URL;
import java.util.Map;

/**
 * @author Trey Roby
 */
public class JsonDataCommands {



    public static class StaticJsonData extends ServCommand {


        public String doCommand(Map<String, String[]> paramMap) throws Exception {

            SrvParam sp= new SrvParam(paramMap);
            String url = sp.getRequired(ServerParams.URL);
            String name = sp.getRequired(ServerParams.FILE);
            String retval= "";

            Cache cache= CacheManager.getCache(Cache.TYPE_PERM_SMALL);
            CacheKey urlKey= new StringKey(url);
            File outfile= (File)cache.get(urlKey);
            try {
                if (outfile==null) {
                    outfile= File.createTempFile("irsa-toobar", ".js", ServerContext.getPermWorkDir());
                    cache.put(urlKey, outfile);
                }
                URLDownload.getDataToFile(new URL(url), outfile, null, null, null, false, true, 0);
            } catch (Exception e) {
                outfile= null;
                cache.put(urlKey,null);
            }


            if (outfile==null) {
                outfile= ServerContext.convertToFile(name);
            }

            try {
                retval= FileUtil.readFile(outfile);
            } catch (Exception e) {
                throw new Exception("Failed to read file");
            }
            return retval;
        }

        public boolean getCanCreateJson() { return true; }
    }
}

