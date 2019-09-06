/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {useState, useEffect} from 'react';
import {isEmpty, get,uniq, isEqual} from 'lodash';
import {useStoreConnector} from '../../ui/SimpleComponent';
import CompleteButton from '../../ui/CompleteButton.jsx';
import DialogRootContainer from '../../ui/DialogRootContainer.jsx';
import {PopupPanel} from '../../ui/PopupPanel.jsx';
import {visRoot, dispatchChangeActivePlotView} from '../ImagePlotCntlr.js';
import {primePlot} from '../PlotViewUtil.js';
import {getMultiViewRoot,getExpandedViewerItemIds,dispatchReplaceViewerItems,
                             EXPANDED_MODE_RESERVED, IMAGE} from '../MultiViewCntlr.js';
import {dispatchShowDialog} from '../../core/ComponentCntlr.js';
import {TablePanel} from '../../tables/ui/TablePanel';
import {dispatchTableAddLocal} from '../../tables/TablesCntlr';
import {getTblById, processRequest} from '../../tables/TableUtil';
import {SelectInfo} from '../../tables/SelectInfo';
import {getFormattedWaveLengthUnits, getPlotViewAry, isPlotViewArysEqual} from '../PlotViewUtil';
import {PlotAttribute} from '../PlotAttribute';
import {dispatchDeletePlotView} from '../ImagePlotCntlr';

const TABLE_ID= 'active-image-view-list-table';

export function showExpandedOptionsPopup(plotViewAry) {
    const popup= (
        <PopupPanel title={'Choose which'} >
            <ImageViewOptionsPanel  plotViewAry={plotViewAry}/>
        </PopupPanel>
    );
    DialogRootContainer.defineDialog('ExpandedOptionsPopup', popup);
    dispatchShowDialog('ExpandedOptionsPopup');
}




const [NAME_IDX,WAVE_LENGTH_DESC,PID_IDX,STATUS, PROJ_TYPE_DESC, WAVE_TYPE, DATA_HELP_URL]= [0,1,2,3,4,5,6];

const columnsTemplate = [];
columnsTemplate[NAME_IDX]= {name: 'Name', type: 'char', width: 20};
columnsTemplate[PID_IDX]= {name: 'plotId', type: 'char', width: 10, visibility: 'hidden'};
columnsTemplate[STATUS]= {name: 'Status', type: 'char', width: 15};
columnsTemplate[PROJ_TYPE_DESC]= {name: 'Project', type: 'char', width: 8};
columnsTemplate[WAVE_TYPE]= {name: 'Type', type: 'char', width: 8};
columnsTemplate[WAVE_LENGTH_DESC]= {name: 'Wavelength', type: 'char', width: 10};
columnsTemplate[DATA_HELP_URL]= {name: 'Help', type: 'location', width: 7};



// columnsTemplate[DATA_HELP_URL]= {name: 'Help', type: 'location', width: 7, links:[{ href: '${Help}',  value: 'help'}]};
// columns[DATA_HELP_URL]= {name: 'Help', type: 'location', width: 7};
//
// const helpAllPopulated= {name: 'Help', type: 'location', width: 7, links:[{ href: '${Help}',  value: 'help'}]};
// const helpSomePopulated= {name: 'Help', type: 'location', width: 7};





const getAttribute= (attributes, attribute) => get(attributes,[attribute],'');

const makeEnumValues= (data,idx) => uniq(data.map((d) => d[idx]).filter((d) => d)).join(',');



function makeModel(tbl_id,plotViewAry, expandedIds, oldModel) {

    const selectInfo= SelectInfo.newInstance({rowCount: plotViewAry.length});



    plotViewAry.forEach( ({plotId},idx) => selectInfo.setRowSelect(idx, expandedIds.includes(plotId)) );

    const data= plotViewAry.map( (pv) => {
        const attributes= plot? plot.attributes : pv.request.getAttributes();
        const plot= primePlot(pv);
        const {plotId, serverCall, plottingStatus,request}= pv;
        const title = plot ? plot.title :  request.getTitle() || 'failed image';
        const row= [];
        row[NAME_IDX]=title;
        row[PID_IDX]= plotId;
        row[STATUS]= serverCall==='success' ? 'Success' : plottingStatus;
        row[PROJ_TYPE_DESC]= getAttribute(attributes,PlotAttribute.PROJ_TYPE_DESC);
        row[WAVE_TYPE]= getAttribute(attributes,PlotAttribute.WAVE_TYPE);
        row[WAVE_LENGTH_DESC]= getFormattedWaveLengthUnits(getAttribute(attributes,PlotAttribute.WAVE_LENGTH_DESC),true);
        row[DATA_HELP_URL]= getAttribute(attributes,PlotAttribute.DATA_HELP_URL);
        return row;
    });

    const columns= [...columnsTemplate];
    columns[PROJ_TYPE_DESC].enumVals= makeEnumValues(data,PROJ_TYPE_DESC);
    columns[WAVE_TYPE].enumVals=  makeEnumValues(data,WAVE_TYPE);


    let newModel = {
        tbl_id,
        tableData:{columns,data},
        totalRows: data.length, highlightedRow: 0,
        selectInfo: selectInfo.data,
        tableMeta:  {},
        request: oldModel ? oldModel.request : undefined
    };
    if (newModel.request) {
        newModel = processRequest(newModel, newModel.request, newModel.highlightedRow);
    }
    dispatchTableAddLocal(newModel, undefined, false);
    return newModel;
}

