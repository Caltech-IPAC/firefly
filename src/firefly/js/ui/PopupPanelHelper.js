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

var getAbsoluteX= function(ev) {
    if (ev.type===MOUSE_EV) {
        return  ev.clientX+window.scrollX;
    }
    if (ev.type===TOUCH_EV) {
        return  ev.targetTouches[0].clientX+window.scrollX;
    }
    return 0;
};


var getAbsoluteY= function(ev) {
    if (ev.type===MOUSE_EV) {
        return  ev.clientY+window.scrollY;
    }
    if (ev.type===TOUCH_EV) {
        return  ev.targetTouches[0].clientY+window.scrollY;
    }
    return 0;
};


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


/**
 * @param ctx
 * @param x
 * @param y
 * @return {null|{top: number, left: number}}
 */
const doMove= function(ctx, x, y) {
    if (!ctx || !ctx.moving) return null;
    if (x<0) x= 0;
    if (y<0) y= 0;
    const xDiff= x-ctx.originalMouseX;
    const yDiff= y-ctx.originalMouseY;
    let newX=ctx.originalX+xDiff;
    let newY=ctx.originalY+yDiff;
    if (newX+ctx.popup.offsetWidth+10>window.innerWidth+ window.scrollX ) {
        newX= window.innerWidth+window.scrollX-ctx.popup.offsetWidth-10;
    }
    if (newX<2) newX=2;
    if (newY+ctx.popup.offsetHeight>window.innerHeight + window.scrollY ) {
        newY= window.innerHeight+window.scrollY-ctx.popup.offsetHeight;
    }
    if (newY<2) newY=2;
    return {left:newX,top:newY};
};

const endMove= function(ctx) {
    if (!ctx || !ctx.moving) return;
    ctx.moving= false;
};

/**
 * @param {element} e
 * @param layoutType
 * @return {{top: number, left: number}}
 */
export const getDefaultPopupPosition= function(e, layoutType) {

    var left= 0;
    var top= 0;
    switch (layoutType.toString()) {
        case 'CENTER' :
            left= window.innerWidth/2 - e.offsetWidth/2 + window.scrollX;
            top= window.innerHeight/2 - e.offsetHeight/2 + window.scrollY;

            break;
        case 'TOP_CENTER' :
            left= window.innerWidth/2 - e.offsetWidth/2 + window.scrollX;
            top= window.scrollY+ 100;

            break;
        case 'TOP_RIGHT' :
            left= window.innerWidth - e.offsetWidth - 30 + window.scrollX;
            top= window.scrollY+ 3;
            break;
        case 'TOP_LEFT' :
            left= 2 + window.scrollX;
            top= window.scrollY+ 3;
            break;

        case 'TOP_EDGE_CENTER' :
            left= window.innerWidth/2 - e.offsetWidth/2 + window.scrollX;
            top= window.scrollY+ 3;
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
    //console.log('region: ' + popupRegion.key); //-- debug code
    var ctx= null;

    if (popupRegion===PopupRegion.TITLE_BAR) {
        //ev.target.releaseCapture();
        //ev.target.setCapture(true);
        ctx= beginMove(popup,x,y);

    } else if (canExpand) {
        if (popupRegion===PopupRegion.SE_CORNER || popupRegion===PopupRegion.SW_CORNER) {
            //ev.target.releaseCapture();
            //ev.target.setCapture(true);
            //ctx= beginResize(popupRegion); //todo - uncomment and implement beginResize method
        }
    }
    return ctx;
};

export const humanMove= function(ev,ctx,titleBar) {
    if (!ctx) return undefined;
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
        //return moveResize(x,y); //todo implement moveResize and call
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
    else if (ctx.resizing) {
        ev.preventDefault();
        ev.stopPropagation();
        //endResize(); //todo implement endResize and call
    }
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





//---------------------------------------------------
//--- TODO
//--- TODO
//--- TODO
//---- uncomment and implement code below when ware
//---- are ready to resize
//---------------------------------------------------



//const beginResize= function(resizeCorner,popup, ctx) {
//    var retval= null;
//    if (canExpand) {
//        //popup.releaseCapture();
//        //popup.setCapture(true);
//        var e= _expandW.getElement();
//
//        retval= {
//            resizeCorner,
//            popup,
//            resizing: true,
//            doAlign: false,
//            xDiff: popup.offsetWidth- e.offsetWidth,
//            yDiff: popup.offsetHeight- e.offsetHeight,
//            originalX: getAbsoluteLeft(popup),
//            originalY: getAbsoluteTop(popup)
//        };
//    }
//};
//
//
//const moveResize= function(ctx, x, y) {
//    if (!ctx || !ctx.resizing) return;
//    var newW;
//    var newH;
//    var e= ctx.expandW;
//    var beginWidgetH= e.getOffsetHeight();
//    var beginWidgetW= e.getOffsetWidth();
//    var beginW= ctx.popup.getOffsetWidth();
//
//    if (ctx.resizeCorner===PopupRegion.SE_CORNER) {
//        newW= x- getAbsoluteLeft(popup) - ctx.xDiff;
//        newH= y- getAbsoluteTop(popup) - ctx.yDiff;
//
//        if (newW<minWidth) newW= beginWidgetW;
//        if (newH<minHeight) newH= beginWidgetH;
//
//        performResize(_expandW, newW,newH);
//    }
//    else if (ctx.resizeCorner== PopupRegion.SW_CORNER) {
//        newW= e.offsetWidth + (popup.getAbsoluteLeft() - x);
//        newH= y- popup.getAbsoluteTop() - _yDiff;
//
//        if (newW<minWidth) newW= beginWidgetW;
//        if (newH<minHeight) newH= beginWidgetH;
//
//        performResize(_expandW, newW,newH);
//
//        if (beginW !== popup.getOffsetWidth()) setPopupPosition(x, _originalY);
//
//    }
//};
//
//
//const performResize= function(w, width, height) {
//};


//---------------------------------------------------
//--- END TODO
//--- END TODO
//--- END TODO
//---------------------------------------------------


/**
 *
 * @param popup
 * @param titleBar
 * @param mx
 * @param my
 * @return {PopupRegion}
 */
function findRegion(popup,titleBar,mx, my) {

    var retval= PopupRegion.NONE;

    mx+= window.scrollX;
    my+= window.scrollY;
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










