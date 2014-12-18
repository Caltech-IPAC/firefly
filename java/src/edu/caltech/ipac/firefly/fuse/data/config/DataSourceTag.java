package edu.caltech.ipac.firefly.fuse.data.config;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import edu.caltech.ipac.firefly.data.dyn.xstream.ParamTag;

import java.io.Serializable;
import java.util.List;

/**
 * Date: 2/12/14
 *
 * @author loi
 * @version $Id: $
 */
@XStreamAlias("DataSource")
public class DataSourceTag implements Serializable {
    @XStreamAsAttribute
    private String searchProcId;

    @XStreamAsAttribute
    private String argColUrl;

    @XStreamAsAttribute
    private String argHeaderUrl;

    @XStreamAlias("IBE")
    private IbeTag ibe;
    @XStreamAlias("TAP")
    private TapTag tap;

    @XStreamImplicit
    private List<ParamTag> param;

    public String getSearchProcId() {
        return searchProcId;
    }

    public void setSearchProcId(String searchProcId) {
        this.searchProcId = searchProcId;
    }

    public IbeTag getIbe() {
        return ibe;
    }

    public void setIbe(IbeTag ibe) {
        this.ibe = ibe;
    }

    public TapTag getTap() {
        return tap;
    }

    public void setTap(TapTag tap) {
        this.tap = tap;
    }

    public List<ParamTag> getParam() {
        return param;
    }

    public void setParam(List<ParamTag> param) {
        this.param = param;
    }
}
