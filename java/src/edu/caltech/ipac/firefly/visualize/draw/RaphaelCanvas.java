package edu.caltech.ipac.firefly.visualize.draw;

import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.hydro4ge.raphaelgwt.client.PathBuilder;
import com.hydro4ge.raphaelgwt.client.RaphaelJS;

/**
 * User: roby
 * Date: Mar 26, 2010
 * Time: 10:35:21 AM
 */



/**
 * This class skips the Raphael GWT class and goes directly toe the RaphaeJS.
 * The Raphael class creates a lot of Widgets that we do not need.  Better to just work at a lower level.
 * We also use the Raphael PathBuilder class which is very useful.
 *
 * @author Trey Roby
 */
public class RaphaelCanvas extends Composite {


    private final RaphaelJS _js;
    private final AbsolutePanel _panel= new AbsolutePanel();

    public RaphaelCanvas() {
        _js = RaphaelJS.create(_panel.getElement(), 100, 100);
        initWidget(_panel);
    }


    public void setPixelSize(int width, int height) {
        _js.setSize(width,height);
    }


    public void addLabel(Label label,int x, int y) { _panel.add(label,x,y); }

    public void removeLabel(Label label) { _panel.remove(label); }

    public void clear() {
        _js.clear();
    }

    public RaphaelJS.Element makePath(PathBuilder pb) {
        return  _js.path(pb.toString());
    }

    public RaphaelJS.Element circle(int x, int y, int radius) {
        return  _js.circle(x,y,radius);
    }


}

