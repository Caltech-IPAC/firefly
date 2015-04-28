package edu.caltech.ipac.firefly.fftools;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayNumber;
import edu.caltech.ipac.firefly.data.JscriptRequest;
import edu.caltech.ipac.firefly.data.table.*;
import edu.caltech.ipac.firefly.visualize.ui.ReactUIWrapper;

/**
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 *
 * @author tatianag
 */
public class HistogramJSInterface {
    public static void plotHistogram(JscriptRequest jspr, String div) {
        TableDataView.Column[] cols = new TableDataView.Column[2];
        cols[0] = new BaseTableColumn("bincenter", TableDataView.Align.LEFT, 20, true);
        cols[1] = new BaseTableColumn("count", TableDataView.Align.LEFT, 10, true);

        DataSet ds = new DataSet(cols);
        BaseTableData model = (BaseTableData)ds.getModel();
        model.addRow(new String[]{"-2.3138013781265", "1"});
        model.addRow(new String[]{"-2.0943590644815", "4"});
        model.addRow(new String[]{"-1.8749167508365", "11"});
        model.addRow(new String[]{"-1.6554744371915", "12"});
        model.addRow(new String[]{"-1.4360321235466", "18"});
        model.addRow(new String[]{"-1.2165898099016", "18"});
        model.addRow(new String[]{"-0.99714749625658", "21"});
        model.addRow(new String[]{"-0.55826286896661", "36"});
        model.addRow(new String[]{"-0.33882055532162", "40"});
        model.addRow(new String[]{"-0.11937824167663", "51"});
        model.addRow(new String[]{"0.10006407196835", "40"});
        model.addRow(new String[]{"0.31950638561334", "42"});
        model.addRow(new String[]{"0.53894869925832", "36"});
        model.addRow(new String[]{"0.75839101290331", "40"});
        model.addRow(new String[]{"0.9778333265483", "36"});
        model.addRow(new String[]{"1.1972756401933", "23"});
        model.addRow(new String[]{"1.4167179538383", "18"});
        model.addRow(new String[]{"1.6361602674833", "9"});
        model.addRow(new String[]{"1.8556025811282", "12"});
        model.addRow(new String[]{"2.0750448947732", "3"});
        model.addRow(new String[]{"2.2944872084182", "4"});

        JsArray<JsArrayNumber> jsArrData = JsArray.createArray().cast();

        for (TableData.Row r : model.getRows()) {
            JsArrayNumber jsArrNum = JsArrayNumber.createArray().cast();
            jsArrNum.push(Double.parseDouble(r.getValue(0).toString()));
            jsArrNum.push(Double.parseDouble(r.getValue(1).toString()));
            jsArrData.push(jsArrNum);
        }

        ReactUIWrapper.ReactJavaInterface reactInterface = getReactInterface();
        reactInterface.createHistogram(jsArrData, div);
    }

    public static native ReactUIWrapper.ReactJavaInterface getReactInterface() /*-{
        if ($wnd.firefly && $wnd.firefly.gwt) {
            return new $wnd.firefly.gwt.ReactJavaInterface();
        }
        else {
            return null;
        }
    }-*/;
}
