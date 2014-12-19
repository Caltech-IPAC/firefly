package edu.caltech.ipac.firefly.server.fuse;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.HierarchicalStreamDriver;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.io.xml.DomReader;
import edu.caltech.ipac.firefly.data.SpacialType;
import edu.caltech.ipac.firefly.data.fuse.MissionInfo;
import edu.caltech.ipac.firefly.data.fuse.config.ActiveTargetTag;
import edu.caltech.ipac.firefly.data.fuse.config.ArtifactTag;
import edu.caltech.ipac.firefly.data.fuse.config.CatalogSetTag;
import edu.caltech.ipac.firefly.data.fuse.config.CoverageTag;
import edu.caltech.ipac.firefly.data.fuse.config.DataSourceTag;
import edu.caltech.ipac.firefly.data.fuse.config.IbeTag;
import edu.caltech.ipac.firefly.data.fuse.config.ImageSetTag;
import edu.caltech.ipac.firefly.data.fuse.config.ImageTag;
import edu.caltech.ipac.firefly.data.fuse.config.MissionTag;
import edu.caltech.ipac.firefly.data.fuse.config.SpacialTypeTag;
import edu.caltech.ipac.firefly.data.fuse.config.SpectrumTag;
import edu.caltech.ipac.firefly.data.fuse.config.TapTag;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.dyn.DynTagMapper;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.util.Ref;
import edu.caltech.ipac.util.StringUtils;
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


public class MissionConfigManager {

    private static MissionConfigManager mgr;

    private final String CONFIG_BASE = "/configurable/";

    private static final Logger.LoggerImpl logger = Logger.getLogger();


    /**
     * singleton; use getInstance().
     */
    private MissionConfigManager() {
    }

    public static MissionConfigManager getInstance() {
        if (mgr == null) {
            mgr = new MissionConfigManager();
        }
        return mgr;
    }


    public List<MissionInfo> getMissionInfos() {
        List<MissionInfo> retval = new ArrayList<MissionInfo>();
        MissionConfig dsc = getMissionConfig();
        for (MissionTag dstag : dsc.getAllMissions()) {
            HashSet<SpacialType> types;
            MissionInfo dsi = new MissionInfo(dstag.getName(), dstag.getDesc());
            retval.add(dsi);
            SpacialTypeTag spacialTypes = dstag.getSpacialTypes();
            if (spacialTypes != null) {
                // catalogs
                if (!StringUtils.isEmpty(spacialTypes.getCatalog())) {
                    String[] vals = spacialTypes.getCatalog().split(",");
                    types = new HashSet<SpacialType>(vals.length);
                    for (String s : vals) {
                        types.add(SpacialType.valueOf(s.trim()));
                    }
                    dsi.setCatSpatial(types);
                }
                // images
                if (!StringUtils.isEmpty(spacialTypes.getImageSet())) {
                    String[] vals = spacialTypes.getImageSet().split(",");
                    types = new HashSet<SpacialType>(vals.length);
                    for (String s : vals) {
                        types.add(SpacialType.valueOf(s.trim()));
                    }
                    dsi.setImageSpatial(types);
                }
                // spectrum
                if (!StringUtils.isEmpty(spacialTypes.getSpectrum())) {
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

    //    public MissionDetail getMissionDetail(String name) {
//
//    }
//
    public MissionConfig getMissionConfig() {

        // obtain object from cache and compare timestamps with file
        File xmldir = ServerContext.getConfigFile(CONFIG_BASE);
        final Ref<Long> lastModified = new Ref<Long>(new Long(0));
        if (xmldir == null || !xmldir.canRead()) {
            return new MissionConfig();
        }
        File[] xmlFiles = xmldir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                File f = new File(dir, name);
                if (f.lastModified() > lastModified.getSource()) {
                    lastModified.setSource(f.lastModified());
                }
                return name.endsWith(".xml");
            }
        });

        CacheKey cacheKey = new StringKey("FuseMissionConfig");
        Cache cache = CacheManager.getCache(Cache.TYPE_PERM_SMALL);
        MissionConfig dsConfig = (MissionConfig) cache.get(cacheKey);
        if (dsConfig == null || lastModified.getSource() > dsConfig.getLastModified()) {
            dsConfig = parseMissionConfig(xmlFiles);
            cache.put(cacheKey, dsConfig);
        }
        return dsConfig;
    }

    private MissionConfig parseMissionConfig(File[] xmlFiles) {
        MissionConfig config = new MissionConfig();

        for (File f : xmlFiles) {
            MissionTag ds = getMission(f);
            if (ds != null) {
                config.addMission(ds);
            }
        }
        config.setLastModified(System.currentTimeMillis());
        return config;
    }


    private MissionTag getMission(File xmlFile) {

        MissionTag dstag = null;

        if (xmlFile == null || !xmlFile.canRead()) {
            System.out.println("Unable to read file:" + xmlFile);
            return null;
        }

        try {
            HierarchicalStreamDriver driver = new DomDriver();
            XStream xstream = new XStream(driver);

            DynTagMapper.doMappings(xstream);
            doMappings(xstream);

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setXIncludeAware(true);
            dbf.setNamespaceAware(true);
            DocumentBuilder builder = dbf.newDocumentBuilder();
            Document doc = builder.parse(xmlFile);
            dstag = (MissionTag) xstream.unmarshal(new DomReader(doc));


            System.out.println("Loaded dataset xml file: " + xmlFile.getPath());

            System.out.println("Marshalled object: \n" + xstream.toXML(dstag));

            return dstag;
        } catch (Exception e) {
            logger.error("Error reading xml file: " + xmlFile.getPath());
            e.printStackTrace();
        }
        if (dstag != null) {
            dstag.setLastModified(System.currentTimeMillis());
        }
        return dstag;
    }

    public static final void main(String[] args) {
        MissionTag dstag = MissionConfigManager.getInstance().getMission(new File("/hydra/cm/ife/firefly/config/fuse/configurable/planck.xml"));
        System.out.println("done...");
    }

    public static void doMappings(XStream xstream) {

        Class[] classArr = {ActiveTargetTag.class, ArtifactTag.class, CatalogSetTag.class, CoverageTag.class,
                DataSourceTag.class, IbeTag.class, ImageSetTag.class, ImageTag.class, MissionTag.class,
                SpacialTypeTag.class, SpectrumTag.class, TapTag.class};
        xstream.processAnnotations(classArr);
    }
}

