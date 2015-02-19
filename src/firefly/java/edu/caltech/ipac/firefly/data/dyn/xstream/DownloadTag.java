/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
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

