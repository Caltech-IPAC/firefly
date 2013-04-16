package edu.caltech.ipac.firefly.commands;

import com.google.gwt.user.client.ui.Image;
import edu.caltech.ipac.firefly.resbundle.images.CBarIconCreator;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.util.WebAssert;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotView;


public class ChangeColorCmd extends BaseGroupVisCmd {




    private final int _colorIdx;

    public ChangeColorCmd(String commandName,
                          int colorIdx) {
        super(commandName);
        WebAssert.argTst(   (colorIdx>=0 && colorIdx<=21),
                            "colorIdx must be between 0 and 21 inclusive");
        _colorIdx= colorIdx;
    }


    protected void doExecute() {
        for(MiniPlotWidget mpwItem : getGroupActiveList() ) {
            WebPlotView pv= mpwItem.getPlotView();
            WebPlot plot= pv.getPrimaryPlot();
            if (plot==null) continue; //don't do anything if plot is null.
            if (!plot.isThreeColor()) {
                plot.getHistogramOps(Band.NO_BAND).changeColor(_colorIdx);
            }
            else {
                if (getGroupActiveList().size()==1) {
                    PopupUtil.showInfo(getPlotView(),"Can't change color", "This is a three color plot, you can not change the color");
                }
            }
        }
    }

//    public boolean isIconBundleSafe() { return false; }

    @Override
    public Image createCmdImage() {

        CBarIconCreator ic= CBarIconCreator.Creator.getInstance();
        String iStr= this.getIconProperty();
        if (iStr!=null) {

            if (iStr.equals("colorTable0.Icon"))  {
                return new Image(ic.getColorTable0());
            }
            else if (iStr.equals("colorTable1.Icon"))  {
                return new Image(ic.getColorTable1());
            }
            else if (iStr.equals("colorTable2.Icon"))  {
                return new Image(ic.getColorTable2());
            }
            else if (iStr.equals("colorTable3.Icon"))  {
                return new Image(ic.getColorTable3());
            }
            else if (iStr.equals("colorTable4.Icon"))  {
                return new Image(ic.getColorTable4());
            }
            else if (iStr.equals("colorTable5.Icon"))  {
                return new Image(ic.getColorTable5());
            }
            else if (iStr.equals("colorTable6.Icon"))  {
                return new Image(ic.getColorTable6());
            }
            else if (iStr.equals("colorTable7.Icon"))  {
                return new Image(ic.getColorTable7());
            }
            else if (iStr.equals("colorTable8.Icon"))  {
                return new Image(ic.getColorTable8());
            }
            else if (iStr.equals("colorTable9.Icon"))  {
                return new Image(ic.getColorTable9());
            }
            else if (iStr.equals("colorTable10.Icon"))  {
                return new Image(ic.getColorTable10());
            }
            else if (iStr.equals("colorTable11.Icon"))  {
                return new Image(ic.getColorTable11());
            }
            else if (iStr.equals("colorTable12.Icon"))  {
                return new Image(ic.getColorTable12());
            }
            else if (iStr.equals("colorTable13.Icon"))  {
                return new Image(ic.getColorTable13());
            }
            else if (iStr.equals("colorTable14.Icon"))  {
                return new Image(ic.getColorTable14());
            }
            else if (iStr.equals("colorTable15.Icon"))  {
                return new Image(ic.getColorTable15());
            }
            else if (iStr.equals("colorTable16.Icon"))  {
                return new Image(ic.getColorTable16());
            }
            else if (iStr.equals("colorTable17.Icon"))  {
                return new Image(ic.getColorTable17());
            }
            else if (iStr.equals("colorTable18.Icon"))  {
                return new Image(ic.getColorTable18());
            }
            else if (iStr.equals("colorTable19.Icon"))  {
                return new Image(ic.getColorTable19());
            }
            else if (iStr.equals("colorTable20.Icon"))  {
                return new Image(ic.getColorTable20());
            }
            else if (iStr.equals("colorTable21.Icon"))  {
                return new Image(ic.getColorTable21());
            }
            else {
                return null;
            }
        }

        return null;
    }
}