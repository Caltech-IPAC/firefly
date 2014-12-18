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
