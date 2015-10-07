/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */



import Enum from 'enum';
import {getAbsoluteLeft, getAbsoluteTop} from '../util/BrowserUtil.js';

export const PopupRegion = new Enum(['NW_CORNER', 'NE_CORNER', 'SE_CORNER', 'SW_CORNER', 'TITLE_BAR', 'NONE']);

var canExpand= true;
const MARGIN= 10;

const MOUSE_EV= 'mouse';
const TOUCH_EV= 'touch';

export const getPopupPosition= function(e,layoutType) {

    var left= 0;
    var top= 0;
    switch (layoutType.toString()) {
        case 'CENTER' :
            left= window.innerWidth/2 - e.offsetWidth/2;
            top= window.innerHeight/2 - e.offsetHeight/2;

            break;
        case 'TOP_CENTER' :
            left= window.innerWidth/2 - e.offsetWidth/2;
            top= 100;

            break;

    }

    return {left, top};
    //return {left : left +"px", top : top+"px"};
};






var setCursorShape= function(popup,titleBar,ev) {
    if (!canExpand) return;
    var x= getAbsoluteX(ev);
    var y= getAbsoluteY(ev);
    var r= findRegion(popup,titleBar,x,y);
    if (r===PopupRegion.SE_CORNER) {
        popup.style.cursor='se-resize';
    }
    else if (r===PopupRegion.SW_CORNER) {
        popup.style.cursor='sw-resize';
    }
    else {
        popup.style.cursor='auto';
    }
};

export const humanStart= function(ev,popup,titleBar) {
    ev.preventDefault();
    ev.stopPropagation();
    var x= ev.clientX;
    var y= ev.clientY;
    var popupRegion= findRegion(popup,titleBar,x,y);
    console.log('region: ' + popupRegion.key);
    var ctx= null;

    if (popupRegion===PopupRegion.TITLE_BAR) {
        //ev.target.releaseCapture();
        //ev.target.setCapture(true);
        ctx= beginMove(popup,x,y);

    } else if (canExpand) {
        if (popupRegion===PopupRegion.SE_CORNER || popupRegion===PopupRegion.SW_CORNER) {
            //ev.target.releaseCapture();
            //ev.target.setCapture(true);
            ctx= beginResize(popupRegion);
        }
    }
    return ctx;
};

export const humanMove= function(ev,ctx,titleBar) {
    if (!ctx) return null;
    setCursorShape(ctx.popup,titleBar,ev);
    var x= ev.clientX;
    var y= ev.clientY;
    if (ctx.moving) {
        ev.preventDefault();
        ev.stopPropagation();
        return doMove(ctx,x,y);
    }
    else if (ctx.resizing) {
        ev.preventDefault();
        ev.stopPropagation();
        return moveResize(x,y);
    }
};

export const humanStop= function(ev,ctx) {
    if (!ctx) return;
    if (ctx.moving) {
        ev.preventDefault();
        ev.stopPropagation();
        //ev.target.releaseCaptures();
        endMove(ctx);

    }
    else if (cx.resizing) {
        ev.preventDefault();
        ev.stopPropagation();
        //ev.target.releaseCaptures();
        endResize();
    }
}


var getAbsoluteX= function(ev) {
    if (ev.type===MOUSE_EV) {
        return  ev.clientX+document.scrollLeft;
    }
    if (ev.type===TOUCH_EV) {
        return  ev.targetTouches[0].clientX+document.scrollLeft;
    }
    return 0;
};


var getAbsoluteY= function(ev) {
    if (ev.type===MOUSE_EV) {
        return  ev.clientY+document.scrollLeft;
    }
    if (ev.type===TOUCH_EV) {
        return  ev.targetTouches[0].clientY+window.scrollLeft;
    }
    return 0;
};

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
    return {
        popup,
        moving: true,
        originalX: getAbsoluteLeft(popup),
        originalY: getAbsoluteTop(popup),
        originalMouseX: x,
        originalMouseY: y
    };
};


const doMove= function(ctx, x, y) {
    if (!ctx || !ctx.moving) return null;
    if (x<0) x= 0;
    if (y<0) y= 0;
    if (ctx) {
        var xDiff= x-ctx.originalMouseX;
        var yDiff= y-ctx.originalMouseY;
        var newX=ctx.originalX+xDiff;
        var newY=ctx.originalY+yDiff;
        if (newX+ctx.popup.offsetWidth>window.innerWidth ) {
            newX= window.innerWidth-ctx.popup.offsetWidth;
        }
        if (newX<2) newX=2;
        if (newY+ctx.popup.offsetHeight>window.innerHeight ) {
            newY= window.innerHeight-ctx.popup.offsetHeight;
        }
        if (newY<2) newY=2;

        return {newX,newY};
        //ctx.popup.style.left= newX+"px";
        //ctx.popup.style.top= newY+"px";
    }
    return null;
}

const endMove= function(ctx) {
    if (!ctx || !ctx.moving) return;
    //ctx.popup.releaseCapture();
    ctx.moving= false;
}


const beginResize= function(resizeCorner,popup, ctx) {
    var retval= null;
    if (canExpand) {
        //popup.releaseCapture();
        //popup.setCapture(true);
        var e= _expandW.getElement();

        retval= {
            resizeCorner,
            popup,
            resizing: true,
            doAlign: false,
            xDiff: popup.offsetWidth- e.offsetWidth,
            yDiff: popup.offsetHeight- e.offsetHeight,
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

        if (beginW !== popup.getOffsetWidth()) setPopupPosition(x, _originalY);

    }
};


const performResize= function(w, width, height) {
};


/**
 *
 * @param popup
 * @param titleBar
 * @param mx
 * @param my
 * @return {PopupRegion}
 */
var findRegion= function(popup,titleBar,mx, my) {

    var retval= PopupRegion.NONE;

    var x= getAbsoluteLeft(popup);
    var y= getAbsoluteTop(popup);
    var w= popup.offsetWidth;
    var h= popup.offsetHeight;


    var nwCornerCheck= inRangeCheck(x,y,true,true);
    var neCornerCheck= inRangeCheck(x+w,y, false, true);
    var swCornerCheck= inRangeCheck(x,y+h, true, false);
    var seCornerCheck= inRangeCheck(x+w,y+h, false, false);


    if (neCornerCheck(mx,my))      retval= PopupRegion.NE_CORNER;
    else if (nwCornerCheck(mx,my)) retval= PopupRegion.NW_CORNER;
    else if (seCornerCheck(mx,my)) retval= PopupRegion.SE_CORNER;
    else if (swCornerCheck(mx,my)) retval= PopupRegion.SW_CORNER;

    if (retval===PopupRegion.NONE) { // look for the title bar
        w= titleBar.offsetWidth;
        h= titleBar.offsetHeight;
        x= getAbsoluteLeft(titleBar);
        y= getAbsoluteTop(titleBar);
        if (mx>=x && mx<=x+w  && my>=y && my<=y+h) {
            retval= PopupRegion.TITLE_BAR;
        }
    }
    return retval;
};




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






