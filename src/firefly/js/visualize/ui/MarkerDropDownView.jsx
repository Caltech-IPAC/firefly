/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import PropTypes from 'prop-types';
import {SingleColumnMenu, DropDownSubMenu} from '../../ui/DropDownMenu.jsx';
import {ToolbarButton,
        DropDownVerticalSeparator} from '../../ui/ToolbarButton.jsx';
import { dispatchCreateMarkerLayer, dispatchCreateFootprintLayer } from '../DrawLayerCntlr.js';
import {visRoot} from '../ImagePlotCntlr.js';
import {FootprintFactory, FootprintList,  SOFIA_INSTRUMENTS} from '../draw/FootprintFactory.js';
import { createNewFootprintLayerId, getFootprintLayerTitle} from '../../drawingLayers/FootprintTool.js';
import {relocatable} from '../../drawingLayers/FootprintLocatable.js';
import {has, get} from 'lodash';
let idCntM = 0;
let idCntF = 0;


const markerItem = {
    marker: {label: 'Marker'}     // TODO: add more items for marker
};

function displayItemText(itemName) {
    if (has(markerItem, itemName)) {
        return `Add ${markerItem[itemName].label}`;
    }
}

const getPlotId = (pv) => ( pv ?  pv.plotId : get(visRoot(), 'activePlotId'));

export function addNewDrawLayer(pv, itemName) {
    if (!has(markerItem, itemName)) return;
    const drawLayerId = `${markerItem[itemName].label}-${idCntM++}`;
    const title = `Marker #${idCntM}`;
    const plotId = getPlotId(pv);        // pv should be true, otherwise marker is disabled.

    dispatchCreateMarkerLayer(drawLayerId, title, plotId, true);
}

export function  addFootprintDrawLayer(pv, {footprint, instrument, relocateBy, fromFile, fromRegionAry}) {
    let drawLayerId, title;

    if (fromFile || fromRegionAry) {
        drawLayerId = createNewFootprintLayerId();
        title = getFootprintLayerTitle(fromFile);
    } else {
        drawLayerId = `${footprint}` + (instrument ? `_${instrument}` : '') + `_${idCntF++}`;
        const instrumentLabel = instrument ? footprint==='SOFIA'? instrument.replace(/_/g, ' '):instrument : ' ';
        title = getFootprintLayerTitle(`Footprint: ${footprint} ${instrumentLabel}`);
    }
    const plotId =  getPlotId(pv);

    dispatchCreateFootprintLayer(drawLayerId, title, {footprint, instrument, relocateBy, fromFile, fromRegionAry}, plotId,  true);
}


export function MarkerDropDownView({plotView:pv}) {
    const enabled = !!pv;
    let  sep = 1;

    const footprintCmdJSX = (text, footprint, instrument) => {
        const key = footprint + (instrument ? `_${instrument}`: '');
        const fpInfo = {footprint, instrument, relocateBy: relocatable.center.key};

        return (<ToolbarButton key={key}
                               text={text}
                               enabled={enabled} horizontal={false}
                               onClick={() => addFootprintDrawLayer(pv, fpInfo)}/>);
    };


    const footprints = FootprintList.map((fp) => {
            const fpDesc = FootprintFactory.footprintDesc(fp);
            const instruments = FootprintFactory.getInstruments(fp);
            if (fp === 'SOFIA'){


                var innerDropDownMenu=[],sofiaSubMenuItems=[];
                for (let i=0; i<instruments.length; i++){
                    sofiaSubMenuItems = SOFIA_INSTRUMENTS[instruments[i]]?SOFIA_INSTRUMENTS[instruments[i]].enums.map((item) => footprintCmdJSX(item.value, fp, item.key)):[];

                    if (sofiaSubMenuItems.length>0){
                        innerDropDownMenu[i]= <DropDownSubMenu key={instruments[i]} text={instruments[i]}>{sofiaSubMenuItems} </DropDownSubMenu>;

                    }
                    else {
                        innerDropDownMenu[i]=footprintCmdJSX(instruments[i], fp, instruments[i]);
                    }
                }


                return <DropDownSubMenu key={fp} text={`Add ${fpDesc} footprint`}>{innerDropDownMenu}</DropDownSubMenu>;
            }
            else {
                 if (instruments && instruments.length > 0) {
                    const items = instruments.map((inst) => footprintCmdJSX(inst, fp, inst));

                    //SPITZER doesn't have any full footprint to overlay, skip
                    if (fpDesc && fp !== 'SPITZER'  &&  fp!== 'SOFIA'){
                        items.splice(0, 0, footprintCmdJSX('All', fp));  // add to the beginning of the array.
                    }


                    return <DropDownSubMenu key={fp} text={`Add ${fpDesc} footprint`}> {items} </DropDownSubMenu>;
                }
                else {
                    return footprintCmdJSX(`Add ${fpDesc} footprint`, fp);
                }

          }
        });

    return (
        <SingleColumnMenu>
            <ToolbarButton key={'marker'}
                           text={displayItemText('marker')}
                           enabled={enabled} horizontal={false}
                           onClick={()=> addNewDrawLayer(pv, 'marker')}/>
            <DropDownVerticalSeparator key={sep++}/>
            {footprints}
        </SingleColumnMenu>
    );
}

MarkerDropDownView.propTypes= {
    plotView : PropTypes.object
};
