package edu.caltech.ipac.firefly.server;

import edu.caltech.ipac.astro.IpacTableException;
import edu.caltech.ipac.astro.IpacTableReader;
import edu.caltech.ipac.astro.IpacTableWriter;
import edu.caltech.ipac.firefly.data.SortInfo;
import edu.caltech.ipac.firefly.data.table.RawDataSet;
import edu.caltech.ipac.firefly.server.cache.UserCache;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.CollectionUtil;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataGroupQuery;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.StringKey;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Date: Nov 9, 2007
 *
 * @author loi
 * @version $Id: ResourceManager.java,v 1.6 2009/06/23 18:57:19 loi Exp $
 */
public class ResourceManager {

    public static final String KEY_PREFIX = "ResourceManager";


//====================================================================
//  Ipac Table related functions
//====================================================================


    public DataGroup getIpacTable(File source, SortInfo sortInfo,
                                  CollectionUtil.Filter<DataObject>... filters) throws IpacTableException {

        if (!source.exists()) {
            throw new IpacTableException("File not found:" + source.getAbsolutePath());
        }

        StringKey key = new StringKey(KEY_PREFIX, source);
        if (filters != null && filters.length > 0) {
            key.appendToKey((Object[])filters);
        }

        Cache cache = UserCache.getInstance();
        DataGroup dg = (DataGroup) cache.get(key);
        if (dg == null) {
            // not in cache... go get data
            dg = IpacTableReader.readIpacTable(source, source.getName());

            // do filtering...
            if (filters != null && filters.length > 0) {
                dg = QueryUtil.doFilter(dg, filters);
            }

            cache.put(key, dg);
        }
        // do sorting...
        if (sortInfo != null) {
            QueryUtil.doSort(dg, sortInfo);
        }

        return dg;
    }

    public RawDataSet getIpacTableView(File source) throws IpacTableException {
        DataGroup dg = getIpacTable(source, null);
        RawDataSet ds = QueryUtil.getRawDataSet(dg, 0, dg.size());
        return ds;

    }

    public RawDataSet getIpacTableView(File source,  int startIndex, int pageSize,
                                   SortInfo sortInfo, CollectionUtil.Filter<DataObject>... filters) throws IpacTableException {
        DataGroup dg = getIpacTable(source, sortInfo, filters);
        RawDataSet ds = QueryUtil.getRawDataSet(dg, startIndex, pageSize);
        return ds;

    }

    public void saveIpacTable(OutputStream saveTo, File source, SortInfo sortInfo,
                              DataGroupQuery.DataFilter... filters) throws IOException, IpacTableException {
        DataGroup dg = getIpacTable(source, sortInfo, filters);
        IpacTableWriter.save(saveTo, dg);
    }

//====================================================================
//
//====================================================================


    /**
     * Loads all of the properties file in the given resources directory.
     * It will search first the class path, and then from file system.
     * @param resourcesDir directory
     */
    public static void loadAllProperties(String resourcesDir) {
        
        try {
            URL url = ResourceManager.class.getResource(resourcesDir );

            File resDir;
            if ( url != null ) {
                resDir = new File(url.toURI());
            } else {
                resDir = new File(resourcesDir);
            }
            if (resDir.isDirectory()) {
                File[] props = resDir.listFiles(new FilenameFilter(){
                            public boolean accept(File dir, String name) {
                                return name.endsWith(".prop") || name.endsWith(".properties");
                            }
                        });
                for(File f : props) {
                    AppProperties.loadClassPropertiesFromFileToPdb(f, null);
                }
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Unable to load resources");
        }
    }



    public static File getFromUrl(URL url, String tempFilePrefix, String tempFileExt) throws IpacTableException, IOException {
        Cache cache = CacheManager.getCache(Cache.TYPE_PERM_FILE);
        File inf = (File) cache.get(new StringKey(url.toString()));
        if (inf == null) {
            BufferedReader ir = null;
            PrintWriter writer = null;
            try {
                Logger.briefDebug("ResourceManager.getFromUrl(" + url+","+tempFilePrefix+","+tempFileExt);
                ir = new BufferedReader(new InputStreamReader(url.openStream()));
                inf = File.createTempFile(tempFilePrefix, tempFileExt, ServerContext.getPermWorkDir());
                writer = new PrintWriter(inf);
                String l = ir.readLine();
                do {
                    writer.println(l);
                    l = ir.readLine();
                } while (l != null);
                cache.put(new StringKey(url), inf);
            } finally {
                if (ir != null) ir.close();
                if (writer != null) writer.close();
            }
        }
        return inf;
    }
}
