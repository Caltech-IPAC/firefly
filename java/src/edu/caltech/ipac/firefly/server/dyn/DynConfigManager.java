package edu.caltech.ipac.firefly.server.dyn;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.HierarchicalStreamDriver;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.io.xml.DomReader;
import edu.caltech.ipac.firefly.data.dyn.xstream.CatalogTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.ConstraintsTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.DownloadTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.EventWorkerTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.FormEventWorkerTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.HelpTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.HtmlLoaderTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.LayoutAreaTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.LayoutTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.ParamTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.PreDefFieldTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.PreviewTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.ProjectItemTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.ProjectListTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.ProjectTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.SearchGroupTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.SearchTypeTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.SplitPanelTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.TableTag;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.dyn.xstream.CatalogConverter;
import edu.caltech.ipac.firefly.server.dyn.xstream.EventWorkerConverter;
import edu.caltech.ipac.firefly.server.dyn.xstream.FieldGroupConverter;
import edu.caltech.ipac.firefly.server.dyn.xstream.FormEventWorkerConverter;
import edu.caltech.ipac.firefly.server.dyn.xstream.HtmlLoaderConverter;
import edu.caltech.ipac.firefly.server.dyn.xstream.LayoutAreaConverter;
import edu.caltech.ipac.firefly.server.dyn.xstream.LayoutConverter;
import edu.caltech.ipac.firefly.server.dyn.xstream.PreviewConverter;
import edu.caltech.ipac.firefly.server.dyn.xstream.ProjectConverter;
import edu.caltech.ipac.firefly.server.dyn.xstream.QueryConverter;
import edu.caltech.ipac.firefly.server.dyn.xstream.ResultConverter;
import edu.caltech.ipac.firefly.server.dyn.xstream.SplitPanelConverter;
import edu.caltech.ipac.firefly.server.dyn.xstream.TableConverter;
import edu.caltech.ipac.firefly.server.dyn.xstream.ViewConverter;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheKey;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.StringKey;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


public class DynConfigManager {

    private static DynConfigManager mgr;

    public final static String HYDRA_PROJ_ROOT = "HydraProject";
    private final String CONFIG_BASE = "/configurable/";
    private final String PROJECT_LIST_XML = "projectList.xml";

    private static final Logger.LoggerImpl logger = Logger.getLogger();


    /**
     * singleton; use getInstance().
     */
    private DynConfigManager() {
    }

    public static DynConfigManager getInstance() {
        if (mgr == null) {
            mgr = new DynConfigManager();
        }
        return mgr;
    }


    public ProjectListTag getCachedProjects() {
        ProjectListTag obj;

        // obtain object from cache and compare timestamps with file
        File xmlFile = ServerContext.getConfigFile(CONFIG_BASE + PROJECT_LIST_XML);
        String xmlFileName = xmlFile.getAbsolutePath();
        long xmlLastModified = getLastModified(xmlFileName);

        CacheKey cacheKey = new StringKey("HydraProjectList");
        logger.info("cache key: " + cacheKey.getUniqueString());
        Cache cache = CacheManager.getCache(Cache.TYPE_PERM_SMALL);
        ProjectListCache pCache = (ProjectListCache) cache.get(cacheKey);
        if (pCache == null || pCache.getXmlTimestamp() < xmlLastModified) {
            try {
                XStream xstream = new XStream();

                // since XML contains 'id', must alias the System's 'id'
                xstream.aliasSystemAttribute("refid", "id");

                // process annotations & register custom converters
                xstream.processAnnotations(ProjectListTag.class);


                obj = (ProjectListTag) xstream.fromXML(new FileInputStream(xmlFileName));

                logger.info("Loaded top-level xml file: " + xmlFileName);
                //logger.debug("Marshalled object: \n" + xstream.toXML(obj));

                // remove projects whose xml files do not exist
                List<String> invalidIds = new ArrayList<String>();
                List<ProjectItemTag> piList = obj.getProjectItems();
                for (ProjectItemTag pi : piList) {
                    if (!Boolean.parseBoolean(pi.getIsCommand())) {
                        File f = ServerContext.getConfigFile(CONFIG_BASE + pi.getConfigFile());
                        if (f == null || !f.exists()) {
                            invalidIds.add(pi.getId());
                        }
                    }
                }
                for (String id : invalidIds) {
                    obj.removeProject(id);
                }

                ProjectListCache newCache = new ProjectListCache(xmlFileName, xmlLastModified, obj);
                cache.put(cacheKey, newCache);

            } catch (Exception e) {
                logger.error("Error reading xml file: " + xmlFileName);
                obj = null;
            }

        } else {
            obj = pCache.getData();
            logger.info("Using server-side cached xml object: " + xmlFileName);
        }

        return obj;
    }


