package edu.caltech.ipac.firefly.ui.input;

import com.google.gwt.user.client.ui.Widget;

/**
 * User: roby
 * Date: Jan 26, 2009
 * Time: 12:57:45 PM
 */

/**
 * An abstract representation of the label.  It could be as simple as a Label widget or something
 * more complex
 */
public interface FieldLabel {


    public interface Immutable extends FieldLabel {

        /**
         * return the html to create this label
         * @return the html string
         */
        String getHtml();
    }

    public interface Mutable extends FieldLabel {
        /**
         * Return the widget that contains the label
         * @return a widget
         */
        Widget getWidget();

        /**
         * Set the text of the label
         * @param txt a string with the text the user will see
         */
        void setText(String txt);

        /**
         * Set the text of the tool tip
         * @param tip the tool tip
         */
        void setTip(String tip);

        /**
         * set the label visible or invisible
         * @param v true if visible, false if invisible
         */
        public void setVisible(boolean v);
    }

}

