package edu.caltech.ipac.firefly.ui.input;

import edu.caltech.ipac.util.StringUtils;

/**
 * @author Trey
 *         $Id: HTMLImmutableLabel.java,v 1.2 2011/03/09 18:24:39 schimms Exp $
 */

public class HTMLImmutableLabel implements FieldLabel.Immutable {

    private String _html;

    public HTMLImmutableLabel(String text, String tip) {
        if (!StringUtils.isEmpty(text)) {
            _html= "<span title=\""+tip + "\">"+ text+":" +"</span>";
        } else {
            _html= "<span title=\""+tip + "\"></span>";
        }
    }

    public String getHtml() { return _html; }

}
