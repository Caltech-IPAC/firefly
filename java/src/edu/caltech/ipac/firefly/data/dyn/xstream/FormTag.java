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
