/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PropTypes} from 'react';
import {
    ToolbarButton,
    DropDownVerticalSeparator,
    } from '../../ui/ToolbarButton.jsx';
import {SingleColumnMenu} from '../../ui/DropDownMenu.jsx';
import {dispatchColorChange} from '../ImagePlotCntlr.js';
import {primePlot, getPlotViewIdListInGroup, isThreeColor} from '../PlotViewUtil.js';
import {visRoot} from '../ImagePlotCntlr.js';
import {showInfoPopup} from '../../ui/PopupUtil.jsx';



import ColorTable0 from 'html/images/cbar/ct-0-gray.png';
import ColorTable1 from 'html/images/cbar/ct-1-reversegray.png';
import ColorTable2 from 'html/images/cbar/ct-2-colorcube.png';
import ColorTable3 from 'html/images/cbar/ct-3-spectrum.png';
import ColorTable4 from 'html/images/cbar/ct-4-false.png';
import ColorTable5 from 'html/images/cbar/ct-5-reversefalse.png';
import ColorTable6 from 'html/images/cbar/ct-6-falsecompressed.png';
import ColorTable7 from 'html/images/cbar/ct-7-difference.png';
import ColorTable8 from 'html/images/cbar/ct-8-a-ds9.png';
import ColorTable9 from 'html/images/cbar/ct-9-b-ds9.png';
import ColorTable10 from 'html/images/cbar/ct-10-bb-ds9.png';
import ColorTable11 from 'html/images/cbar/ct-11-he-ds9.png';
import ColorTable12 from 'html/images/cbar/ct-12-i8-ds9.png';
import ColorTable13 from 'html/images/cbar/ct-13-aips-ds9.png';
import ColorTable14 from 'html/images/cbar/ct-14-sls-ds9.png';
import ColorTable15 from 'html/images/cbar/ct-15-hsv-ds9.png';
import ColorTable16 from 'html/images/cbar/ct-16-heat-ds9.png';
import ColorTable17 from 'html/images/cbar/ct-17-cool-ds9.png';
import ColorTable18 from 'html/images/cbar/ct-18-rainbow-ds9.png';
import ColorTable19 from 'html/images/cbar/ct-19-standard-ds9.png';
import ColorTable20 from 'html/images/cbar/ct-20-staircase-ds9.png';
import ColorTable21 from 'html/images/cbar/ct-21-color-ds9.png';

//=================================

const colorTables=[
    {
        icon: ColorTable0,
        tip:'Gray Scale'
    },
    {
        icon: ColorTable1,
        tip: 'Reverse Gray Scale'
    },
    {
        icon: ColorTable2,
        tip: 'Color Cube'
    },
    {
        icon: ColorTable3,
        tip: 'Spectrum'
    },
    {
        icon: ColorTable4,
        tip: 'For False Color'
    },
    {
        icon: ColorTable5,
        tip: 'For False Color - Reversed'
    },
    {
        icon: ColorTable6,
        tip: 'For False Color - Compressed'
    },
    {
        icon: ColorTable7,
        tip: 'For difference images'
    },
    {
        icon: ColorTable8,
        tip: `DS9's a color bar`
    },
    {
        icon: ColorTable9,
        tip: `DS9's b color bar`
    },
    {
        icon: ColorTable10,
        tip: `DS9's bb color bar`
    },
    {
        icon: ColorTable11,
        tip: `DS9's he color bar`
    },
    {
        icon: ColorTable12,
        tip: `DS9's i8 color bar`
    },
    {
        icon: ColorTable13,
        tip: `DS9's aips color bar`
    },
    {
        icon: ColorTable14,
        tip: `DS9's sls color bar`
    },
    {
        icon: ColorTable15,
        tip: `DS9's hsv color bar`
    },
    {
        icon: ColorTable16,
        tip: 'Heat (ds9)'
    },
    {
        icon: ColorTable17,
        tip: 'Cool (ds9)'
    },
    {
        icon: ColorTable18,
        tip: 'Rainbow (ds9)'
    },
    {
        icon: ColorTable19,
        tip: 'Standard (ds9)'
    },
    {
        icon: ColorTable20,
        tip: 'Staircase (ds9)'
    },
    {
        icon: ColorTable21,
        tip: 'Color (ds9)'
    }
];



//====================================

function makeItems(pv,ctAry) {
    return ctAry.map( (ct,cbarIdx) => {
        return (
            <ToolbarButton icon={ct.icon} tip={ct.tip}
                           enabled={true} horizontal={false} key={cbarIdx}
                           onClick={() => handleColorChange(pv,cbarIdx)}/>
        );
    });
}


const isAllThreeColor= (vr,plotIdAry) => plotIdAry.every( (id) => isThreeColor(primePlot(vr,id)));

function handleColorChange(pv,cbarId) {
    var vr= visRoot();
    var plotIdAry= getPlotViewIdListInGroup(vr,pv);

    if (isAllThreeColor(vr,plotIdAry)) {
        showInfoPopup('This is a three color plot, you can not change the color.', 'Color change not allowed');
    }
    else {
        dispatchColorChange({plotId:pv.plotId,cbarId});
    }
}


export function ColorTableDropDownView({plotView:pv}) {
    return (
        <SingleColumnMenu>
            {makeItems(pv,colorTables)}
        </SingleColumnMenu>
        );

}

ColorTableDropDownView.propTypes= {
    plotView : PropTypes.object
};
