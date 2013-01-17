package edu.caltech.ipac.heritage.searches;

import edu.caltech.ipac.firefly.commands.DownloadCmd;

import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.DataSet;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.ui.table.DownloadSelectionIF;
import edu.caltech.ipac.firefly.ui.table.SelectableTablePanel;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.ui.table.builder.BaseTableConfig;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.util.event.WebEventManager;
import edu.caltech.ipac.firefly.visualize.ActiveTarget;
import edu.caltech.ipac.firefly.visualize.VisUtil;
import edu.caltech.ipac.heritage.data.entity.DataType;
import edu.caltech.ipac.heritage.ui.DownloadSelectionDialog;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Date: Jun 8, 2009
 *
 * @author loi
 * @version $Id: HeritageSearch.java,v 1.30 2012/09/18 23:08:08 tatianag Exp $
 */
public abstract class HeritageSearch <SReq extends TableServerRequest> extends BaseTableConfig<SReq> {

    public static final String IRS_ENHANCED_SEARCH_ID = "IrsEnhancedQuery";
    public static final String DATA_TYPE= "DataType";

    public static final String FILTER_AOR_KEYS = "AorKeys";
    public static final String FILTER_PBCD_KEYS = "PbcdKeys";


    private DataType dtype;
    private String shortDesc;
    private boolean isInit;
    private TableDataView dataset;
    private SelectableTablePanel table;
    private boolean updateActiveTarget;

    public HeritageSearch(DataType dtype,
                          String shortDesc,
                          SReq searchReq,
                          String dlFilePrefix, String dlTitlePrefix) {
        this(dtype,shortDesc,searchReq,dlFilePrefix, dlTitlePrefix ,true);
    }

    public HeritageSearch(DataType dtype,
                          String shortDesc,
                          SReq searchReq,
                          String dlFilePrefix, String dlTitlePrefix,
                          boolean updateActiveTarget) {
        super(searchReq, dtype == null ? "" : dtype.getTitle(), shortDesc, "to be replaced", dlFilePrefix, dlTitlePrefix);
        this.dtype = dtype;
        this.shortDesc = shortDesc;
        this.updateActiveTarget= updateActiveTarget ;

    }

    @Override
    public DownloadSelectionIF getDownloadSelectionIF() {
        DownloadSelectionIF dsif = super.getDownloadSelectionIF();
        if (dsif == null) {
            if (dtype != null) {
                DownloadSelectionDialog.DialogType dialogType = null;
                if(dtype.equals(DataType.AOR)) {
                    dialogType = DownloadSelectionDialog.DialogType.AOR;
                } else if (dtype.equals(DataType.BCD)) {
                    dialogType = DownloadSelectionDialog.DialogType.BCD;
                } else if (dtype.equals(DataType.PBCD)) {
                    dialogType = DownloadSelectionDialog.DialogType.POSTBCD;
                } else if (dtype.equals(DataType.IRS_ENHANCED)) {
                    dialogType = DownloadSelectionDialog.DialogType.IRS_ENHANCED;
                } else if (dtype.equals(DataType.SM)) {
                    dialogType = DownloadSelectionDialog.DialogType.SM;
                } else {
                    // do not allow download for other data types
                    return null;
                }
                DownloadSelectionDialog dsd = new DownloadSelectionDialog(dialogType, getDownloadRequest(), getLoader().getCurrentData());
                this.setDownloadSelectionIF(dsd);
                return dsd;
            }
        }
        return dsif;
    }

    public String getShortDesc() {
        return shortDesc;
    }


    public TableDataView getTableDataView() {
        return dataset;
    }


    public void onLoad(TableDataView data) {
        if (!isInit) {
//            if (dtype != null) {
//                if (dtype.equals(DataType.BCD)) {
//                    setupColumns(data, BCD_LIST);
//                } else if (dtype.equals((DataType.PBCD))) {
//                    setupColumns(data, PBCD_LIST);
//                }
//            }
            isInit = true;
            dataset = data;
        }
    }

