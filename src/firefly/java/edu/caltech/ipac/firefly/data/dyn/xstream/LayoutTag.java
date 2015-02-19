/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.data.dyn.xstream;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import java.io.Serializable;

// custom converter used (LayoutConverter) - no annotations needed within class
@XStreamAlias("Layout")
public class LayoutTag implements Serializable {

    // xml element 'SplitPanel'
    protected LayoutTypeTag layoutTypeTag;


    public LayoutTypeTag getLayoutType() {
        return layoutTypeTag;
    }

    public void setLayoutType(LayoutTypeTag layoutTypeTag) {
        this.layoutTypeTag = layoutTypeTag;
    }

}

