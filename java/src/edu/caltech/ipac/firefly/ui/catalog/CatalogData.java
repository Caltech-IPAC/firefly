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
