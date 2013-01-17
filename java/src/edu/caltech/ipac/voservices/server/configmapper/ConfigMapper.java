package edu.caltech.ipac.voservices.server.configmapper;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import edu.caltech.ipac.firefly.server.util.Logger;

import java.io.File;
import java.io.Serializable;
import java.util.*;

/**
 * @author tatianag
 *         $Id: ConfigMapper.java,v 1.1 2010/12/06 13:13:52 tatianag Exp $
 */
@XStreamAlias("ConfigMapper")
public class ConfigMapper implements Serializable {

    private static final Logger.LoggerImpl LOG = Logger.getLogger();

    // list of config objects
    // xml element 'Config+'
    @XStreamImplicit
    protected List<Config> configList = new ArrayList<Config>();

    public ConfigMapper() {}

    public boolean validate(File tableMapperDir){
        boolean validated = true;
        for (Config config : configList) {
            if (config.getServiceName()==null ||
                    config.getDataset()==null ||
                    config.getTableMapperFile()==null) {
                LOG.error("Insufficiently defined service: "+config.toString());
                validated = false;
            }
            File tableMapperFile = new File(tableMapperDir, config.getTableMapperFile());
            if (!tableMapperFile.exists() || !tableMapperFile.canRead()) {
                LOG.error("Unreadable table mapper: "+config.toString());
                validated = false;
            }
        }
        return validated;
    }

    public Config getConfig(String service, String dataset) {
        for (Config config : configList) {
            if (config.getServiceName().equals(service) &&
                    config.getDataset().equals(dataset)) {
                return config;
            }
        }
        return null;
    }

    public Collection<Config> getAllConfigs() {
        return Collections.unmodifiableCollection(configList);
    }
}
