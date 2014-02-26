package edu.caltech.ipac.firefly.server.fuse;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.HierarchicalStreamDriver;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.io.xml.DomReader;
import edu.caltech.ipac.firefly.data.SpacialType;
import edu.caltech.ipac.firefly.fuse.data.DataSetInfo;
import edu.caltech.ipac.firefly.fuse.data.config.CatalogSetTag;
import edu.caltech.ipac.firefly.fuse.data.config.DataSourceTag;
import edu.caltech.ipac.firefly.fuse.data.config.DatasetTag;
import edu.caltech.ipac.firefly.fuse.data.config.IbeTag;
import edu.caltech.ipac.firefly.fuse.data.config.ImageSetTag;
import edu.caltech.ipac.firefly.fuse.data.config.SpacialTypeTag;
import edu.caltech.ipac.firefly.fuse.data.config.SpectrumTag;
import edu.caltech.ipac.firefly.fuse.data.config.TapTag;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.dyn.DynTagMapper;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.util.Ref;
import edu.caltech.ipac.util.StringUtil;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheKey;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.StringKey;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;


public class DatasetConfigManager {

    private static DatasetConfigManager mgr;

    private final String CONFIG_BASE = "/configurable/";

    private static final Logger.LoggerImpl logger = Logger.getLogger();


    /**
     * singleton; use getInstance().
     */
    private DatasetConfigManager() {
    }

    public static DatasetConfigManager getInstance() {
        if (mgr == null) {
            mgr = new DatasetConfigManager();
        }
        return mgr;
    }


    public List<DataSetInfo> getDataSetInfos() {
        List<DataSetInfo> retval = new ArrayList<DataSetInfo>();
        DatasetConfig dsc = getDatasetConfig();
        for (DatasetTag dstag : dsc.getAllDatasets()) {
            HashSet<SpacialType> types;
            DataSetInfo dsi = new DataSetInfo(dstag.getName(), dstag.getDesc());
            retval.add(dsi);
            SpacialTypeTag spacialTypes = dstag.getSpacialTypes();
            if (spacialTypes != null) {
                // catalogs
                if (!StringUtil.isEmpty(spacialTypes.getCatalog())) {
                    String[] vals = spacialTypes.getCatalog().split(",");
                    types = new HashSet<SpacialType>(vals.length);
                    for (String s : vals) {
                        types.add(SpacialType.valueOf(s.trim()));
                    }
                    dsi.setCatSpatial(types);
                }
                // images
                if (!StringUtil.isEmpty(spacialTypes.getImageSet())) {
                    String[] vals = spacialTypes.getImageSet().split(",");
                    types = new HashSet<SpacialType>(vals.length);
                    for (String s : vals) {
                        types.add(SpacialType.valueOf(s.trim()));
                    }
                    dsi.setImageSpatial(types);
                }
                // spectrum
                if (!StringUtil.isEmpty(spacialTypes.getSpectrum())) {
                    String[] vals = spacialTypes.getSpectrum().split(",");
                    types = new HashSet<SpacialType>(vals.length);
                    for (String s : vals) {
                        types.add(SpacialType.valueOf(s.trim()));
                    }
                    dsi.setSpectrumSpatial(types);
                }
            }
        }
        return retval;
    }

    //    public DataSetDetail getDataSetDetail(String name) {
//
//    }
//
    public DatasetConfig getDatasetConfig() {

        // obtain object from cache and compare timestamps with file
        File xmldir = ServerContext.getConfigFile(CONFIG_BASE);
        final Ref<Long> lastModified = new Ref<Long>(new Long(0));
        File[] xmlFiles = xmldir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                File f = new File(dir, name);
                if (f.lastModified() > lastModified.getSource()) {
                    lastModified.setSource(f.lastModified());
                }
                return name.endsWith(".xml");
            }
        });

        CacheKey cacheKey = new StringKey("FuseDataSetConfig");
        Cache cache = CacheManager.getCache(Cache.TYPE_PERM_SMALL);
        DatasetConfig dsConfig = (DatasetConfig) cache.get(cacheKey);
        if (dsConfig == null || lastModified.getSource() > dsConfig.getLastModified()) {
            dsConfig = parseDatasetConfig(xmlFiles);
            cache.put(cacheKey, dsConfig);
        }
        return dsConfig;
    }

    private DatasetConfig parseDatasetConfig(File[] xmlFiles) {
        DatasetConfig config = new DatasetConfig();

        for (File f : xmlFiles) {
            DatasetTag ds = getDataset(f);
            if (ds != null) {
                config.addDataset(ds);
            }
        }
        config.setLastModified(System.currentTimeMillis());
        return config;
    }


    private DatasetTag getDataset(File xmlFile) {

        DatasetTag dstag = null;

        try {
            HierarchicalStreamDriver driver = new DomDriver();
            XStream xstream = new XStream(driver);

            DynTagMapper.doMappings(xstream);
            doMappings(xstream);

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
//            dbf.setXIncludeAware(true);
            dbf.setNamespaceAware(true);
            DocumentBuilder builder = dbf.newDocumentBuilder();
            Document doc = builder.parse(xmlFile);
            dstag = (DatasetTag) xstream.unmarshal(new DomReader(doc));


            System.out.println("Loaded dataset xml file: " + xmlFile.getPath());

            System.out.println("Marshalled object: \n" + xstream.toXML(dstag));

            return dstag;
        } catch (Exception e) {
            logger.error("Error reading xml file: " + xmlFile.getPath());
            e.printStackTrace();
        }
        dstag.setLastModified(System.currentTimeMillis());
        return dstag;
    }

    public static final void main(String[] args) {
        DatasetTag dstag = DatasetConfigManager.getInstance().getDataset(new File("/hydra/cm/ife/firefly/config/fuse/configurable/planck.xml"));
        System.out.println("done...");
    }

    public static void doMappings(XStream xstream) {

        Class[] classArr = {CatalogSetTag.class, DatasetTag.class, DataSourceTag.class, IbeTag.class,
                ImageSetTag.class, SpacialTypeTag.class, SpectrumTag.class, TapTag.class};
        xstream.processAnnotations(classArr);
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
