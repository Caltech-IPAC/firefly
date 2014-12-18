package edu.caltech.ipac.firefly.fuse.data.config;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

import java.io.Serializable;

/**
 * Date: 2/13/14
 *
 * @author loi
 * @version $Id: $
 */
@XStreamAlias("TAP")
public class TapTag implements Serializable {

    @XStreamAsAttribute
    private String baseUrl;

    @XStreamAsAttribute
    private String queryStr;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getQueryStr() {
        return queryStr;
    }

    public void setQueryStr(String queryStr) {
        this.queryStr = queryStr;
    }
}
