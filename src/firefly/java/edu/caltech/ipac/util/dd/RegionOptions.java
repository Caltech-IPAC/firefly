/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util.dd;
/**
 * User: roby
 * Date: 2/11/13
 * Time: 1:24 PM
 */


import com.google.gwt.core.client.js.JsExport;
import com.google.gwt.core.client.js.JsType;
import edu.caltech.ipac.util.HandSerialize;

import java.io.Serializable;

/**
 * @author Trey Roby
 */
@JsExport
@JsType
public class RegionOptions implements Serializable, HandSerialize {

    private String color= "green";
    private boolean editable= true;
    private boolean movable= true;
    private boolean rotatable= true;
    private boolean highlightable= true;
    private boolean deletable= true;
    private boolean fixedSize= true;
    private boolean include= true;
    private int     lineWidth= 1;
    private int     offsetX= 0;
    private int     offsetY= 0;
    private String  text= "";
    private RegionFont font= new RegionFont("helvetica", 10, "normal", "roman");

    public RegionOptions() {}

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public boolean isMovable() {
        return movable;
    }

    public void setMovable(boolean movable) {
        this.movable = movable;
    }

    public boolean isRotatable() {
        return rotatable;
    }

    public void setRotatable(boolean rotatable) {
        this.rotatable = rotatable;
    }

    public boolean isHighlightable() {
        return highlightable;
    }

    public void setHighlightable(boolean highlightable) {
        this.highlightable = highlightable;
    }

    public boolean isDeletable() {
        return deletable;
    }

    public void setDeletable(boolean deletable) {
        this.deletable = deletable;
    }

    public boolean isFixedSize() {
        return fixedSize;
    }


    public void setFixedSize(boolean fixedSize) {
        this.fixedSize = fixedSize;
    }

    public int getLineWidth() {
        return lineWidth;
    }

    public void setLineWidth(int lineWidth) {
        this.lineWidth = lineWidth;
    }

    public int getOffsetX() {
        return offsetX;
    }

    public void setOffsetX(int x) {
        this.offsetX = x;
    }

    public int getOffsetY() {
        return offsetY;
    }

    public void setOffsetY(int y) {
        this.offsetY = y;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public RegionFont getFont() {
        return font;
    }

    public void setFont(RegionFont font) {
        this.font = font;
    }

    public boolean isInclude() {
        return include;
    }

    public void setInclude(boolean include) {
        this.include = include;
    }

    public RegionOptions copy() {
        RegionOptions op= new RegionOptions();
        op.color= this.color;
        op.editable= this.editable;
        op.movable= this.movable;
        op.rotatable= this.rotatable;
        op.highlightable= this.highlightable;
        op.deletable= this.deletable;
        op.fixedSize= this.fixedSize;
        op.lineWidth= this.lineWidth;
        op.offsetX= this.offsetX;
        op.offsetY= this.offsetY;
        op.text= this.text;
        op.font= this.font;
        return op;
    }

    public String serialize() {
        return color + ";"+
               editable + ";"+
               movable + ";"+
               rotatable + ";"+
               highlightable + ";"+
               deletable + ";"+
               include + ";"+
               fixedSize + ";"+
               lineWidth + ";"+
               offsetX + ";"+
               offsetY + ";"+
               font + ";"+
               text;
    }
}

