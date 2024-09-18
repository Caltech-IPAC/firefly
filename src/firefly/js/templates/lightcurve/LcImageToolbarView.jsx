/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import {Stack, Switch, Typography} from '@mui/joy';
import React from 'react';
import PropTypes from 'prop-types';
import {WcsMatchType, visRoot, dispatchWcsMatch} from '../../visualize/ImagePlotCntlr.js';
import {RadioGroupInputFieldView} from '../../ui/RadioGroupInputFieldView.jsx';
import {
    dispatchChangeViewerLayout, getMultiViewRoot, GRID, SINGLE, getLayoutDetails
} from '../../visualize/MultiViewCntlr.js';
import {LC, getConverterData} from './LcManager.js';
import {CloseButton} from '../../ui/CloseButton.jsx';
import {getTblById} from '../../tables/TableUtil.js';
import {SortInfo, SORT_ASC, SORT_DESC, UNSORTED} from '../../tables/SortInfo.js';
import {VisMiniToolbar} from 'firefly/visualize/ui/VisMiniToolbar.jsx';



export function LcImageToolbarView({viewerId, tableId, closeFunc=null}) {
    const converter = getConverterData();
    if (!converter) { return null; }

    const count= getLayoutDetails(getMultiViewRoot(), viewerId)?.count ?? converter.defaultImageCount;

    const options= [];
    for(let i= 1; (i<=LC.MAX_IMAGE_CNT); i+=2) {
        options.push({label: String(i), value: String(i)});
    }

    return (
        <div>
            <Stack {...{
                direction:'row', flexWrap:'nowrap', alignItems: 'center', justifyContent:'space-between',
                height: 30, ml:closeFunc ? 1/2 : 0, mb: closeFunc ? 1/2 : 0}}>
                {closeFunc && <CloseButton onClick={closeFunc}/>}
                <Stack {...{whiteSpace: 'nowrap', pl: 1, spacing:1, direction:'row', alignItems:'center'}}>
                    <Typography level='body-sm'>Image Count:</Typography>
                    <RadioGroupInputFieldView options={options} value={String(count)}
                                              orientation='horizontal'
                                              onChange={(ev) => changeSize(viewerId, ev.target.value)} />
                </Stack>
                <Typography level='body-sm'>{ getSortInfo(tableId) } </Typography>
                <Switch size='sm' sx={{alignSelf:'center', pr:1, pl:3}}
                        checked={visRoot().wcsMatchType===WcsMatchType.Target}
                        onChange={(ev) => wcsMatchTarget(ev.target.checked, visRoot().activePlotId)}
                        endDecorator={'Target Match'} />
                <VisMiniToolbar sx={{width:350}}/>
            </Stack>
        </div>
    );
}

LcImageToolbarView.propTypes= {
    viewerId : PropTypes.string.isRequired,
    tableId: PropTypes.string,
    closeFunc : PropTypes.func
};

function getSortInfo(tableId) {
    if (!tableId) return '';
    const sInfo = SortInfo.parse(getTblById(tableId)?.request?.sortInfo ??'');
    if (sInfo.direction === UNSORTED) return '';
    const orderInfo = {[SORT_ASC]: 'ascending', [SORT_DESC]:'descending'};
    return `Sorted by column: ${sInfo.sortColumns.join(',')} `+ ` ${orderInfo[sInfo.direction]}`;
};

function changeSize(viewerId, value) {
    const valNum = Number(value);
    dispatchChangeViewerLayout(viewerId, valNum === 1 ? SINGLE : GRID, {count: valNum});
}


const wcsMatchTarget= (doWcsStandard, plotId) =>
    dispatchWcsMatch({matchType:doWcsStandard?WcsMatchType.Target:false, plotId});
