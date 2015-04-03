/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


//import {LayoutType} from 'ipac-firefly/ui/PopupPanel.jsx';


"use strict";





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