    public DownloadCmd makeDownloadCmd() {
        DownloadSelectionIF dsd = this.getDownloadSelectionIF();
        return dsd == null ? null : new DownloadCmd(getLoader().getCurrentData(), dsd);
    }

    public void setDataType(DataType dtype) {
        this.dtype = dtype;
    }

    public void setShortDesc(String shortDesc) {
        this.shortDesc = shortDesc;
    }


    public void setTable(SelectableTablePanel table) {
        this.table = table;
        WebEventManager evm= table.getEventManager();
        evm.addListener(TablePanel.ON_INIT, new WebEventListener(){
            public void eventNotify(WebEvent ev) {
                if (HeritageSearch.this.table.getDataset() != null) {
                    TableMeta meta= HeritageSearch.this.table.getDataset().getMeta();
                    meta.setAttribute(DATA_TYPE, dtype.toString());
                }
            }
        });
        evm.addListener(SelectableTablePanel.ON_PAGE_LOAD, new WebEventListener(){
            public void eventNotify(WebEvent ev) {
                if (dtype == DataType.BCD || dtype == DataType.PBCD) {
                    calculateCentralPointFromResult(HeritageSearch.this.table);
                }
            }
        });
    }

    public SelectableTablePanel getTable() {
        return table;
    }

    abstract public String getDownloadFilePrefix();
    abstract public String getDownloadTitlePrefix();

//====================================================================
//
//====================================================================

    private void setupColumns(TableDataView source, List<String> limiter) {
        for ( DataSet.Column col : source.getColumns()) {
            if (!limiter.contains(col.getName())) {
                col.setHidden(true);
            }
        }
        for(int i = limiter.size()-1; i >=0; i--) {
            source.moveColumn(source.findColumn(limiter.get(i)), 0);
        }
    }


    private void calculateCentralPointFromResult(SelectableTablePanel table) {
        WorldPt[] WPT ;
        ArrayList<WorldPt> ar ;
        int raDec = 0;
        List<TableData.Row> rows = table.getTable().getRowValues();
        if (rows != null )  {
            TableMeta meta= table.getDataset().getMeta();
            meta.setAttribute(DATA_TYPE, dtype.toString());
            if (rows.size() > 0) {
                ar  = new ArrayList<WorldPt>();
                for (int rowNum = 0; rowNum < rows.size(); rowNum++) {
                    try {
                        for(int i= 1; (i<=4); i++) {
                            String raS = String.valueOf(rows.get(rowNum).getValue("ra"+i));
                            double ra = Double.parseDouble(raS);
                            String decS = String.valueOf(rows.get(rowNum).getValue("dec"+i));
                            double dec = Double.parseDouble(decS);
                            if (ra != -1 && dec != -1) {
                                ar.add( new WorldPt(ra,dec));      // using  CoordinateSys.EQ_J2000
                                raDec = raDec+1;
                            }
                        }
                    } catch(NumberFormatException ex) {
                        // skip bad data
                    }

                }
                WPT = new WorldPt[ar.size()];
                for (int i = 0; ar.size() > i; i++) {
                    WPT[i] = ar.get(i);
                }

                VisUtil.CentralPointRetval result = VisUtil.computeCentralPointAndRadius(Arrays.asList(WPT));
                WorldPt wp = result.getWorldPt();
                Double radiusD = result.getRadius();
                float radius = radiusD.floatValue();

//                meta.setWorldPtAttribute(MetaConst.WORLD_PT_KEY, wp);
//                meta.setAttribute(MetaConst.RADIUS_KEY, radius);
                if (updateActiveTarget) {
                    ActiveTarget.getInstance().setActive(wp);
                    ActiveTarget.getInstance().setRadius(radius);
                }
            }
            else {
//                meta.setAttribute(MetaConst.RADIUS_KEY, 0F);
                if (updateActiveTarget) {
                    ActiveTarget.getInstance().setRadius(0F);
                }
            }
        }
    }
}
/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
* INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
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
