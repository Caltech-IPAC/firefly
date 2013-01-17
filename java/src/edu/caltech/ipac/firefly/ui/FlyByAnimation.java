package edu.caltech.ipac.firefly.ui;

import com.google.gwt.animation.client.Animation;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Date: Apr 26, 2011
 *
 * @author loi
 * @version $Id: FlyByAnimation.java,v 1.1 2011/04/30 00:01:45 loi Exp $
 */
public class FlyByAnimation extends Animation {

    private PopupPanel _popup= new PopupPanel();
    private int _startX;
    private int _startY;
    private int _endX;
    private int _endY;
    private float _slope;
    private int _startSize= 55;
    private int _endSize= 15;
    private int _totalDiff = _startSize-_endSize;
    private Widget _aninationIcon;


    public FlyByAnimation(Widget animator, int startX, int startY, int endX, int endY) {
        _startX= startX;
        _startY= startY;
        _endX= endX;
        _endY= endY;
        _slope= (float)(_endY-startY)/(float)(_endX-startX);
        _aninationIcon = animator;
    }

    public void setStartSize(int startSize) {
        _startSize = startSize;
        _totalDiff= _startSize-_endSize;
    }

    public void setEndSize(int endSize) {
        _endSize = endSize;
        _totalDiff= _startSize-_endSize;
    }

    @Override
    protected void onStart() {
        _popup.setStyleName("");
        _popup.setAnimationEnabled(false);
        _popup.setWidget(_aninationIcon);
        _popup.setPopupPosition(_startX,_startY);
        _popup.show();
    }

    @Override
    protected void onComplete() {
        _popup.hide();
    }

    @Override
    protected void onUpdate(double progress) {
        double x= ((_endX-_startX)*progress) + _startX;
        double y= _slope*(x-_endX) + _endY;
        _popup.setPopupPosition((int)x,(int)y);
        int pixDown= (int)(_totalDiff*progress);
        _aninationIcon.setPixelSize(_startSize-pixDown,_startSize-pixDown);

    }

}
/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
* INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
* OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS,
* HOWEVER USED.
*
* IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE
* FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL
* OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO
* PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE
* ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
*
* RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE
* AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR
* ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE
* OF THE SOFTWARE.
*/
