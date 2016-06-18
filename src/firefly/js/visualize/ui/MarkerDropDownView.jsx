/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PropTypes} from 'react';
import {SingleColumnMenu} from '../../ui/DropDownMenu.jsx';
import {ToolbarButton,
        DropDownVerticalSeparator} from '../../ui/ToolbarButton.jsx';
import { dispatchCreateMarkerLayer, dispatchCreateFootprintLayer } from '../DrawLayerCntlr.js';
import {visRoot} from '../ImagePlotCntlr.js';
import {FootprintFactory, FootprintList} from '../draw/FootprintFactory.js';
import {has, get} from 'lodash';

var idCnt=0;

const markerItem = {
    marker: {label: 'Marker'}     // TODO: add more items for footprint cases
};

function displayItemText(itemName) {
    if (has(markerItem, itemName)) {
        return `Add ${markerItem[itemName].label}`;
    }
}

var getPlotId = (pv) => ( pv ?  pv.plotId : get(visRoot(), 'activePlotId'));

export function addNewDrawLayer(pv, itemName) {
    if (!has(markerItem, itemName)) return;
    var drawLayerId = `${markerItem[itemName].label}-${idCnt++}`;
    var plotId = getPlotId(pv);        // pv should be true, otherwise marker is disabled.

    dispatchCreateMarkerLayer(drawLayerId, drawLayerId, plotId, true);
}

export function addFootprintDrawLayer(pv, {footprint, instrument}) {
    var drawLayerId = `${footprint}` + (instrument ? `_${instrument}` :'') + `_${idCnt++}`;
    var plotId =  getPlotId(pv);

    dispatchCreateFootprintLayer(drawLayerId, drawLayerId, footprint, instrument, plotId,  true);
}

export function MarkerDropDownView({plotView:pv}) {
    var enabled = !!pv;
    var sep = 1;


    var footprintCommands = () => {
        var footprintCmdJSX = (text, footprint, instrument) => {
            var addText = `Add ${text}`;
            var key = footprint + (instrument ? `_${instrument}`: '');
            var fpInfo = {footprint, instrument};

            return (<ToolbarButton key={key}
                                   text={addText}
                                   enabled={enabled} horizontal={false}
                                   onClick={() => addFootprintDrawLayer(pv, fpInfo)}/>);
        };

        return FootprintList.reduce((prev, fp) => {
            var cmd = FootprintFactory.footprintCommand(fp);

            if (cmd) prev.push(footprintCmdJSX(cmd, fp));
            FootprintFactory.getInstruments(fp).reduce((cmdList, inst) => {
                    cmd = FootprintFactory.instrumentCommand(fp, inst);
                    if (cmd) cmdList.push(footprintCmdJSX(cmd, fp, inst));
                    return cmdList;
                }, prev);

            prev.push(<DropDownVerticalSeparator key={sep++}/>);
            return prev;
        }, []);
    };

    return (
        <SingleColumnMenu>
            <ToolbarButton key={'marker'}
                           text={displayItemText('marker')}
                           enabled={enabled} horizontal={false}
                           onClick={()=> addNewDrawLayer(pv, 'marker')}/>
            <DropDownVerticalSeparator key={sep++}/>
            {footprintCommands()}
        </SingleColumnMenu>
    );
}

MarkerDropDownView.propTypes= {
    plotView : PropTypes.object
};
