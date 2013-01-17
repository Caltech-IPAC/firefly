package edu.caltech.ipac.firefly.visualize.draw;
/**
 * User: roby
 * Date: 5/8/12
 * Time: 2:19 PM
 */


import edu.caltech.ipac.firefly.visualize.OffsetScreenPt;
import edu.caltech.ipac.util.HandSerialize;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Trey Roby
 */
public class StaticDrawInfo implements Serializable, HandSerialize, Iterable<WorldPt> {

    public enum DrawType {SYMBOL, GRID, NORTH_ARROW, VECTOR, SHAPE, LABEL}

    private final static String SPLIT_TOKEN= "--SDI--";
    private final static String SPLIT_TOKEN_ARY= "--SDI_ARRAY--";

    private DrawType drawType= DrawType.SYMBOL;
    private DrawSymbol symbol= DrawSymbol.X;
    private String color= "red";
    private String gridType= WebGridLayer.GRID_NONE;
    private String label= null;
    private ShapeDataObj.ShapeType shapeType= ShapeDataObj.ShapeType.Circle;
    private float dim1= 0F;
    private float dim2= 0F;
    private List<WorldPt> list= new ArrayList<WorldPt>(100);
    private OffsetScreenPt textOffset = null;


    public void add(WorldPt pt) { list.add(pt); }

    public void setSymbol(DrawSymbol symbol) { this.symbol= symbol; }
    public DrawSymbol getSymbol() { return symbol; }

    public void setColor(String color) { this.color = color; }
    public String getColor() { return color; }

    public void setList(List<WorldPt> list) { this.list = list; }
    public List<WorldPt> getList() { return list; }

    public void setGridType(String gridType) { this.gridType = gridType; }
    public String getGridType() { return gridType; }

    public DrawType getDrawType() { return drawType; }
    public void setDrawType(DrawType drawType) {  this.drawType = drawType; }

    public void setLabel(String label) { this.label = label; }
    public String getLabel() {  return label; }

    public Iterator<WorldPt> iterator() { return list.iterator(); }

    public void setDim1(float dim1) { this.dim1 = dim1; }
    public float getDim1() {  return dim1; }

    public void setDim2(float dim2) { this.dim2 = dim2; }
    public float getDim2() {  return dim2; }

    public void setShapeType(ShapeDataObj.ShapeType shapeType) { this.shapeType = shapeType; }
    public ShapeDataObj.ShapeType getShapeType() { return shapeType; }

    public void setTextOffset(OffsetScreenPt textOffset) { this.textOffset= textOffset; }
    public OffsetScreenPt getTextOffset() { return textOffset; }

    public String serialize() {
        String ary[]=  new String[list.size()];
        int i= 0;
        for(WorldPt pt : list)  ary[i++]= pt.toString();
        String wpAry= StringUtils.combineAry(SPLIT_TOKEN_ARY, ary);
        return   StringUtils.combine(SPLIT_TOKEN, drawType.toString(), symbol.toString(),
                                     color, gridType, label, textOffset==null ? null : textOffset.serialize(),
                                     shapeType.toString(), dim1+"", dim2+"",
                                     wpAry);
    }

    public static StaticDrawInfo parse(String s) {
        String sAry[]= StringUtils.parseHelper(s, 10, SPLIT_TOKEN);
        StaticDrawInfo drawInfo= null;
        if (sAry!=null) {
            try {
                WorldPt wp;
                int i=0;
                drawInfo= new StaticDrawInfo();
                DrawType drawType= Enum.valueOf(DrawType.class, sAry[i++]);
                drawInfo.setDrawType(drawType);
                DrawSymbol symbol= Enum.valueOf(DrawSymbol.class, sAry[i++]);
                drawInfo.setSymbol(symbol);
                String color= StringUtils.checkNull(sAry[i++]);
                if (color!=null) drawInfo.setColor(color);
                String gridType= StringUtils.checkNull(sAry[i++]);
                if (gridType!=null) drawInfo.setGridType(gridType);
                drawInfo.setLabel(StringUtils.checkNull(sAry[i++]));
                OffsetScreenPt offsetScreenPt= OffsetScreenPt.parse(StringUtils.checkNull(sAry[i++]));
                if (offsetScreenPt!=null) drawInfo.setTextOffset(offsetScreenPt);
                ShapeDataObj.ShapeType shapeType= Enum.valueOf(ShapeDataObj.ShapeType.class, sAry[i++]);
                drawInfo.setShapeType(shapeType);
                drawInfo.setDim1(Float.parseFloat(sAry[i++]));
                drawInfo.setDim2(Float.parseFloat(sAry[i++]));

                List<String> wpStrList= StringUtils.parseStringList(sAry[i++],SPLIT_TOKEN_ARY);
                for(String wpStr : wpStrList) {
                    wp= WorldPt.parse(wpStr);
                    if (wp!=null) drawInfo.add(wp);
                }

            } catch (Exception e) {
            }
        }
        return drawInfo;
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
