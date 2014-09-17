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
