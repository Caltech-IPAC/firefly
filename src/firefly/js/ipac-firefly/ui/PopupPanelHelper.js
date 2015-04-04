/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


//import {LayoutType} from 'ipac-firefly/ui/PopupPanel.jsx';

import Enum from 'enum';

"use strict";

var canExpand= false;
const MARGIN= 10;

const MOUSE_EV= "mouse";
const TOUCH_EV= "touch";

import {getAbsoluteLeft, getAbsoluteTop} from 'ipac-firefly/util/BrowserUtil.js';

export const PopupRegion = new Enum(['NW_CORNER', 'NE_CORNER', 'SE_CORNER', 'SW_CORNER', 'TITLE_BAR', 'NONE']);


export const getPopupPosition= function(e,layoutType) {

    var left= 0;
    var top= 0;
    switch (layoutType.toString()) {
        case "CENTER" :
            left= window.innerWidth/2 - e.offsetWidth/2;
            top= window.innerHeight/2 - e.offsetHeight/2;

            break;
        case "TOP_CENTER" :
            left= window.innerWidth/2 - e.offsetWidth/2;
            top= 100;

            break;

    }

    return {left : left +"px", top : top+"px"};
}






var setCursorShape= function(popup,ev) {
    if (!canExpand) return;
    var x= getAbsoluteX(ev);
    var y= getAbsoluteY(ev);
    r= findRegion(popup,x,y);
    if (r==PopupRegion.SE_CORNER) {
        GwtUtil.setStyle(popup, "cursor", "se-resize");
    }
    else if (r==PopupRegion.SW_CORNER) {
        GwtUtil.setStyle(popup, "cursor", "sw-resize");
    }
    else {
        GwtUtil.setStyle(popup, "cursor", "auto");
    }
}

export const humanStart= function(ev,popup) {
    ev.preventDefault();
    var x= getAbsoluteX(ev);
    var y= getAbsoluteY(ev);
    var popupRegion= findRegion(popup,x,y);
    var ctx= null;

    if (popupRegion===PopupRegion.TITLE_BAR) {
        if (_hideOnResizeWidget!=null) {
            GwtUtil.setStyle(_hideOnResizeWidget, "visibility", "hidden");
        }
        ctx= beginMove(popup,x,y);

    } else if (canExpand) {
        if (popupRegion===PopupRegion.SE_CORNER || popupRegion===PopupRegion.SW_CORNER) {
            ctx= beginResize(popupRegion);
        }
    }
    return ctx;
}

export const humanMove= function(ev,ctx) {
    if (!ctx) return;
    setCursorShape(ctx.popup,ev);
    var x= getAbsoluteX(ev);
    var y= getAbsoluteY(ev);
    if (ctx.moving) {
        ev.preventDefault();
        doMove(ctx,x,y);
    }
    else if (ctx.resizing) {
        ev.preventDefault();
        moveResize(x,y);
    }
}

export const humanStop= function(ev,ctx) {
    if (!ctx) return;
    if (_moving) {
        if (_hideOnResizeWidget!=null) {
            GwtUtil.setStyle(_hideOnResizeWidget, "visibility", "visible");
        }
        ev.preventDefault();
        endMove(ctx);

    }
    else if (_resizing) {
        if (_hideOnResizeWidget!=null) {
            GwtUtil.setStyle(_hideOnResizeWidget, "visibility", "visible");
        }
        ev.preventDefault();
        endResize();
    }
}


var getAbsoluteX= function(ev) {
    if (ev.type===MOUSE_EV) {
        return  ev.clientX+window.scrollLeft;
    }
    if (ev.type===TOUCH_EV) {
        return  targetTouches[0].clientX+window.scrollLeft;
    }
    return 0;
}


var getAbsoluteY= function(ev) {
    if (ev.type===MOUSE_EV) {
        return  ev.clientY+window.scrollLeft;
    }
    if (ev.type===TOUCH_EV) {
        return  targetTouches[0].clientY+window.scrollLeft;
    }
    return 0;
}

/**
 * resize the popup when the browser area is smaller then the popup size
 */
//private void adjustResizable() {
//    if (_expandW!=null && optimalWidth>0 && optimalHeight>0) {
//        int width= content.getOffsetWidth();
//        int height= content.getOffsetHeight();
//
//        if (width> Window.getClientWidth()) {
//            width= Window.getClientWidth()-20;
//        }
//        else if (width<optimalWidth) {
//            width= Math.min(optimalWidth, Window.getClientWidth()-20);
//        }
//
//        if (height> Window.getClientHeight()) {
//            height= Window.getClientHeight()-35;
//        }
//        else if (height<optimalHeight) {
//            height= Math.min(optimalHeight, Window.getClientHeight()-35);
//        }
//
//        if (width<minWidth) {
//            width = minWidth;
//            _doAlign= false; //turn off the automatic align, the dialog will jump around when users scrolls
//        }
//        if (height<minHeight) {
//            height= minHeight;
//            _doAlign= false; //turn off the automatic align, the dialog will jump around when users scrolls
//        }
//
//        if (height!=content.getOffsetHeight() || width!=content.getOffsetWidth()) {
//            content.getWidget().setSize(width+"px", height+"px");
//            ((RequiresResize)_expandW).onResize();
//        }
//
//    }
//}



