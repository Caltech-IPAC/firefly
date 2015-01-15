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
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class ColorPicker extends Composite implements FocusHandler, CloseHandler<PopupPanel>, ClickHandler
{
	private TextBox textBox = new TextBox();
	private ColorChooserPanel colorChooserPanel = new ColorChooserPanel();
	private FocusPanel colorPanel = new FocusPanel();

	private static class ColorPickerPopup extends PopupPanel implements ClickHandler
	{
		private Label cancel = new Label("cancel");
		private Label ok = new Label("ok");

		private boolean accepted = false;
		private boolean showLinks;
		
		public ColorPickerPopup(ColorChooserPanel colorPicker, boolean showLinks, boolean autoClose)
		{
			super(autoClose, true);
			
			this.showLinks = showLinks;
			
			VerticalPanel vpanel = new VerticalPanel();
			vpanel.add(colorPicker);
			
			if (showLinks)
			{
				HorizontalPanel hpanel = new HorizontalPanel();
				cancel.addStyleName("agilar-colorpicker-popup-link");
				ok.addStyleName("agilar-colorpicker-popup-link");
				cancel.addClickHandler(this);
				ok.addClickHandler(this);
				hpanel.add(cancel);
				hpanel.add(ok);
				vpanel.add(hpanel);
				vpanel.setCellHorizontalAlignment(hpanel, VerticalPanel.ALIGN_RIGHT);
			}
			setWidget(vpanel);
		}

		@Override
		public void show()
		{
			super.show();
			accepted = !showLinks;
		}


        public void onClick(ClickEvent ev) {
			if (ev.getSource() == ok)
			{
				accepted = true;
			}
			
			hide();
		}

		public boolean isAccepted()
		{
			return accepted;
		}
	}
	
	private ColorPickerPopup popup;
	
	public ColorPicker()
	{
		this("ffffff", true, false);
	}
	
	public ColorPicker(String color)
	{
		this(color, true, false);
	}
	
	public ColorPicker(String color, boolean autoClose)
	{
		this(color, true, autoClose);
	}
	
	public ColorPicker(String color, boolean showLinks, boolean autoClose)
	{
		popup = new ColorPickerPopup(colorChooserPanel, showLinks, autoClose);
		
		textBox.addStyleName("agilar-colorpicker-text");
		textBox.addFocusHandler(this);
		textBox.setMaxLength(6);
		
		popup.addStyleName("agilar-colorpicker-popup");		
		popup.addCloseHandler(this);
		
		colorPanel.addStyleName("agilar-colorpicker-colorPreview");
		colorPanel.addClickHandler(this);
		colorPanel.addFocusHandler(this);
		
		HorizontalPanel hpanel = new HorizontalPanel();
		hpanel.add(textBox);
		hpanel.add(colorPanel);
		hpanel.addStyleName("agilar-colorpicker-widget");
		initWidget(hpanel);

		setColor(color);
	}

    public void onFocus(FocusEvent event) {
		showPopup();
	}


	public void setColor(String color)
	{
		textBox.setText(color);
		colorChooserPanel.setColor(color);
		colorPanel.getElement().setAttribute("style", "background-color: #"+colorChooserPanel.getColor());
	}
	
	public String getColor()
	{
		return colorChooserPanel.getColor();
	}

    public void onClose(CloseEvent<PopupPanel> ev) {
		if (this.popup.isAccepted())
		{
			textBox.setText(colorChooserPanel.getColor());
			colorPanel.getElement().setAttribute("style", "background-color: #"+colorChooserPanel.getColor());
		}
	}

    public void onClick(ClickEvent event) {
		showPopup();
	}

	private void showPopup()
	{
		colorChooserPanel.setColor(textBox.getText());
		popup.show();
		popup.setPopupPosition(textBox.getAbsoluteLeft(), textBox.getAbsoluteTop());
	}
}
