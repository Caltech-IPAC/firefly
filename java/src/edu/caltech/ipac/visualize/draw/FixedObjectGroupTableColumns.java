package edu.caltech.ipac.visualize.draw;


import edu.caltech.ipac.gui.table.AbstractModelColumn;
import edu.caltech.ipac.gui.table.BoolEditor;
import edu.caltech.ipac.gui.table.BoolRender;
import edu.caltech.ipac.gui.table.ComboEditor;
import edu.caltech.ipac.gui.table.ComboRender;
import edu.caltech.ipac.gui.table.ModelColumn;
import edu.caltech.ipac.gui.table.SortInfo;
import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.util.ComparisonUtil;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.action.ClassProperties;

import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FixedObjectGroupTableColumns {


   private final static ClassProperties _prop= new ClassProperties(
                                        FixedObjectGroupTableColumns.class);

    private final String ENABLED_COL     = _prop.getColumnName("on");
    private final String HILIGHT_COL     = _prop.getColumnName("hilight");
    private final String SHAPE_COL       = _prop.getColumnName("shape");
    private final String SHOW_NAME_COL   = _prop.getColumnName("showName");
    private final String TNAME_COL       = _prop.getColumnName("targetName");
    private final String USER_RA_COL     = _prop.getColumnName("userRa");
    private final String USER_DEC_COL    = _prop.getColumnName("userDec");
    //private final String USER_LON_COL    = _prop.getColumnName("userLon");
    //private final String USER_LAT_COL    = _prop.getColumnName("userLat");

    private final String USER_RA_TIP     = _prop.getTip("userRa");
    private final String USER_DEC_TIP    = _prop.getTip("userDec");
    private final String ENABLED_TIP     = _prop.getTip("on");
    private final String HILIGHT_TIP     = _prop.getTip("hilight");
    private final String SHOW_NAME_TIP   = _prop.getTip("showName");
    private final String TNAME_TIP       = _prop.getTip("targetName");
    private final String SHAPE_TIP       = _prop.getTip("shape");

    private final FixedObjectGroup     _fixedGroup;
    private List<ModelColumn>    _columns= null;

    private final SkyShape _shapes[];
    private boolean _onlyImportantCol;
    private boolean _showPlottingCol;
    private Font _font;



    public FixedObjectGroupTableColumns(FixedObjectGroup fixedGroup,
                                        boolean          onlyImportantCol,
                                        boolean          showPlottingCol,
                                        SkyShape         shapes[]) {
        this(fixedGroup, onlyImportantCol, showPlottingCol, shapes, null);
    }

    public FixedObjectGroupTableColumns(FixedObjectGroup fixedGroup,
                                        boolean          onlyImportantCol,
                                        boolean          showPlottingCol,
                                        SkyShape         shapes[],
                                        Font font) {
        _fixedGroup= fixedGroup;
        _shapes= shapes;
        _font=font;
        _onlyImportantCol= onlyImportantCol;
        _showPlottingCol= showPlottingCol;
    }


    public FixedObjectGroupTableColumns(FixedObjectGroup fixedGroup,
                                        SkyShape         shapes[]) {
        this(fixedGroup,false,true,shapes);
    }

    public FixedObjectGroup getFixedGroup() { return _fixedGroup; }

    public final List<ModelColumn> getColumns() {
        Color c= Color.lightGray;
        if (_columns == null) {
            _columns= makeColumns(c);
        }
        return _columns;
    }

    protected List<ModelColumn> makeColumns(Color c) {
        List<ModelColumn> list= new ArrayList<ModelColumn>(11);

        if (_showPlottingCol) {
            list.add(new OnColumn(c));
            list.add(new HighlightColumn(c));
            if (_shapes!=null) list.add(new ShapeColumn(c));
        }

        list.add(new TargetNameColumn(c));

        if (_showPlottingCol) list.add(new ShowNameColumn(c));

        addRaDecColumns(list, c);

        addExtraDataColumns(list, c);
        return list;
    }


    protected List<ModelColumn> makeSimpleColumns(Color c) {
        List<ModelColumn> list= new ArrayList<ModelColumn>(11);
        list.add(new TargetNameColumn(c));
        addRaDecColumns(list, c);
        addExtraDataColumns(list, c);
        return list;
    }


    protected void addExtraDataColumns(List<ModelColumn> list, Color c) {
        DataType dataDef[]= _fixedGroup.getExtraDataDefs();
        FontRenderContext frc = new FontRenderContext(null, false, false);
        ExtraDataColumn edc;
        if (dataDef!=null) {
            for(DataType data: dataDef) {
                if (!_onlyImportantCol || (data.getImportance()==DataType.Importance.HIGH)) {
                    edc = new ExtraDataColumn(c,data);
                    if (_font != null && data.hasFormatInfo()) {
                        int width = data.getFormatInfo().getWidth();
                        char ch[] = new char[width];
                        Arrays.fill(ch, '0');
                        edc.setPreferredWidth((int)_font.getStringBounds(new String(ch), frc).getWidth());
                    }
                    list.add(edc);
                }
            }
        }
    }

    protected void addRaDecColumns(List<ModelColumn> list, Color c) {
        list.add(new RaColumn(c));
        list.add(new DecColumn(c));
    }

//=========================================================================
//-------------------------- inner classes --------------------------------
//=========================================================================

    public abstract class BaseFGColumn extends AbstractModelColumn {

       public BaseFGColumn(String name,
                           String tip,
                           Color background,
                           String propName) {
           super(name,tip,background,propName);
       }
       public int compare(int row1, int row2, int sortDir) {
           int retval= doFixedObjCompare(_fixedGroup.get(row1),
                                         _fixedGroup.get(row2));
           if (sortDir==SortInfo.SORTED_DECENDING) retval*= -1;
           return retval;
       }

       public abstract int doFixedObjCompare(FixedObject fo1, FixedObject fo2);
    }



    public class RaColumn extends BaseFGColumn {
        public RaColumn(Color background) {
            super(USER_RA_COL,USER_RA_TIP,background,"userRa");
        }

        public int doFixedObjCompare(FixedObject fo1, FixedObject fo2) {
            double v1= fo1.getPosition().getLon();
            double v2= fo2.getPosition().getLon();
            return ComparisonUtil.doCompare(v1,v2);
        }
        public Object getValueAt(int row) {
            FixedObject  fixedObj= _fixedGroup.get(row);
            return _fixedGroup.getFormatedLon(fixedObj.getPosition());
        }

        public void setValueAt(Object aValue, int row) { Assert.tst(false); }
        public boolean isEditable(int row)      { return false;}
        public int getPreferredWidth() { return 130;}
    }


    public class DecColumn extends BaseFGColumn {
        public DecColumn(Color background) {
            super(USER_DEC_COL,USER_DEC_TIP,background,"userDec");
        }

        public int doFixedObjCompare(FixedObject fo1, FixedObject fo2) {
            double v1= fo1.getPosition().getLat();
            double v2= fo2.getPosition().getLat();
            return ComparisonUtil.doCompare(v1,v2);
        }
        public Object getValueAt(int row) {
            FixedObject  fixedObj= _fixedGroup.get(row);
            return _fixedGroup.getFormatedLat(fixedObj.getPosition());
        }

        public void setValueAt(Object aValue, int row) { Assert.tst(false); }
        public boolean isEditable(int row)      { return false;}
        public int getPreferredWidth() { return 130; }
    }


    protected class ShapeColumn extends BaseFGColumn {
        private final ComboEditor _ed;
        private final ComboRender _ren;

        public ShapeColumn(Color background) {
            super(SHAPE_COL,SHAPE_TIP,background, "shape");
            _ed = new ComboEditor(_shapes);
            _ren= new ComboRender(_shapes);
        }
        public int doFixedObjCompare(FixedObject fo1, FixedObject fo2) {
            return 0;
        }
        public Object getValueAt(int row) {
            return _fixedGroup.get(row).getDrawer().getSkyShape();
        }
        public TableCellRenderer getRenderer() { return _ren; }
        public boolean isEditable(int row)     { return true; }
        public TableCellEditor getEditor()     { return _ed; }
        public void setValueAt(Object aValue, int row) {
            FixedObject fixedObj= _fixedGroup.get(row);
            fixedObj.getDrawer().setSkyShape( (SkyShape)aValue);
            if (fixedObj.isEnabled()) _fixedGroup.doRepair(fixedObj);
        }
        public int getPreferredWidth() {
            return _ren.getPreferredSize().width+10+5;
        }
    }

    protected class OnColumn extends BaseFGColumn {
        private BoolEditor _boolEditor      = new BoolEditor();
        private BoolRender _boolRender      = new BoolRender();
        public OnColumn(Color background) {
            super(ENABLED_COL,ENABLED_TIP,background, "on");
        }
        public int doFixedObjCompare(FixedObject fo1, FixedObject fo2) {
            return ComparisonUtil.doCompare(fo1.isEnabled(), fo1.isEnabled() );
        }
        public Object getValueAt(int row) {
            return new Boolean(_fixedGroup.get(row).isEnabled());
        }
        public TableCellRenderer getRenderer() { return _boolRender; }
        public boolean isEditable(int row)            { return true; }
        public TableCellEditor getEditor()     { return _boolEditor; }
        public void setValueAt(Object aValue, int row) {
            _fixedGroup.get(row).setEnabled( ((Boolean)aValue));
        }
        public int getPreferredWidth() {
            return _boolRender.getPreferredSize().width+10+5;
        }
    }



    protected class HighlightColumn extends BaseFGColumn {
        private BoolEditor _boolEditor      = new BoolEditor();
        private BoolRender _boolRender      = new BoolRender();
        public HighlightColumn(Color background) {
            super(HILIGHT_COL,HILIGHT_TIP,background, "hilight");
        }
        public int doFixedObjCompare(FixedObject fo1, FixedObject fo2) {
            return ComparisonUtil.doCompare(
                              fo1.isHiLighted(),fo2.isHiLighted());
        }
        public Object getValueAt(int row) {
            return new Boolean(_fixedGroup.get(row).isHiLighted());
        }
        public TableCellRenderer getRenderer() { return _boolRender; }
        public boolean isEditable(int row)            { return true; }
        public TableCellEditor getEditor()     { return _boolEditor; }
        public void setValueAt(Object aValue, int row) {
            _fixedGroup.get(row).setHiLighted( ((Boolean)aValue));
        }
        public int getPreferredWidth() {
            return _boolRender.getPreferredSize().width+10+5;
        }
    }

    protected class ShowNameColumn extends BaseFGColumn {
        private BoolEditor _boolEditor      = new BoolEditor();
        private BoolRender _boolRender      = new BoolRender();
        public ShowNameColumn(Color background) {
            super(SHOW_NAME_COL,SHOW_NAME_TIP,background, "showName");
        }
        public int doFixedObjCompare(FixedObject fo1, FixedObject fo2) {
            return ComparisonUtil.doCompare(
                                          fo1.getShowName(),fo2.getShowName());
        }
        public Object getValueAt(int row) {
            return new Boolean(_fixedGroup.get(row).getShowName());
        }
        public TableCellRenderer getRenderer() { return _boolRender; }
        public boolean isEditable(int row)            { return true; }
        public TableCellEditor getEditor()     { return _boolEditor; }
        public void setValueAt(Object aValue, int row) {
            _fixedGroup.get(row).setShowName( ((Boolean)aValue));
        }
        public int getPreferredWidth() {
            return _boolRender.getPreferredSize().width+10+60;
        }
    }

    public class ExtraDataColumn extends BaseFGColumn {

        private DataType _dataType;
        public ExtraDataColumn(Color background, DataType dataType) {
            super(dataType.getDefaultTitle(),
                  dataType.getDefaultTitle() + " : " + dataType.getShortDesc(),
                  background,dataType.getKeyName());
            _dataType= dataType;
        }

        public int doFixedObjCompare(FixedObject fo1, FixedObject fo2) {
            Object v1= fo1.getExtraData(_dataType);
            Object v2= fo2.getExtraData(_dataType);

            int retval= 0;
            if (v1 instanceof Number && v2 instanceof Number) {
                retval= ComparisonUtil.doCompare( (Number)v1,(Number)v2);
            }
            else {
                String s1= (v1!=null) ? v1.toString() : null;
                String s2= (v2!=null) ? v2.toString() : null;
                retval= ComparisonUtil.doCompare(s1,s2);
            }
            return retval;
        }
        public Object getValueAt(int row) {
            Object v= _fixedGroup.get(row).getExtraData(_dataType);
            Object retval= v;
            if (v instanceof Integer || v instanceof Long) {
                retval= String.format("%,d",v);
            }
            else if (v instanceof Double || v instanceof Float) {
                retval= String.format("%,f",v);
            }
            else if (v instanceof URL) {
               retval= v.toString();
            }
            return retval;
        }
        public int getPreferredWidth() {
            int retval;
            int basePW= super.getPreferredWidth();
            if (basePW > 0) {
                retval = basePW;
            } else {
                retval= 50;
                if (_dataType.getDataType()==String.class) retval= 100;
            }
            return retval;
        }
    }


    protected class TargetNameColumn extends BaseFGColumn {
        public TargetNameColumn(Color background) {
            super(TNAME_COL,TNAME_TIP,background, "targetName");
        }
        public int doFixedObjCompare(FixedObject fo1, FixedObject fo2) {
            return fo1.getTargetName().compareTo(fo2.getTargetName());
        }
        public Object getValueAt(int row) {
            return _fixedGroup.get(row).getTargetName();
        }
        public boolean isEditable(int row) {
            return _fixedGroup.isTargetNameEditable();
        }
        public void setValueAt(Object aValue, int row) {
            FixedObject fixedObj= _fixedGroup.get(row);
            fixedObj.setTargetName((String)aValue);
            if (fixedObj.isEnabled()) _fixedGroup.doRepair(fixedObj);
        }
        public int getPreferredWidth() { return 100;}
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
