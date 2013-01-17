package edu.caltech.ipac.voservices.server.configmapper;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

import java.io.Serializable;


/**
 * @author tatianag
 *         $Id: Config.java,v 1.1 2010/12/06 13:13:52 tatianag Exp $
 */
@XStreamAlias("Config")
public class Config implements Serializable {

    // xml attribute 'servicename'
    @XStreamAsAttribute
    protected String servicename;

    // xml attribute 'dataset'
    @XStreamAsAttribute
    protected String dataset;


    // xml attribute 'tablemapperfile'
    @XStreamAsAttribute
    protected String tablemapperfile;

    // xml attribute 'desc'
    @XStreamAsAttribute
    protected String desc;

    public Config() {}

    public String getServiceName() { return this.servicename; }
    public String getDataset() { return this.dataset; }
    public String getTableMapperFile() { return this.tablemapperfile; }
    public String getDesc() {return this.desc; }

    public String toString() {
        return getServiceName()+"/"+getDataset()+" "+getTableMapperFile();    
    }
}
