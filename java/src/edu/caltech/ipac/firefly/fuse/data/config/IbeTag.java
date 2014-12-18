package edu.caltech.ipac.firefly.fuse.data.config;

/**
 * Date: 2/13/14
 *
 * @author loi
 * @version $Id: $
 */

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

import java.io.Serializable;

@XStreamAlias("IBE")
public class IbeTag implements Serializable {

    @XStreamAsAttribute
    private String baseUrl;

    @XStreamAsAttribute
    private String action;

    @XStreamAsAttribute
    private String mission;

    @XStreamAsAttribute
    private String dataSet;

    @XStreamAsAttribute
    private String tableName;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getMission() {
        return mission;
    }

    public void setMission(String mission) {
        this.mission = mission;
    }

    public String getDataSet() {
        return dataSet;
    }

    public void setDataSet(String dataSet) {
        this.dataSet = dataSet;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }
}
