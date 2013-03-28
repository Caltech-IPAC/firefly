package edu.caltech.ipac.firefly.server.servlets;

import com.thoughtworks.xstream.XStream;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.filters.CommonFilter;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.util.CollectionUtil;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheKey;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.StringKey;
import edu.caltech.ipac.voservices.server.RemoteDataProvider;
import edu.caltech.ipac.voservices.server.VODataProvider;
import edu.caltech.ipac.voservices.server.VOTableWriter;
import edu.caltech.ipac.voservices.server.configmapper.Config;
import edu.caltech.ipac.voservices.server.configmapper.ConfigMapper;
import edu.caltech.ipac.voservices.server.servlet.VOServices;
import edu.caltech.ipac.voservices.server.tablemapper.TableMapper;
import edu.caltech.ipac.voservices.server.tablemapper.VoOption;
import edu.caltech.ipac.voservices.server.tablemapper.VoServiceParam;
import edu.caltech.ipac.voservices.server.tablemapper.VoValues;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: tlau
 * Date: 3/27/13
 * Time: 7:20 PM
 * To change this template use File | Settings | File Templates.
 *
 * test url:
 * http://localhost:8080/applications/finderchart/servlet/VoServices?RA=148.88822&DEC=69.06529&SIZE=0.5&thumbnail_size=medium&sources=DSS,SDSS,twomass,WISE&dss_bands=poss1_blue,poss1_red,poss2ukstu_blue,poss2ukstu_red,poss2ukstu_ir&SDSS_bands=u,g,r,i,z&twomass_bands=j,h,k&wise_bands=1,2,3,4
 */
public class BaseVoServices  extends BaseHttpServlet {

    public static final String CONFIG_MAPPER_FILE = "services.xml";

    private static final Logger.LoggerImpl LOG = Logger.getLogger();

    private static final String PARAM_FORMAT = "FORMAT";
    private static final String PARAM_SERVICE = "SERVICE";
    private static final String PARAM_DATASET = "DATASET";


    private static final String FORMAT_METADATA = "METADATA";
    private static final String FORMAT_ALL = "ALL";
    private static final String FORMAT_TEST = "TEST";

    private static final String CACHE_NAME = "VOSERVICES";


    public void init() {

        Assert.setServerMode(true);
        System.setProperty(CommonFilter.WEBAPP_CONFIG_DIR, getServletContext().getRealPath("WEB-INF/config"));
        String appName = getServletContext().getServletContextName();
        System.setProperty(CommonFilter.APP_NAME, appName);
        ServerContext.configInit();


        // read config and mappings
        ConfigMapper configMapper = getConfigMapper();
        configMapper.validate(ServerContext.getConfigFile("xml"));
        for (Config config : configMapper.getAllConfigs()) {
            getTableMapper(config);
        }
    }

    protected void processRequest(HttpServletRequest req, HttpServletResponse res) throws Exception {

        // extract parameters
        Map origParamMap = req.getParameterMap();
        Map<String, String> paramMap = new HashMap<String,String>();

        // parameters could be upper or lower case
        for (Object p : origParamMap.keySet()) {
            if (p instanceof String) {
                paramMap.put(((String)p).toUpperCase(), (((String[])origParamMap.get(p))[0]).trim());
            }
        }

        String mimeType = "text/xml";
        res.setContentType(mimeType);

        //
        TableMapper tableMapper = new TableMapper();
        VODataProvider dataProvider = new RemoteDataProvider(tableMapper, paramMap);
        VOTableWriter voWriter = new VOTableWriter(dataProvider);
        voWriter.sendData(new PrintStream(res.getOutputStream()), paramMap);
        /*
        Config config = getConfigMapper().getConfig(service, dataset);
        if (config == null) {
            List<String> services = getAvailableServices();
            VOTableWriter.sendError(new PrintStream(res.getOutputStream()), "Unsupported SERVICE or DATASET. Supported (SERVICE DATASET) pairs: "+CollectionUtil.toString(services)+".");
            return;
        }
        TableMapper tableMapper;
        try {
            tableMapper = getTableMapper(config);
        } catch (Exception e) {
            VOTableWriter.sendError(new PrintStream(res.getOutputStream()), e.getMessage());
            return;
        }


        if (tableMapper != null) {
            String format = getFormat(paramMap, tableMapper);
            VODataProvider dataProvider = new RemoteDataProvider(tableMapper, paramMap);
            dataProvider.setTestMode(format.equalsIgnoreCase(FORMAT_TEST));
            VOTableWriter voWriter = new VOTableWriter(dataProvider);

            if (format.equalsIgnoreCase(FORMAT_METADATA)) {
                voWriter.sendMetadata(new PrintStream(res.getOutputStream()), tableMapper);
            } else {
                voWriter.sendData(new PrintStream(res.getOutputStream()), paramMap);
            }
        } else {
            VOTableWriter.sendError(new PrintStream(res.getOutputStream()), "Can not obtain mapping info for service "+service);
        }
        */
    }

