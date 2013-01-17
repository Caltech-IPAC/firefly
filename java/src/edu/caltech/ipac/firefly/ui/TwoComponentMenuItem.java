package edu.caltech.ipac.firefly.ui;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.*;

/**
 * Class that extends the standard MenuItem widget to display
 * some menu text as well as a 2nd component after the text.
 * $Id: TwoComponentMenuItem.java,v 1.5 2009/10/30 15:42:24 tatianag Exp $
 */
public class TwoComponentMenuItem extends MenuItem{

    private HorizontalPanel theMenuItem = new HorizontalPanel();
    /**
     * Constructor of the TwoComponentMenuItem - the result is a HTML MenuItem
     * that can be treated as such.
     *
     * @param theText The text part of the menu item.
     * @param firstComponent A widget which is placed to the right of the text.
     * @param theCommand The GWT GeneralCommand that will be executed when the menu item is selected.
     */
    public TwoComponentMenuItem(String theText,
                                Widget firstComponent,
                                Command theCommand){
        super(theText,false,theCommand);
        theMenuItem.setVerticalAlignment(HasVerticalAlignment.ALIGN_BOTTOM);
        theMenuItem.add(firstComponent);
        theMenuItem.add(new Label(theText+" "));
//        setStyleName(theMenuItem.getWidget(1).getElement(), "gwt-MenuItem", true);
//        setStyleName(theMenuItem.getWidget(0).getElement(), "gwt-MenuItem", true);
        setStyleName(theMenuItem.getElement(),"holder",true);

//        firstComponent.addStyleName("firefly-MenuItemLabel");
        setSecondComponent(theText);
    }

    /**
     * Sets the 2nd component of the TwoComponentMenuItem by removing the
     * initial 2nd item from the HorizontalPanel and then placing the
     * provided paramter as the new 2nd item.
     *
     * Then, we set the MenuItem text to be the HTML representation of the
     * HorizontalPanel.
     *
     * @param newComponent The widget to be placed as the 2nd item in the MenuItem.
     */
    public void setFirstComponent(Widget newComponent){
        theMenuItem.remove(0);
        theMenuItem.insert(newComponent, 0);
        SimplePanel dummyContainer = new SimplePanel();
        dummyContainer.add(theMenuItem);
        String test = DOM.getInnerHTML(dummyContainer.getElement());
        this.setHTML(test);
    }



    public void setImage(Image image){
        setFirstComponent(image);
    }



        /**
	 * Sets the 1st component of the TwoComponentMenuItem by removing the
	 * initial 1st item from the HorizontalPanel and then placing the 
	 * provided paramter as the text to a new Label Widget as the 1st item.
	 * 
	 * Then, we set the MenuItem text to be the HTML representation of the 
	 * HorizontalPanel.
	 * 
	 * @param newComponent The widget to be placed as the 1st item in the MenuItem.
	 */
	public void setSecondComponent(String newComponent){
            theMenuItem.remove(1);
            Label label= new Label(newComponent);
//            setStyleName(label.getElement(), "gwt-MenuItem", true);
            label.setStyleName("firefly-MenuItemLabel");
            label.addStyleName("gwt-MenuItem");
            theMenuItem.add(label);
            SimplePanel dummyContainer = new SimplePanel();
            dummyContainer.add(theMenuItem);
            String test = DOM.getInnerHTML(dummyContainer.getElement());
            this.setHTML(test);
	}


}