const beginMove= function(popup, x, y) {
    this.moving= true;
    this.doAlign= false;
    popup.releaseCapture();
    popup.setCapture(true);
    return {
        popup,
        moving: true,
        originalX: getAbsoluteLeft(popup),
        originalY: getAbsoluteTop(popup),
        originalMouseX: x,
        originalMouseY: y
    };
}


const doMove= function(ctx, x, y) {
    if (!ctx || !ctx.moving) return;
    if (x<0) x= 0;
    if (y<0) y= 0;
    if (ctx) {
        var xDiff= x-ctx.originalMouseX;
        var yDiff= y-ctx.originalMouseY;
        var newX=ctx.originalX+xDiff;
        var newY=ctx.originalY+yDiff;
        ctx.popup.style.left= newX;
        ctx.popup.style.top= newY;
    }
}

const endMove= function(ctx) {
    if (!ctx || !ctx.moving) return;
    ctx.popup.releaseCapture();
    ctx.moving= false;
}


const beginResize= function(resizeCorner,popup, ctx) {
    var retval= null;
    if (_expandW!=null) {
        DOM.releaseCapture(popup.getElement());
        DOM.setCapture(popup.getElement());
        var e= _expandW.getElement();

        retval= {
            resizeCorner,
            popup,
            resizing: true,
            doAlign: false,
            xDiff: popup.getOffsetWidth()- e.getOffsetWidth(),
            yDiff: popup.getOffsetHeight()- e.getOffsetHeight(),
            originalX: getAbsoluteLeft(popup),
            originalY: getAbsoluteTop(popup)
        }
    }
}


const moveResize= function(ctx, x, y) {
    if (!ctx || !ctx.resizing) return;
    var newW;
    var newH;
    var e= ctx.expandW;
    var beginWidgetH= e.getOffsetHeight();
    var beginWidgetW= e.getOffsetWidth();
    var beginW= ctx.popup.getOffsetWidth();

    if (ctx.resizeCorner===PopupRegion.SE_CORNER) {
        newW= x- getAbsoluteLeft(popup) - ctx.xDiff;
        newH= y- getAbsoluteTop(popup) - ctx.yDiff;

        if (newW<minWidth) newW= beginWidgetW;
        if (newH<minHeight) newH= beginWidgetH;

        performResize(_expandW, newW,newH);
    }
    else if (ctx.resizeCorner== PopupRegion.SW_CORNER) {
        newW= e.offsetWidth + (popup.getAbsoluteLeft() - x);
        newH= y- popup.getAbsoluteTop() - _yDiff;

        if (newW<minWidth) newW= beginWidgetW;
        if (newH<minHeight) newH= beginWidgetH;

        performResize(_expandW, newW,newH);

        if (beginW!=popup.getOffsetWidth()) setPopupPosition(x, _originalY);

    }
}


const performResize= function(w, width, height) {
}



var findRegion= function(popup,mx, my) {

    var retval= PopupRegion.NONE;

    var x= getAbsoluteLeft(popup);
    var y= getAbsoluteTop(popup);
    var w= popup.offsetWidth;
    var h= popup.offsetHeight;


    var nwCornerCheck= new inRangeCheck(x,y,true,true);
    var neCornerCheck= new inRangeCheck(x+w,y, false, true);
    var swCornerCheck= new inRangeCheck(x,y+h, true, false);
    var seCornerCheck= new inRangeCheck(x+w,y+h, false, false);


    if (neCornerCheck(mx,my))      retval= PopupRegion.NE_CORNER;
    else if (nwCornerCheck(mx,my)) retval= PopupRegion.NW_CORNER;
    else if (seCornerCheck(mx,my)) retval= PopupRegion.SE_CORNER;
    else if (swCornerCheck(mx,my)) retval= PopupRegion.SW_CORNER;

    if (retval===PopupRegion.NONE) { // look for the title bar
        w= titleLabel.offsetWidth;
        h= titleLabel.offsetHeight;
        x= getAbsoluteLeft(titleLabel);
        y= getAbsoluteTop(titleLabel);
        if (mx>=x && mx<=x+w  && my>=y && my<=y+h) {
            retval= PopupRegion.TITLE_BAR;
        }
    }
    return retval;
}




const inRangeCheck = function(x, y, xIncrease, yIncrease) {
    return function(mx, my) {
        var inX;
        var inY;

        if (xIncrease) inX= (mx<=x+MARGIN && mx>=x);
        else            inX= (mx>=x-MARGIN && mx<=x);

        if (yIncrease) inY= (my<=y+MARGIN && my>=y);
        else            inY= (my>=y-MARGIN && my<=y);

        return inX && inY;
    };
};






