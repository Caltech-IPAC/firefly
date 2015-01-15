/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.data.dyn.xstream;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import java.util.LinkedHashSet;


// custom converter used (SplitPanelConverter) - no annotations needed within class
@XStreamAlias("SplitPanel")
public class SplitPanelTag extends LayoutTypeTag {

    // xml element '(North|West|East|South)*, Center'
    protected LinkedHashSet<LayoutAreaTag> layoutAreas;


    public LinkedHashSet<LayoutAreaTag> getLayoutAreas() {
        return layoutAreas;
    }

    public void addLayoutArea(LayoutAreaTag item) {
        if (layoutAreas == null) {
            layoutAreas = new LinkedHashSet<LayoutAreaTag>();
        }
        
        layoutAreas.add(item);
    }

}

