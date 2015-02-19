/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.catalog;

import edu.caltech.ipac.firefly.data.table.BaseTableData;
import edu.caltech.ipac.firefly.data.table.DataSet;

import java.util.ArrayList;
import java.util.List;
/**
 * User: roby
 * Date: Oct 30, 2009
 * Time: 12:52:28 PM
 */


/**
 * @author Trey Roby
 */
public class CatalogData {

    private List<Proj> _projects= new ArrayList<Proj>();
    private DataSet _inputDataSet;

//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public CatalogData(DataSet dataset) {
        _inputDataSet= dataset;
        parseDataSet(dataset);
    }

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    public List<Proj> getProjects() { return _projects; }
    public DataSet getDataSet() { return _inputDataSet; }

//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    private void parseDataSet(DataSet dataset) {
        BaseTableData data= (BaseTableData)dataset.getModel();
        List<BaseTableData.RowData> rows= data.getRows();
        Proj proj;
        Catagory catagory;
        Catalog cat;

        String projectShort;
        String catagoryStr;

        for(BaseTableData.RowData row : rows) {
            projectShort= row.getValue("projectshort");
            if (!projectShort.toLowerCase().contains("research")) {
                catagoryStr= row.getValue("subtitle");

                proj= findProj(projectShort);
                catagory= findCatagory(catagoryStr, proj);

//                cat= new Catalog(proj,catagory, row);
                cat= new Catalog(row);
                catagory.addCatalog(cat);
            }
        }
    }

    private Proj findProj(String shortName) {
        Proj proj= new Proj(shortName);
        if (_projects.contains(proj)) {
            proj= _projects.get(_projects.indexOf(proj));
        }
        else {
            _projects.add(proj);
        }
        return proj;
    }

    private Catagory findCatagory(String name, Proj proj) {
        Catagory catagory= new Catagory(name);
        if (proj.contains(catagory)) {
            catagory= proj.get(proj.indexOf(catagory));
        }
        else {
            proj.addCatagory(catagory);
        }
        return catagory;
    }

}

