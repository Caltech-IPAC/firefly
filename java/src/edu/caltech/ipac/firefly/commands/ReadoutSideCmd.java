package edu.caltech.ipac.firefly.commands;

import com.google.gwt.user.client.ui.Image;
import edu.caltech.ipac.firefly.resbundle.images.VisIconCreator;
import edu.caltech.ipac.firefly.util.WebAssert;
import edu.caltech.ipac.firefly.visualize.AllPlots;
import edu.caltech.ipac.firefly.visualize.WebMouseReadout;

import static edu.caltech.ipac.firefly.visualize.WebMouseReadout.Side.Bottom;
import static edu.caltech.ipac.firefly.visualize.WebMouseReadout.Side.Left;
import static edu.caltech.ipac.firefly.visualize.WebMouseReadout.Side.Right;
import static edu.caltech.ipac.firefly.visualize.WebMouseReadout.Side.Top;


public class ReadoutSideCmd extends BaseGroupVisCmd {
    public static final String CommandName= "ReadoutSide";
    public int _lastSide=0;
    public  WebMouseReadout.Side[] _order = { Left,Top, Right,Bottom};
    public  final static String[] _icons= { "ReadoutSide.left.Icon",
                                            "ReadoutSide.top.Icon",
                                            "ReadoutSide.right.Icon",
                                            "ReadoutSide.bottom.Icon"};


    public ReadoutSideCmd() {
        super(CommandName);
        WebAssert.tst(_icons.length==_order.length,
                      "ReadoutSideCmd: _icons and _order must be the same array length");
    }


    protected void doExecute() {
        _lastSide++;
        if (_lastSide==_order.length) _lastSide= 0;
        setReadoutSide(_order[_lastSide]);
    }

    public void setReadoutSide(WebMouseReadout.Side side) {
        AllPlots.getInstance().getMouseReadout().setDisplaySide(side);
        for(int i= 0; (i<_order.length); i++) {
            if (_order[i]==side) {
                setIconProperty(_icons[i]);
                _lastSide= i;
            }
        }

    }


    @Override
    public Image createCmdImage() {

        VisIconCreator ic= VisIconCreator.Creator.getInstance();
        String iStr= this.getIconProperty();
        if (iStr!=null) {

            if (iStr.equals("ReadoutSide.left.Icon"))  {
                return new Image(ic.getSideLeftArrow());
            }
            else if (iStr.equals("ReadoutSide.right.Icon"))  {
                return new Image(ic.getSideRightArrow());
            }
            else if (iStr.equals("ReadoutSide.top.Icon"))  {
                return new Image(ic.getSideUpArrow());
            }
            else if (iStr.equals("ReadoutSide.bottom.Icon"))  {
                return new Image(ic.getSideDownArrow());
            }
            else if (iStr.equals("ReadoutSide.Icon"))  {
                return new Image(ic.getSideLeftArrow());
            }
            else {
                return null;
            }
        }

        return null;
    }


}