/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.searchui;
/**
 * User: roby
 * Date: 2/14/14
 * Time: 11:35 AM
 */


import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.data.DataSetInfo;
import edu.caltech.ipac.firefly.data.Param;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * @author Trey Roby
 */
public class SpectrumSelectUI implements DataTypeSelectUI {

    private DataSetInfo dsInfo;


    public SpectrumSelectUI(DataSetInfo dsInfo) {
        this.dsInfo = dsInfo;
    }


    public Widget makeUI() {
        Label label= new Label("Spectrum View Here for "+dsInfo.getUserDesc());
        return label;
    }

    public String getDataDesc() {
        return "Spectrum Data"; //TODO: put some project information here
    }


    public List<Param> getFieldValues() {
        return Collections.emptyList(); //todo
    }

    public void setFieldValues(List<Param> list) {
        //todo
    }

    public boolean validate() {
        return true; //todo
    }

    public String makeRequestID() {
        return "SomeImageRequestID";
    }

    public Iterator<Widget> iterator() {
        List<Widget> l= Collections.emptyList();
        return l.iterator();
    }

    public void add(Widget w) { throw new UnsupportedOperationException("operation not allowed"); }
    public void clear() { throw new UnsupportedOperationException("operation not allowed"); }
    public boolean remove(Widget w) { throw new UnsupportedOperationException("operation not allowed"); }



}

