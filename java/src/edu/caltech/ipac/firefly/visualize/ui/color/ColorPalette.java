/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
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

public class ColorPalette extends Composite implements HasValueChangeHandlers<String>, ClickHandler  {
	private float hue;
	private float saturation;
	private float value;
	
	private Grid grid = new Grid(16, 16);
	
	public ColorPalette()
	{
		super();
		initWidget(grid);
		grid.setCellPadding(0);
		grid.setCellSpacing(0);
		grid.addStyleName("agilar-colorpicker-popup-palette");
		setHue(0.2f);
		
        grid.addClickHandler(this);
	}
	
	public float getHue()
	{
		return hue;
		
	}
	public void setHue(float hue)
	{
		this.hue = hue;
		float [] hsv = new float[]{hue, 0.0f, 0.0f};
		int [] irgb = new int[3];
		float [] rgb = new float[3];
		
		CellFormatter formatter = grid.getCellFormatter();
		for (int row=0; row<16; row++)
		{
			hsv[2] = 1.0f - (float)row / 15.0f;
			for (int col=0; col<16; col++)
			{
				hsv[1] = (float)col / 15.0f;
				Color.HSVToRGB(hsv, rgb);
				Color.toInt(rgb, irgb);
//				formatter.setWidth(row, col, "6.25%");
//				formatter.setHeight(row, col, "6.25%");
                formatter.setWidth(row, col, "10px");
                formatter.setHeight(row, col, "10px");
				formatter.getElement(row, col).setAttribute("bgcolor", "#"+Color.toHex(irgb));
				grid.setText(row, col, "");
			}
		}
	}


    public void onClick(ClickEvent ev) {
        HTMLTable.Cell cell= grid.getCellForEvent(ev);
        if (cell!=null) {
            int row= cell.getRowIndex();
            int col= cell.getCellIndex();
            float sat= (float)col / 15.0f;
            float val= 1.0f - (float)row / 15.0f;
            setSaturationAndValue(sat, val);
            ValueChangeEvent.fire(this, sat + " " + val);
        }
    }

	public float getSaturation()
	{
		return saturation;
	}

	public float getValue()
	{
		return value;
	}

	public void setSaturationAndValue(float saturation, float value)
	{
		// deselect old selected cell
		int col = Math.round(this.saturation * 15.0f);
		int row = Math.round((1.0f - this.value) * 15.0f);

		CellFormatter formatter = grid.getCellFormatter();
		formatter.removeStyleName(row, col, "agilar-colorpicker-popup-palette-selected");

		this.saturation = saturation;
		this.value = value;
		
		col = Math.round(this.saturation * 15.0f);
		row = Math.round((1.0f - this.value) * 15.0f);
		formatter.addStyleName(row, col, "agilar-colorpicker-popup-palette-selected");
		// select new cell
	}
	
	public HandlerRegistration addValueChangeHandler(ValueChangeHandler<String> h)
	{
		return this.addHandler(h, ValueChangeEvent.getType());
	}

}
