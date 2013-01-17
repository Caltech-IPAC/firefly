package edu.caltech.ipac.firefly.data.dyn.xstream;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import edu.caltech.ipac.firefly.data.dyn.DynUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


@XStreamAlias("Download")
public class DownloadTag implements Serializable {

    // xml attribute 'id'
    @XStreamAsAttribute
    protected String id;

    // xml attribute 'title'
    @XStreamAsAttribute
    protected String title;

    // xml attribute 'filePrefix'
    @XStreamAsAttribute
    protected String filePrefix;

    // xml attribute 'titlePrefix'
    @XStreamAsAttribute
    protected String titlePrefix;

    // xml attribute 'maxRows'
    @XStreamAsAttribute
    protected String maxRows;

    // xml element 'Param*'
    @XStreamImplicit
    protected List<ParamTag> paramTags;

    // xml element 'SearchFormParam*'
    @XStreamImplicit
    protected List<SearchFormParamTag> searchFormParamTags;

    // xml element 'FieldGroup*'
    @XStreamImplicit
    protected List<FieldGroupTag> fieldGroupTags;

    // xml element 'Form'
    @XStreamAlias("Form")
    protected FormTag formTag;

    public String getId() {
        return id;
    }
    public void setId(String value) {
        this.id = value;
    }


    public String getTitle() {
        if (title == null)
            title = DynUtils.DEFAULT_DOWNLOAD_TITLE;
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }


    public String getFilePrefix() {
        if (filePrefix == null)
            filePrefix = DynUtils.DEFAULT_FILE_PREFIX;
        return filePrefix;
    }
    public void setFilePrefix(String filePrefix) {
        this.filePrefix = filePrefix;
    }


    public String getMaxRows() {
        return maxRows;
    }
    public void setMaxRows(String maxRows) {
        this.maxRows = maxRows;
    }

    public String getTitlePrefix() {
        if (titlePrefix == null)
            titlePrefix = DynUtils.DEFAULT_FILE_PREFIX;
        return titlePrefix;
    }
    public void setTitlePrefix(String titlePrefix) {
        this.titlePrefix = titlePrefix;
    }

    public List<ParamTag> getParams() {
        if (paramTags == null) {
            paramTags = new ArrayList<ParamTag>();
        }
        return this.paramTags;
    }

    public List<SearchFormParamTag> getSearchFormParams() {
        if (searchFormParamTags == null) {
            searchFormParamTags = new ArrayList<SearchFormParamTag>();
        }
        return this.searchFormParamTags;
    }

    public List<FieldGroupTag> getFieldGroups() {
        if (fieldGroupTags == null) {
            fieldGroupTags = new ArrayList<FieldGroupTag>();
        }
        return this.fieldGroupTags;
    }

    public FormTag getFormTag() {
        return formTag;
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