    private static ConfigMapper getConfigMapper() {
        // find xml file, containing service mappings
        //appConfigDir+"/xml/services.xml"
        File xmlFile = new File(ServerContext.getConfigFile("xml"),CONFIG_MAPPER_FILE);

        if (!xmlFile.exists() || !xmlFile.canRead()) {
            throw new IllegalArgumentException(xmlFile.getAbsolutePath()+"is not found");
        }

        LOG.briefDebug("Services config file = "+xmlFile.getAbsolutePath());

        ConfigMapper configMapper;
        InputStream fileStream = null;
        CacheKey cacheKey = new StringKey("configMapper");
        Cache cache = CacheManager.getCache(CACHE_NAME);
        Object cachedMapper = cache.get(cacheKey);
        if (cachedMapper != null && cachedMapper instanceof ConfigMapper) {
            LOG.briefDebug("Using cached service mapper");
            configMapper = (ConfigMapper)cachedMapper;
        } else {
            try {
                LOG.briefDebug("Parsing configuration from file");
                XStream xstream = new XStream();

                // since XML contains 'id', must alias the System's 'id'
                xstream.aliasSystemAttribute("refid", "id");

                // process annotations & register custom converters
                xstream.processAnnotations(ConfigMapper.class);

                fileStream = new FileInputStream(xmlFile);
                configMapper = (ConfigMapper) xstream.fromXML(fileStream);
                cache.put(cacheKey, configMapper);

            } catch (Exception e) {
                LOG.error(e, "Error reading xml file: " + xmlFile.getAbsolutePath());
                throw new IllegalArgumentException("Error reading xml file: " + xmlFile.getAbsolutePath()+" - "+e.getMessage());
                //configMapper = null;
            } finally {
                if (fileStream != null) {
                    try {
                        fileStream.close();
                    } catch (Exception e) {
                        LOG.error("Unable to close stream: "+e.getMessage());
                    }
                }
            }
        }
        return configMapper;
    }

    private static TableMapper getTableMapper(Config config) {


        TableMapper tableMapper;
        InputStream fileStream = null;
        CacheKey cacheKey = new StringKey(config.toString());
        Cache cache = CacheManager.getCache(CACHE_NAME);
        Object cachedMapper = cache.get(cacheKey);
        if (cachedMapper != null && cachedMapper instanceof TableMapper) {
            LOG.briefDebug("Getting table mapper from cache");
            tableMapper = (TableMapper)cachedMapper;
        } else {
            File xmlFile = new File(ServerContext.getConfigFile("xml"), config.getTableMapperFile());
            try {
                LOG.briefDebug("Parsing configuration from file: "+config.getTableMapperFile());


                XStream xstream = new XStream();

                // since XML contains 'id', must alias the System's 'id'
                xstream.aliasSystemAttribute("refid", "id");

                // process annotations & register custom converters
                xstream.processAnnotations(TableMapper.class);

                fileStream = new FileInputStream(xmlFile);
                tableMapper = (TableMapper) xstream.fromXML(fileStream);
                cache.put(cacheKey, tableMapper);

            } catch (Exception e) {
                LOG.error(e, "Error reading xml file: " + xmlFile.getAbsolutePath());
                throw new IllegalArgumentException("Error reading xml file: " + config.getTableMapperFile()+" - "+e.getMessage());
                //tableMapper = null;
            } finally {
                if (fileStream != null) {
                    try {
                        fileStream.close();
                    } catch (Exception e) {
                        LOG.error("Unable to close stream: "+e.getMessage());
                    }
                }
            }
        }
        return tableMapper;
    }

    private static List<String> getAvailableServices() {
        Collection<Config> configs = getConfigMapper().getAllConfigs();
        List<String> allServices = new ArrayList<String>();
        for (Config config : configs) {
            allServices.add("("+config.getServiceName()+" "+config.getDataset()+")");
        }
        return allServices;
    }

    private static String getFormat(Map<String, String> paramMap, TableMapper tableMapper) {
        // The service must support a parameter with the name FORMAT to indicate the desired format
        // or formats of the images referenced by the output table. The value is a comma-delimited list
        // where each element can be any recognized MIME-type.
        String format;
        if (paramMap.size() < 2) {
            format = FORMAT_METADATA;
        } else {
            if(paramMap.containsKey(PARAM_FORMAT)) {
                format = paramMap.get(PARAM_FORMAT);
                if (format == null ) format = FORMAT_ALL;
                else if (!format.equalsIgnoreCase(FORMAT_ALL) && !format.equalsIgnoreCase(FORMAT_TEST)) {
                    // check that format is supported by the service
                    if (!isFormatSupported(format, tableMapper)) format = FORMAT_METADATA;
                }
            } else {
                format = FORMAT_ALL;
            }
        }
        return format;
    }

    private static boolean isFormatSupported(String format, TableMapper tableMapper) {
        Collection<VoServiceParam> params = tableMapper.getVoParams();
        for (VoServiceParam p : params) {
            if (p.getName().equals(PARAM_FORMAT)) {
                VoValues vovalues = p.getVoValues();
                if (vovalues != null) {
                    List<VoOption> vooptions = vovalues.getVoOptions();
                    if (vooptions != null) {
                        for (VoOption o : vooptions) {
                            if (format.contains(o.getValue())) { // format could be comma-delimited list
                                return true;
                            }
                        }
                        return false;
                    }
                }
            }
        }
        // if no options are defined for PARAM_FORMAT
        return true;
    }


    protected void doPost(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        httpServletResponse.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
    }

    protected void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        try {
            processRequest(httpServletRequest, httpServletResponse);
        } catch (Exception e) {
            Logger.error("GET failed", e.getMessage());
            throw new ServletException(e);
        }
    }

    /**
    private static class AssertLogger implements Assert.Logger {
        private final Logger.LoggerImpl _log= Logger.getLogger();
        public void log(String... messages) { _log.warn(messages); }
    }
    **/



}
