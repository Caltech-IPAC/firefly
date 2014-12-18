package edu.caltech.ipac.visualize.draw;


/**
 * a class to hold multi line values.
 *
 * @see MultiLineTextRender
 *
 * @author Trey Roby
 * @version $Id: MultiLineValue.java,v 1.2 2005/12/08 22:30:55 tatianag Exp $
 */
public class MultiLineValue {
       private String _label;
       private String _value;
       public MultiLineValue( String label, String value) {
          _label= label;
          _value= value;
       }
       public String getLabel() { return _label; }
       public String getValue() { return _value; }
}


