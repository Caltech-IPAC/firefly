/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import {getWorkspaceConfig} from 'firefly/visualize/WorkspaceCntlr.js';
import {PopupPanel} from 'firefly/ui/PopupPanel.jsx';
import DialogRootContainer from 'firefly/ui/DialogRootContainer.jsx';
import {dispatchHideDialog, dispatchShowDialog} from 'firefly/core/ComponentCntlr.js';
import {isString} from 'lodash';
import CompleteButton from 'firefly/ui/CompleteButton.jsx';
import HelpIcon from 'firefly/ui/HelpIcon.jsx';
import {FieldGroup} from 'firefly/ui/FieldGroup.jsx';
import {ValidationField} from 'firefly/ui/ValidationField.jsx';
import {downloadBlob, makeDefaultDownloadFileName} from 'firefly/util/fetch.js';


const DIALOG_ID= 'plotDownloadDialog';

export function showPlotLySaveDialog(Plotly, chartDiv, chartId) {
    // if (fileLocation === WORKSPACE) dispatchWorkspaceUpdate(); //todo keep if we add workspace support, it probably goes somewhere else

    const isWs = getWorkspaceConfig(); //todo - keep if we add workspace support
    const  popup = (
        <PopupPanel title={'Save Chart'}>
                <PlotLySavePanel {...{Plotly, chartDiv, chartId, isWs, filename:getDefaultFilename(chartDiv,chartId)}}/>
        </PopupPanel>
    );
    DialogRootContainer.defineDialog(DIALOG_ID, popup);
    dispatchShowDialog(DIALOG_ID);
}

function generateTitleFromAxis(layout) {
    const xaxis= layout.xaxis?.title?.text;
    const yaxis= layout.yaxis?.title?.text;
    return (isString(xaxis) || isString(yaxis)) ? `${isString(xaxis)?xaxis:''} ${isString(yaxis)?yaxis:''}` : undefined;
}

function getDefaultFilename(chartDiv)  {
    const {layout}= chartDiv;
    const title=  isString(layout.title) ? layout.title : layout.title?.text;
    const axisTitle= generateTitleFromAxis(layout);
    return makeDefaultDownloadFileName('chart',title??axisTitle,'png');
}


async function saveFile(request, Plotly, chartDiv) {
    const filename= request.filename.toLowerCase().endsWith('.png') ? request.filename : request.filename+'.png';
    const dataurl= await Plotly.toImage(chartDiv, {format: 'png'});
    const blob = await (await window.fetch(dataurl)).blob();
    downloadBlob(blob,filename);
}


const PlotLySavePanel= function( {isWs,Plotly, chartDiv, chartId, filename}) {
    return (
        <FieldGroup groupKey={'PlotLySaveField'} style={{display:'flex', flexDirection:'column'}}>
            <div style={ {padding: 10}}>
                <ValidationField
                    wrapperStyle={{marginTop: 10}} size={50} fieldKey={'filename'}
                    initialState= {{
                        value: filename,
                        tooltip: 'Enter filename of chart png',
                        label: 'Chart Filename:',
                        labelWidth: 80
                    }} />
            </div>
            <div style={{textAlign:'center', display:'flex', justifyContent:'space-between', padding: '15px 15px 7px 8px'}}>
                <div style={{display:'flex', justifyContent:'space-between'}}>
                    <CompleteButton text='Save' dialogId={DIALOG_ID}
                                    onSuccess={(request) => saveFile(request,Plotly, chartDiv, chartId)} />
                    <CompleteButton text='Cancel' groupKey='' style={{paddingLeft:10}}
                                    onSuccess={() => dispatchHideDialog(DIALOG_ID)} />
                </div>
                <HelpIcon helpId='charts.save' style={{display:'flex', alignItems:'center'}}/>
            </div>
        </FieldGroup>
    );
}