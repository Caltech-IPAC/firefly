/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {useEffect} from 'react';
import {Box, Button, Stack} from '@mui/joy';
import {getWorkspaceConfig} from 'firefly/visualize/WorkspaceCntlr.js';
import {PopupPanel} from 'firefly/ui/PopupPanel.jsx';
import DialogRootContainer from 'firefly/ui/DialogRootContainer.jsx';
import {dispatchHideDialog, dispatchShowDialog} from 'firefly/core/ComponentCntlr.js';
import {isArray, isString} from 'lodash';
import CompleteButton from 'firefly/ui/CompleteButton.jsx';
import HelpIcon from 'firefly/ui/HelpIcon.jsx';
import {FieldGroup} from 'firefly/ui/FieldGroup.jsx';
import {ValidationField} from 'firefly/ui/ValidationField.jsx';
import {downloadBlob, makeDefaultDownloadFileName} from 'firefly/util/fetch.js';
import {RadioGroupInputField} from 'firefly/ui/RadioGroupInputField';
import {useFieldGroupValue} from 'firefly/ui/SimpleComponent';
import {isDefined} from 'firefly/util/WebUtil';

const DIALOG_ID = 'plotDownloadDialog';

export function showPlotLySaveDialog(Plotly, chartDiv) {
    const isWs = getWorkspaceConfig();
    const popup = (
        <PopupPanel title={'Save Chart'}>
            <Box sx={{
                minWidth: '32rem',
                minHeight: '10rem',
                height: '10rem',
                resize: 'both',
                overflow: 'hidden',
                position: 'relative'
            }}>
                <PlotLySavePanel {...{Plotly, chartDiv, isWs, filename: getDefaultFilename(chartDiv, 'png'),
                    format: 'png'}}/>
            </Box>
        </PopupPanel>
    );
    DialogRootContainer.defineDialog(DIALOG_ID, popup);
    dispatchShowDialog(DIALOG_ID);
}

function generateTitleFromAxis(layout) {
    const xaxis = layout?.xaxis?.title?.text;
    const yaxis = layout?.yaxis?.title?.text;
    return [xaxis, yaxis].filter(Boolean).join(' ');
}

function getDefaultFilename(chartDiv, fmt='png')  {
    const layout = chartDiv?.layout;
    const title = isString(layout?.title) ? layout.title : layout?.title?.text;
    const axisTitle = generateTitleFromAxis(layout);
    return makeDefaultDownloadFileName('chart', title ?? axisTitle, fmt);
}

function ensureExt(name, ext) {
    return name?.toLowerCase?.().endsWith(ext) ? name : `${name}${ext}`;
}

/**
 * Get a plain Plotly figure: {data, layout, frames, config}
 * @param Plotly
 * @param chartDiv
 */
function getFigure(Plotly, chartDiv) {
    return {
        data: isArray(chartDiv?.data) ? chartDiv.data : [],
        layout: chartDiv?.layout ?? {},
        frames: chartDiv?.frames ?? [],
        config: chartDiv?.config ?? {}
    };
}

function getLayoutAttrs(schema) {
    return (
        schema?.layoutAttributes ||
        schema?.layout?.attributes ||
        schema?.layout?.layoutAttributes ||
        {}
    );
}

function getAllTraceAttrs(schema) {
    return (
        schema?.traces?.allTraces?.attributes ||
        schema?.allTraces?.attributes ||
        {}
    );
}

function getTraceAttrs(schema, type) {
    return (
        schema?.traces?.[type]?.attributes ||
        schema?.traces?.[type]?.layoutAttributes ||
        {}
    );
}

//to keep only plotly properties (remove any firefly keys like tbl_id, etc.)
function sanitizeWithPlotlySchema(Plotly, fig) {
    const schema = Plotly?.PlotSchema?.get?.();
    if (!schema) return {data: [], layout: {}, frames: [], config: {}};

    const layoutAttrs = getLayoutAttrs(schema);
    const allAttrs = getAllTraceAttrs(schema);

    //whitelist layout keys
    const cleanLayout = {};
    for (const [k, v] of Object.entries(fig?.layout ?? {})) {
        if (k in layoutAttrs) cleanLayout[k] = v;
    }

    //whitelist data keys per trace type + shared attrs common to all trace types
    const cleanData = (isArray(fig?.data) ? fig.data : []).map((trace) => {
        const t = trace?.type || 'scatter';
        const traceAttrs = getTraceAttrs(schema, t);
        const out = {type: t};
        for (const [k, v] of Object.entries(trace || {})) {
            if (k === 'type') continue;
            if (isDefined( traceAttrs?.[k]) || isDefined( allAttrs?.[k])) out[k] = v;
        }
        return out;
    });

    return {data: cleanData, layout: cleanLayout, frames: fig?.frames ?? {}, config: fig?.config ?? {}};
}

async function saveFile(request, Plotly, chartDiv) {
    const format = (request.format || 'png').toLowerCase();

    if (format === 'json') {
        const fig = getFigure(Plotly, chartDiv);
        const clean = sanitizeWithPlotlySchema(Plotly, fig);
        const jsonText = JSON.stringify(clean, null, 2);
        const blob = new Blob([jsonText], { type: 'application/json;charset=utf-8' });
        downloadBlob(blob, ensureExt(request.filename, '.json'));
        return;
    }

    //default to PNG format
    const filename = ensureExt(request.filename, '.png');
    const dataurl = await Plotly.toImage(chartDiv, { format: 'png' });
    const blob = await (await window.fetch(dataurl)).blob();
    downloadBlob(blob, filename);
}

const PlotLySavePanel = function({Plotly, chartDiv, filename, format}) {
    const groupKey = 'PlotLySaveField';

    const [getFormat, setFormat] = useFieldGroupValue('format', groupKey);
    const [getName, setName] = useFieldGroupValue('filename', groupKey);

    const formatVal = getFormat?.();
    const nameVal = getName?.();

    useEffect(() => {
        if (!nameVal) return;
        //change extension in filename from .png to .json and vice-versa based on user selection
        if (formatVal === 'json' && /\.png$/i.test(nameVal)) {
            setName(nameVal.replace(/\.png$/i, '.json'));
        } else if (formatVal === 'png' && /\.json$/i.test(nameVal)) {
            setName(nameVal.replace(/\.json$/i, '.png'));
        }
    }, [formatVal, nameVal, setName]);

    return (
        <FieldGroup groupKey={groupKey} sx={{height: 1}}>
            <Stack p={1} justifyContent='space-between' spacing={2} height={1}>
                <ValidationField
                    fieldKey={'filename'}
                    initialState={{
                        value: filename,
                        tooltip: 'Enter filename',
                        label: 'Filename',
                    }} />
                <RadioGroupInputField
                    fieldKey='format'
                    groupKey={groupKey}
                    options={[
                        { label: 'PNG Image (.png)', value: 'png' },
                        { label: 'JSON (.json)', value: 'json' }
                    ]}
                    initialState={{
                        value: format ?? 'png', //default to png
                        orientation: 'horizontal',
                        label: 'Format',
                        tooltip: 'Choose output format'
                    }}
                />
                <Stack direction='row' justifyContent='space-between'>
                    <Stack spacing={1} direction='row' alignItems='center'>
                        <CompleteButton text='Save' dialogId={DIALOG_ID}
                            onSuccess={(request) => saveFile(request, Plotly, chartDiv)}
                        />
                        <Button onClick={() => dispatchHideDialog(DIALOG_ID)}>Cancel</Button>
                    </Stack>
                    <HelpIcon helpId={'chart.save'}/>
                </Stack>
            </Stack>
        </FieldGroup>
    );
};