    public ProjectTag getCachedProject(String projectId) {
        ProjectTag obj;

        ProjectListTag pList = getCachedProjects();
        String xmlFilename = pList.getXmlForProject(projectId);

        // obtain object from cache and compare timestamps with file
        File xmlFile = ServerContext.getConfigFile(CONFIG_BASE + xmlFilename);
        String xmlFileName = xmlFile.getAbsolutePath();
        long xmlLastModified = getLastModified(xmlFileName);

        CacheKey cacheKey = new StringKey(HYDRA_PROJ_ROOT, projectId);
        logger.info("cache key: " + cacheKey.getUniqueString());
        Cache cache = CacheManager.getCache(Cache.TYPE_PERM_SMALL);
        ProjectCache pCache = (ProjectCache) cache.get(cacheKey);
        if (pCache == null || pCache.getXmlTimestamp() < xmlLastModified) {
            DynServerData dataStore = (DynServerData) DynServerData.getInstance();
            dataStore.clearAll();

            try {
                HierarchicalStreamDriver driver = new DomDriver();
                XStream xstream = new XStream(driver);

                DynTagMapper.doMappings(xstream);

                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                dbf.setXIncludeAware(true);
                dbf.setNamespaceAware(true);
//                obj = (ProjectTag) xstream.fromXML(new FileInputStream(xmlFileName));
                DocumentBuilder builder = dbf.newDocumentBuilder();

//                System.out.println("xi aware:" + builder.isXIncludeAware());

                Document doc = builder.parse(new File(xmlFileName));
                obj = (ProjectTag) xstream.unmarshal(new DomReader(doc));


                logger.info("Loaded project-level xml file: " + xmlFileName);

//                logger.debug("Marshalled object: \n" + xstream.toXML(obj));

                ProjectCache newCache = new ProjectCache(projectId, xmlFileName, xmlLastModified, obj);
                cache.put(cacheKey, newCache);

            } catch (Exception e) {
                logger.error("Error reading xml file: " + xmlFileName);
                e.printStackTrace();
                obj = null;
            }

        } else {
            obj = pCache.getData();
            logger.info("Using server-side cached xml object: " + xmlFileName);
        }

        return obj;
    }

    private long getLastModified(String fileName) {
        File f = new File(fileName);
        return f.lastModified();
    }

    public static class ProjectListCache implements Serializable {
        private String xmlFile;
        private long xmlTimestamp;
        private ProjectListTag data;

        public ProjectListCache(String fileName, long ts, ProjectListTag data) {
            this.xmlFile = fileName;
            this.xmlTimestamp = ts;
            this.data = data;
        }

        public String getXmlFile() {
            return xmlFile;
        }

        public long getXmlTimestamp() {
            return xmlTimestamp;
        }

        public ProjectListTag getData() {
            return data;
        }
    }


    public static class ProjectCache implements Serializable {
        private String projectId;
        private String xmlFile;
        private long xmlTimestamp;
        private ProjectTag data;

        public ProjectCache(String id, String fileName, long ts, ProjectTag data) {
            this.projectId = id;
            this.xmlFile = fileName;
            this.xmlTimestamp = ts;
            this.data = data;
        }


        public String getProjectId() {
            return projectId;
        }

        public String getXmlFile() {
            return xmlFile;
        }

        public long getXmlTimestamp() {
            return xmlTimestamp;
        }

        public ProjectTag getData() {
            return data;
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
