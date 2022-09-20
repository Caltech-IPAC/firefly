/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import Enum from 'enum';
export const PopupRegion = new Enum(['NW_CORNER', 'NE_CORNER', 'SE_CORNER', 'SW_CORNER', 'TITLE_BAR', 'NONE']);

const canExpand= true;
const MARGIN= 10;
const MOUSE_EV= 'mouse';
const TOUCH_EV= 'touch';

function getAbsoluteLeft(elem) {
    let left = 0;
    let curr = elem;
    // This intentionally excludes body which has a null offsetParent.
    while (curr.offsetParent) {
        left -= curr.scrollLeft;
        curr = curr.parentNode;
    }
    while (elem) {
        left += elem.offsetLeft;
        elem = elem.offsetParent;
    }
    return left;
}

function getAbsoluteTop(elem) {
    let top = 0;
    let curr = elem;
    // This intentionally excludes body which has a null offsetParent.
    while (curr.offsetParent) {
        top -= curr.scrollTop;
        curr = curr.parentNode;
    }
    while (elem) {
        top += elem.offsetTop;
        elem = elem.offsetParent;
    }
    return top;
}

const inRangeCheck = (x, y, xIncrease, yIncrease) =>
    (mx, my) => {
        const inX = xIncrease ? (mx<=x+MARGIN && mx>=x) : (mx>=x-MARGIN && mx<=x);
        const inY = yIncrease ? (my<=y+MARGIN && my>=y) : (my>=y-MARGIN && my<=y);
        return inX && inY;
    };

const getAbsoluteX= function(ev) {
    if (ev.type===MOUSE_EV) return  ev.clientX+window.scrollX;
    if (ev.type===TOUCH_EV) return  ev.targetTouches[0].clientX+window.scrollX;
    return 0;
};


const getAbsoluteY= function(ev) {
    if (ev.type===MOUSE_EV) return  ev.clientY+window.scrollY;
    if (ev.type===TOUCH_EV) return  ev.targetTouches[0].clientY+window.scrollY;
    return 0;
};

const beginMove = (popup, x, y) => (
    {
        popup,
        moving: true,
        originalX: getAbsoluteLeft(popup),
        originalY: getAbsoluteTop(popup),
        originalMouseX: x,
        originalMouseY: y
    });


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

const endMove= (ctx) => void (ctx && (ctx.moving= false));

/**
 * @param {element} e
 * @param layoutType
 * @param {element} positionElement
 * @param {number} topOffset - add to the top computation
 * @param {number} leftOffset - add to the left computation
 * @return {{top: number, left: number}}
 */
export const getDefaultPopupPosition= function(e, layoutType,positionElement, topOffset=0, leftOffset=0) {

    let left= 0;
    let top= 0;
    const {offsetWidth=0,offsetHeight=0}= e??{};
    const posBound= positionElement?.getBoundingClientRect() ?? {};
    switch (layoutType.toString()) {
        case 'CENTER' :
            left= window.innerWidth/2 - offsetWidth/2 + window.scrollX + leftOffset;
            top= window.innerHeight/2 - offsetHeight/2 + window.scrollY + topOffset;
            break;
        case 'TOP_CENTER' :
            left= window.innerWidth/2 - offsetWidth/2 + window.scrollX + leftOffset;
            top= window.scrollY+ 100 + topOffset;
            break;
        case 'TOP_RIGHT' :
            left= window.innerWidth - offsetWidth - 30 + window.scrollX + leftOffset;
            top= window.scrollY+ 3 + topOffset;
            break;
        case 'TOP_LEFT' :
            left= 2 + window.scrollX + leftOffset;
            top= window.scrollY+ 3 + topOffset;
            break;
        case 'TOP_EDGE_CENTER' :
            left= window.innerWidth/2 - offsetWidth/2 + window.scrollX + leftOffset;
            top= window.scrollY+ 3 + topOffset;
            break;
        case 'TOP_RIGHT_OF_BUTTON' :
            if (!positionElement) return getDefaultPopupPosition(e,'TOP_RIGHT');
            left= (posBound.width + posBound.x + 5  + offsetWidth < window.innerWidth ?
                posBound.width + posBound.x + 5 : window.innerWidth - offsetWidth) + leftOffset;
            top= (posBound.y > offsetHeight ? posBound.y - offsetHeight : 5) + topOffset;
            break;
    }
    return {left, top};
};

const setCursorShape= function(popup,titleBar,ev) {
    if (!canExpand) return;
    const r= findRegion(popup,titleBar,getAbsoluteX(ev),getAbsoluteY(ev));
    switch (r) {
        case PopupRegion.SE_CORNER: popup.style.cursor='se-resize'; break;
        case PopupRegion.SW_CORNER: popup.style.cursor='sw-resize'; break;
        default:                    popup.style.cursor='auto'; break;
    }
};

export const humanStart= function(ev,popup,titleBar) {
    ev.preventDefault();
    ev.stopPropagation();
    const {clientX:x,clientY:y}= ev;
    const popupRegion= findRegion(popup,titleBar,x,y);

    if (popupRegion===PopupRegion.TITLE_BAR) {
        return beginMove(popup,x,y);
    } else if (canExpand) {
        if (popupRegion===PopupRegion.SE_CORNER || popupRegion===PopupRegion.SW_CORNER) {
            //ev.target.releaseCapture();
            //ev.target.setCapture(true);
            //return beginResize(popupRegion); //todo - uncomment and implement beginResize method
        }
    }
    return undefined;
};

export const humanMove= function(ev,ctx,titleBar) {
    if (!ctx) return undefined;
    setCursorShape(ctx.popup,titleBar,ev);
    const {clientX:x,clientY:y}= ev;
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
 * @param popup
 * @param titleBar
 * @param mx
 * @param my
 * @return {PopupRegion}
 */
function findRegion(popup,titleBar,mx, my) {
    mx+= window.scrollX;
    my+= window.scrollY;
    let x= getAbsoluteLeft(popup);
    let y= getAbsoluteTop(popup);
    let {offsetWidth:w, offsetHeight:h}= popup;

    const nwCornerCheck= inRangeCheck(x,y,true,true);
    const neCornerCheck= inRangeCheck(x+w,y, false, true);
    const swCornerCheck= inRangeCheck(x,y+h, true, false);
    const seCornerCheck= inRangeCheck(x+w,y+h, false, false);

    if (neCornerCheck(mx,my))      return PopupRegion.NE_CORNER;
    else if (nwCornerCheck(mx,my)) return PopupRegion.NW_CORNER;
    else if (seCornerCheck(mx,my)) return PopupRegion.SE_CORNER;
    else if (swCornerCheck(mx,my)) return PopupRegion.SW_CORNER;

    w= titleBar.offsetWidth;
    h= titleBar.offsetHeight;
    x= getAbsoluteLeft(titleBar);
    y= getAbsoluteTop(titleBar);
    if (mx>=x && mx<=x+w  && my>=y && my<=y+h) return PopupRegion.TITLE_BAR;

    return PopupRegion.NONE;
}
