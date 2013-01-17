/*
	Copyright 2008 Marco Mustapic
	
    This file is part of Agilar GWT Widgets.

    Agilar GWT Widgets is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Agilar GWT Widgets is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with Agilar GWT Widgets.  If not, see <http://www.gnu.org/licenses/>.
*/

package edu.caltech.ipac.firefly.visualize.ui.color;


import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTMLTable;
import com.google.gwt.user.client.ui.HTMLTable.CellFormatter;

public class HueSelector extends Composite implements HasValueChangeHandlers<String>, ClickHandler {
	private Grid grid = new Grid(18, 1);
	private float hue;
	

	public HueSelector()
	{
		super();
		initWidget(grid);
		grid.setCellPadding(0);
		grid.setCellSpacing(0);
		grid.addStyleName("agilar-colorpicker-popup-hueselector");
		
        grid.addClickHandler(this);

		buildUI();
		setHue(0);
	}
	
	private void buildUI()
	{
		float [] hsv = new float[]{0.0f, 1.0f, 1.0f};
		float [] rgb = new float[3];
		int [] irgb = new int[3];
		CellFormatter formatter = grid.getCellFormatter();
        formatter.setHeight(0, 0, "0px");
        formatter.setWidth(0, 0, "100%");
		for (int row=1; row<18; row++)
		{
			Color.HSVToRGB(hsv, rgb);
			Color.toInt(rgb, irgb);
			formatter.getElement(row, 0).setAttribute("bgcolor", "#"+Color.toHex(irgb));
			formatter.setHeight(row, 0, "8px");
			formatter.setWidth(row, 0, "100%");
			grid.setText(row, 0, "");
			hsv[0] = (float)row / 18.0f * 360.0f;
		}
	}

    public void onClick(ClickEvent ev) {
        HTMLTable.Cell cell= grid.getCellForEvent(ev);
        if (cell!=null) {
            int row= cell.getRowIndex();
//            int col= cell.getCellIndex();
            if (row>0) {
                float hue= (float)(row-1) / 18.0f * 360.0f;
                setHue(hue);
                ValueChangeEvent.fire(this,hue+"");
            }
        }
	}

	public void setHue(float hue)
	{
		int row = (int)(this.hue * 18.0f / 360.0f) + 1;
		CellFormatter formatter = grid.getCellFormatter();
		formatter.removeStyleName(row, 0, "agilar-colorpicker-popup-hueselector-selected");
		this.hue = hue;
		row = (int)(this.hue * 18.0f / 360.0f) + 1;
		formatter.addStyleName(row, 0, "agilar-colorpicker-popup-hueselector-selected");
	}

	public float getHue()
	{
		return hue;
	}

    public HandlerRegistration addValueChangeHandler(ValueChangeHandler<String> h)
    {
        return this.addHandler(h, ValueChangeEvent.getType());
    }

}
