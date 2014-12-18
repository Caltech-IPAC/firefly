package edu.caltech.ipac.firefly.visualize.draw;
/**
 * User: roby
 * Date: 5/8/12
 * Time: 2:19 PM
 */


import edu.caltech.ipac.firefly.visualize.OffsetScreenPt;
import edu.caltech.ipac.util.HandSerialize;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.dd.Region;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Trey Roby
 */
public class StaticDrawInfo implements Serializable, HandSerialize, Iterable<WorldPt> {

    public enum DrawType {SYMBOL, GRID, REGION}

    private final static String SPLIT_TOKEN= "--SDI--";
    private final static String SPLIT_TOKEN_ARY= "--SDI_ARRAY--";

    private DrawType drawType= DrawType.SYMBOL;
    private DrawSymbol symbol= DrawSymbol.X;
    private String color= "red";
    private String gridType= WebGridLayer.GRID_NONE;
    private String label= null;
    private ShapeDataObj.ShapeType shapeType= ShapeDataObj.ShapeType.Circle;
    private List<WorldPt> list= new ArrayList<WorldPt>(100);
    private OffsetScreenPt textOffset = null;
    private List<String> serializedRegionList= new ArrayList<String>(40);


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


    public void addRegion(Region r) {
        if (r!=null) serializedRegionList.add(r.serialize());
    }

    public void addAllRegions(List<Region> regList) {
        if (regList!=null) {
           for(Region r : regList) addRegion(r);
        }
    }


    public List<Region> getRegionList() {
        List<Region> retList= new ArrayList<Region>(serializedRegionList.size());
        for(String s :serializedRegionList) {
            Region r= Region.parse(s);
            if (r!=null) retList.add(r);
        }
        return retList;
    }

    public void setShapeType(ShapeDataObj.ShapeType shapeType) { this.shapeType = shapeType; }
    public ShapeDataObj.ShapeType getShapeType() { return shapeType; }

    public void setTextOffset(OffsetScreenPt textOffset) { this.textOffset= textOffset; }
    public OffsetScreenPt getTextOffset() { return textOffset; }

    public String serialize() {
        String ary[]=  new String[list.size()];
        String regAry[]=  new String[serializedRegionList.size()];
        int i= 0;
        for(WorldPt pt : list)  ary[i++]= pt.toString();
        i= 0;
        for(String s: serializedRegionList)  regAry[i++]= s;
        String wpAry= StringUtils.combineAry(SPLIT_TOKEN_ARY, ary);
        String regStrAry= StringUtils.combineAry(SPLIT_TOKEN_ARY, regAry);
        return   StringUtils.combine(SPLIT_TOKEN, drawType.toString(), symbol.toString(),
                                     color, gridType, label, textOffset==null ? null : textOffset.serialize(),
                                     shapeType.toString(), regStrAry,
                                     wpAry);
    }

    public static StaticDrawInfo parse(String s) {
        String sAry[]= StringUtils.parseHelper(s, 9, SPLIT_TOKEN);
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
//                drawInfo.setDim1(Float.parseFloat(sAry[i++]));
//                drawInfo.setDim2(Float.parseFloat(sAry[i++]));
                drawInfo.serializedRegionList= StringUtils.parseStringList(sAry[i++],SPLIT_TOKEN_ARY);

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

