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

import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;


public class ColorChooserPanel extends Composite implements ValueChangeHandler, KeyUpHandler
{
	private ColorPalette palette = new ColorPalette();
	private HueSelector hueSelector = new HueSelector();
	private SimplePanel colorPanel = new SimplePanel();
	private TextBox textColor = new TextBox();
	
	private float [] hsv = new float[3];
	private float [] rgb = new float[3];
	
	public ColorChooserPanel()
	{
        HorizontalPanel hpanel = new HorizontalPanel();
        hpanel.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
        hpanel.add(new HTML("#"));
        hpanel.setVerticalAlignment(HorizontalPanel.ALIGN_TOP);
        hpanel.add(textColor);
        hpanel.add(colorPanel);

        VerticalPanel panel = new VerticalPanel();
        panel.add(hpanel);

        hpanel = new HorizontalPanel();
        hpanel.add(palette);
        hpanel.add(hueSelector);

        palette.addValueChangeHandler(this);
        hueSelector.addValueChangeHandler(this);

        panel.add(hpanel);

        colorPanel.addStyleName("agilar-colorpicker-popup-colorPanel");
        textColor.addStyleName("agilar-colorpicker-popup-text");
        textColor.addKeyUpHandler(this);
        textColor.setMaxLength(6);

        initWidget(panel);
        addStyleName("agilar-colorpicker");

        setRGB(0xff0000);
	}

	public void onValueChange(ValueChangeEvent ev) {
        Widget widget= (Widget)ev.getSource();
		if (widget == hueSelector)
		{
			palette.setHue(hueSelector.getHue());
		}
		hsv[0] = palette.getHue();
		hsv[1] = palette.getSaturation();
		hsv[2] = palette.getValue();
		
		Color.HSVToRGB(hsv, rgb);
		int [] irgb = new int[3];
		Color.toInt(rgb, irgb);
		colorPanel.getElement().setAttribute("style", "background-color: #"+Color.toHex(irgb));
		textColor.setText(Color.toHex(irgb));
	}

	public int getRGB()
	{
		int [] irgb = new int[3];
		Color.toInt(rgb, irgb);
		return irgb[0] << 16 + irgb[1] << 8 + irgb[2];
	}
	
	public void setRGB(int color)
	{
		int [] irgb = new int[3];
		irgb[2] = color & 0xff;
		irgb[1] = (color >> 8) & 0xff;
		irgb[0] = (color >> 16) & 0xff;
		
		Color.toFloat(irgb, rgb);
		Color.RGBToHSV(rgb, hsv);

		// set color in all widgets
		hueSelector.setHue(hsv[0]);
		palette.setHue(hsv[0]);
		palette.setSaturationAndValue(hsv[1], hsv[2]);
		this.colorPanel.getElement().setAttribute("style", "background-color: #"+Color.toHex(irgb));
		textColor.setText(Color.toHex(irgb));
	}

	public void setColor(String text)
	{
		if (isColor(text))
		{
			setRGB(toInt(text));
		}
	}
	
	public String getColor()
	{
		int [] irgb = new int[3];
		Color.toInt(rgb, irgb);
		return Color.toHex(irgb);
	}
	
	private int toInt(String text)
	{
		int num = 0;
		for (int i=0; i<text.length(); i++)
		{
			char c = text.charAt(i);
			num <<= 4;
			num |= Character.digit(c, 16);
		}
		return num;
	}

	private static boolean isColor(String text)
	{
		if (text.length() != 6)
		{
			return false;
		}
		for (int i=0; i<text.length(); i++)
		{
			char c = text.charAt(i);
			if (Character.digit(c, 16) == -1)
			{
				return false;
			}
		}
		return true;
	}

    public void onKeyUp(KeyUpEvent event) {
		String text = textColor.getText();
		if (isColor(text))
		{
			setColor(text);
		}
	}
}
