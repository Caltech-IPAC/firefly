package edu.caltech.ipac.firefly.data.dyn.xstream;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


@XStreamAlias("Form")
public class FormTag implements Serializable {

    // xml element 'FormEventWorker*'
    @XStreamImplicit
    protected List<FormEventWorkerTag> formEventWorkerTags;

    // xml element 'FieldGroup+'
    @XStreamImplicit
    protected List<FieldGroupTag> fieldGroupTags;

    // xml attribute 'MinSize'
    @XStreamAsAttribute
    protected String minSize;    

    // xml attribute 'title'
    @XStreamAsAttribute
    protected String title;

    // xml attribute 'helpId'
    @XStreamAsAttribute
    protected String helpId;

    public List<FormEventWorkerTag> getFormEventWorkerTags() {
        if (formEventWorkerTags == null) {
            formEventWorkerTags = new ArrayList<FormEventWorkerTag>();
        }
        return formEventWorkerTags;
    }

    public List<FieldGroupTag> getFieldGroups() {
        if (fieldGroupTags == null) {
            fieldGroupTags = new ArrayList<FieldGroupTag>();
        }
        return fieldGroupTags;
    }

    public String getMinSize() {
        return minSize;
    }

    public void setMinSize(String minSize) {
        this.minSize = minSize;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getHelpId() {
        return helpId;
    }

    public void setHelpId(String helpId) {
        this.helpId = helpId;
    }
}