function dialogComplete(tbl_id) {
    const model= getTblById(tbl_id);
    if (!model) return;
    const m= model.origTableModel || model;
    const si= SelectInfo.newInstance(m.selectInfo);

    si.isSelected();

    const plotIdAry= m.tableData.data.map( (d) => d[PID_IDX] ).filter( (d,idx) => si.isSelected(idx));
    if (isEmpty(plotIdAry)) return;
    if (!plotIdAry.includes(visRoot().activePlotId)) {
        dispatchChangeActivePlotView(plotIdAry[0]);
    }
    dispatchReplaceViewerItems(EXPANDED_MODE_RESERVED, plotIdAry, IMAGE);



}


// function isPvAryEqual(oldAry,newAry) {
//     if (!oldAry) return false;
//     if (oldAry.length!==newAry.length) return false;
//     return oldAry.every( (pv,idx) => {
//         const {plotId, serverCall, plottingStatus, request}= pv;
//         const newP= primePlot(pv);
//         const oldP= primePlot(newAry[idx]);
//         if (Boolean(newP)!==Boolean(oldP)) return false;
//         if (newP && oldP) {
//             const plotEqual= (newP.plotImageId===oldP.plotImageId);
//             if (!plotEqual) return false;
//         }
//         return (
//             plotId=== newAry[idx].plotId &&
//             request=== newAry[idx].request &&
//             serverCall===newAry[idx].serverCall &&
//             plottingStatus===newAry[idx].plottingStatus);
//
//     });
// }
//

const pvKeys= ['plotId', 'request', 'serverCall', 'plottingStatus'];
const plotKeys= ['plotImageId'];


function getPvAry(oldPvAry) {
    const pvAry= getPlotViewAry(visRoot());
    if (!oldPvAry) return pvAry;
    return isPlotViewArysEqual(oldPvAry, pvAry,pvKeys,plotKeys) ? oldPvAry : pvAry;
}

function getExpandedIds(oldIdAry) {
    const expandedIds= getExpandedViewerItemIds(getMultiViewRoot());
    return isEqual(oldIdAry,expandedIds) ? oldIdAry : expandedIds;
}


function ImageViewOptionsPanel() {

    const tbl_ui_id =TABLE_ID + '-ui';


    const [plotViewAry,expandedIds] = useStoreConnector(getPvAry, getExpandedIds);
    const [model, setModel] = useState(undefined);


    useEffect(() => {
        const oldModel= getTblById(TABLE_ID);
        setModel(makeModel(TABLE_ID,plotViewAry,expandedIds, oldModel));
    }, [plotViewAry,expandedIds]);

    const someFailed= plotViewAry.some( (pv) => pv.serverCall==='fail');

    const hideFailed= () => {
        if (isEmpty(plotViewAry)) return;
        const plotIdAry= plotViewAry
            .filter( (pv) => pv.serverCall!=='fail')
            .map( (pv) => pv.plotId);
        if (!plotIdAry.includes(visRoot().activePlotId)) {
            dispatchChangeActivePlotView(plotIdAry[0]);
        }
        dispatchReplaceViewerItems(EXPANDED_MODE_RESERVED, plotIdAry, IMAGE);
    };

    const deleteFailed= () => {
        plotViewAry.forEach( (pv) => {
            if (pv.serverCall==='fail') {
                dispatchDeletePlotView({plotId:pv.plotId}) ;
            }
            });
        };

    if (!model) return null;

    return (
        <div style={{resize: 'both', overflow: 'hidden', display: 'flex', flexDirection: 'column',
            width: 500, height: 400, minWidth: 250, minHeight: 200}}>

            <div style={{ position: 'relative', width: '100%', height: '100%'}}>
                <div className='TablePanel'>
                    <div className={'TablePanel__wrapper--border'}>
                        <div className='TablePanel__table' style={{top: 0}}>
                            <TablePanel
                                tbl_ui_id={tbl_ui_id}
                                tableModel={model}
                                showToolbar={false}
                                showFilters={true}
                                selectable={true}
                                showOptionButton={true}
                                border= {false}
                                rowHeight={23}
                            />
                        </div>
                    </div>
                </div>
            </div>


            <div style={{display:'flex', justifyContent:'space-between'}}>
                <CompleteButton
                    style={{padding : 5}}
                    onSuccess={() => dialogComplete(model.tbl_id)}
                    dialogId='ExpandedOptionsPopup' />
                {someFailed &&
                ( <div style={{display:'flex', padding:5}}>
                    <button type='button' className='button std hl'
                            onClick={() => hideFailed()}>Hide Failed
                    </button>
                    <button type='button' className='button std hl'
                            onClick={() => deleteFailed()}>Delete Failed
                    </button>

                </div>)}
            </div>
        </div>
    );
}

