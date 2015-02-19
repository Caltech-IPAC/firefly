/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.data.fuse.config;

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
/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
* INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
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
