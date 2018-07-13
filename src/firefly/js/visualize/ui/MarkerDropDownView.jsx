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
import {FootprintFactory, FootprintList} from '../draw/FootprintFactory.js';
import {has, get} from 'lodash';

var idCntM = 0;
var idCntF = 0;

const markerItem = {
    marker: {label: 'Marker'}     // TODO: add more items for marker
};

function displayItemText(itemName) {
    if (has(markerItem, itemName)) {
        return `Add ${markerItem[itemName].label}`;
    }
}

var getPlotId = (pv) => ( pv ?  pv.plotId : get(visRoot(), 'activePlotId'));

export function addNewDrawLayer(pv, itemName) {
    if (!has(markerItem, itemName)) return;
    var drawLayerId = `${markerItem[itemName].label}-${idCntM++}`;
    var title = `Marker #${idCntM}`;
    var plotId = getPlotId(pv);        // pv should be true, otherwise marker is disabled.

    dispatchCreateMarkerLayer(drawLayerId, title, plotId, true);
}

export function addFootprintDrawLayer(pv, {footprint, instrument}) {
    var drawLayerId = `${footprint}` + (instrument ? `_${instrument}` :'') + `_${idCntF++}`;
    var title = `Footprint: ${footprint} ` + (instrument ? `${instrument}` :'');
    var plotId =  getPlotId(pv);

    dispatchCreateFootprintLayer(drawLayerId, title, footprint, instrument, plotId,  true);
}

export function MarkerDropDownView({plotView:pv}) {
    const enabled = !!pv;
    var sep = 1;

    const footprintCmdJSX = (text, footprint, instrument) => {
        var key = footprint + (instrument ? `_${instrument}`: '');
        var fpInfo = {footprint, instrument};

        return (<ToolbarButton key={key}
                               text={text}
                               enabled={enabled} horizontal={false}
                               onClick={() => addFootprintDrawLayer(pv, fpInfo)}/>);
    };

    const footprints = FootprintList.map((fp) => {
            const fpDesc = FootprintFactory.footprintDesc(fp);
            const instruments = FootprintFactory.getInstruments(fp);

            if (instruments && instruments.length > 0) {
                const items = instruments.map((inst) => footprintCmdJSX(inst, fp, inst));

                //SPITZER doesn't have any full footprint to overlay, skip
                if (fpDesc && fp !== 'SPITZER') {
                    items.splice(0, 0, footprintCmdJSX('All', fp));  // add to the beginning of the array.
                }

                return <DropDownSubMenu key={fp} text={`Add ${fpDesc} footprint`}> {items} </DropDownSubMenu>;
            } else {
                return footprintCmdJSX(`Add ${fpDesc} footprint`, fp);
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
