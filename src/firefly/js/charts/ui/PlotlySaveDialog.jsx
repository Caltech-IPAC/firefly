/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import {Button,Stack, Typography} from '@mui/joy';
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
const dialogWidth = '32rem';
const dialogHeight = '10rem';
const labelWidth = '8rem';
const popupPanelResizableStyle = {
    width: dialogWidth,
    height: dialogHeight,
    minWidth: dialogWidth,
    minHeight: dialogHeight,
    resize: 'both',
    overflow: 'hidden',
    position: 'relative'
};

export function showPlotLySaveDialog(Plotly, chartDiv) {
    // if (fileLocation === WORKSPACE) dispatchWorkspaceUpdate(); //todo keep if we add workspace support, it probably goes somewhere else

    const isWs = getWorkspaceConfig(); //todo - keep if we add workspace support
    const  popup = (
        <PopupPanel title={'Save Chart'}>
            <div style={{...popupPanelResizableStyle}}>
                <PlotLySavePanel {...{Plotly, chartDiv, isWs, filename:getDefaultFilename(chartDiv)}}/>
            </div>
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


const PlotLySavePanel= function( {isWs,Plotly, chartDiv, filename}) {
    return (
        <FieldGroup groupKey={'PlotLySaveField'} >
            <Stack spacing={2}
                sx={{px: 2, justifyContent: 'center', width: '95%', height: dialogHeight}}>
                <Stack sx={{'.MuiFormLabel-root': {width: labelWidth}}}>
                    <ValidationField
                        fieldKey={'filename'}
                        initialState= {{
                            value: filename,
                            tooltip: 'Enter filename of chart png',
                            label: 'Chart Filename',
                        }} />
                </Stack>
                <Stack mb={3} sx={{flexDirection: 'row', justifyContent: 'space-between'}}>
                    <Stack spacing={1} direction='row' alignItems='center'>
                        <CompleteButton text='Save' dialogId={DIALOG_ID}
                            onSuccess={(request) => saveFile(request,Plotly, chartDiv)} />
                        <CompleteButton text='Cancel' groupKey=''
                            onSuccess={() => dispatchHideDialog(DIALOG_ID)} />
                    </Stack>
                    <Stack>
                        <HelpIcon helpId={'chart.save'}/>
                    </Stack>
                </Stack>
            </Stack>

        </FieldGroup>
    );
};