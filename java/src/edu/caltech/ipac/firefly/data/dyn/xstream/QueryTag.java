package edu.caltech.ipac.firefly.data.dyn.xstream;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.dyn.DynUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

// custom converter used (QueryConverter) - no annotations needed within class
@XStreamAlias("Query")
public class QueryTag extends XidBaseTag {

    // xml attribute 'id'
    protected String id;

    // xml attribute 'searchProcessorId'
    protected String searchProcessorId;

    // xml element 'Constraints*'
    protected ConstraintsTag constraintsTag;

    // xml element 'Param*'
    protected HashMap<String, ParamTag> paramMap = new HashMap<String, ParamTag>();

    // xml element 'Metadata?'
    protected HashMap<String, String> metaMap;

    // xml element 'Download?'
    protected DownloadTag downloadTag;

    public String getId() {
        return id;
    }

    public void setId(String value) {
        this.id = value;
    }


    public String getSearchProcessorId() {
        return searchProcessorId;
    }

    public void setSearchProcessorId(String value) {
        this.searchProcessorId = value;
    }


    public ConstraintsTag getConstraints() {
        return this.constraintsTag;
    }

    public void setConstraints(ConstraintsTag value) {
        constraintsTag = value;
    }


    public boolean containsParam(String key) {
        return paramMap != null && paramMap.containsKey(key);
    }

    public String getParam(String key) {
        ParamTag p = paramMap.get(key);
        return p == null ? null : p.getValue();
    }
    
    public List<ParamTag> getParams() {
        return new ArrayList<ParamTag>(paramMap.values());
    }

    public void addParam(ParamTag p) {
        paramMap.put(p.getKey(), p);
    }


    public void removeServerOnlyParams() {
        for (ParamTag pt : getParams()) {
            if (pt.getServerOnly()) {
                paramMap.remove(pt.getKey());
            }
        }
    }

    public List<ParamTag> getMetadata() {
        if (metaMap == null) {
            metaMap = new HashMap<String, String>();
        }

        return DynUtils.convertParams(metaMap);
    }

    public void addMetadata(ParamTag p) {
        if (metaMap == null) {
            metaMap = new HashMap<String, String>();
        }

        metaMap.put(p.getKey(), p.getValue());
    }

    public void clearMetadata() {
        metaMap = new HashMap<String, String>();
    }


    public DownloadTag getDownload() {
        /*if (downloadTag == null) {
            downloadTag = new DownloadTag();
        }*/
        return this.downloadTag;
    }

    public void setDownload(DownloadTag value) {
        downloadTag = value;
    }
}

