

import {pick} from 'lodash';
import {func} from 'prop-types';
import React, {useState} from 'react';
import {Stack, Typography} from '@mui/joy';
import {
    dispatchSetLayoutMode, dispatchUpdateLayoutInfo, getLayouInfo, getResultCounts, LO_MODE
} from '../../core/LayoutCntlr.js';
import {ListBoxInputFieldView} from '../../ui/ListBoxInputField.jsx';
import {AccordionPanelView} from '../../ui/panel/AccordionPanel.jsx';
import {useStoreConnector} from '../../ui/SimpleComponent.jsx';


const triViewKey= 'images | tables | xyplots';
const tblImgKey= 'tables | images';
const imgXyKey= 'images | xyplots';
const tblXyKey= 'tables | xyplots';
const stateKeys= ['showTables', 'showImages', 'showXyPlots', 'images', 'coverageSide'];
const LEFT= 'LEFT';
const RIGHT= 'RIGHT';

function getCovSideOptions(currLayoutMode, showImages) {
    const make= (l,r) => [ {label:l, value:LEFT}, {label:r, value:RIGHT}];

    switch (currLayoutMode) {
        case triViewKey: return make('Left', showImages?'Right':'Coverage w/Charts');
        case tblImgKey: return make('Coverage showing', 'Coverage hidden');
        case imgXyKey: return make('Left', 'Right');
        case tblXyKey: return make('Coverage hidden', 'Coverage showing');
        default: return make('Coverage left', 'Coverage right');
    }
}



export function LayoutChoiceAccordion({closeSideBar}) {
    const haveResults= useStoreConnector(() => getResultCounts().haveResults);
    const [panelOpen, setPanelOpen] = useState(true);

    if (!haveResults) return <div/>;

    return (
        <AccordionPanelView
            header={<Typography level='h4'>Results Layout</Typography>} expanded={panelOpen}
            onChange = {(v) => setPanelOpen(v)}>
            <LayoutChoice closeSideBar={closeSideBar} haveResults={haveResults}/>
        </AccordionPanelView>
    );
}

LayoutChoiceAccordion.propTypes= {
    closeSideBar: func,
};

export function LayoutChoice({closeSideBar, haveResults}) {
    const state= useStoreConnector(() => pick(getLayouInfo(), stateKeys));
    if (!haveResults) return <div/>;

    const {showTables, showImages, showXyPlots, images={}, coverageSide=LEFT} = state;
    const {showCoverage} = images;
    const coverageRight= showCoverage && coverageSide===RIGHT;
    const coverageLeft= showCoverage && coverageSide===LEFT;
    const currLayoutMode= getLayouInfo()?.mode?.standard?.toString() ?? triViewKey;
    const isTriViewPossible = (showImages||coverageLeft) && (showXyPlots||coverageRight) && showTables;

    const covSideOptions= getCovSideOptions(currLayoutMode, showImages);

    const options= [
        {label:'Tri-view', value:triViewKey},
        {label:'Bi-view Images', value:imgXyKey},
        {label:'Bi-view Tables', value:tblXyKey},
    ];

    return (
        <Stack spacing={1} pt={1}>
            {showCoverage && currLayoutMode!==tblXyKey &&
                <ListBoxInputFieldView {...{
                    options:covSideOptions, value:coverageSide,
                    label:'Coverage',
                    slotProps:{label: {sx: {width: '7rem'}}},
                    onChange:(ev,newVal) => {
                        dispatchUpdateLayoutInfo({coverageSide:newVal});
                        closeSideBar?.();
                    },
                }} /> }
            {isTriViewPossible &&
               <ListBoxInputFieldView
                    {...{
                        options, value:currLayoutMode,
                        label:'Layout',
                        slotProps:{label: {sx: {width: '7rem'}}},
                        onChange:(ev,newVal) => {
                            dispatchSetLayoutMode(LO_MODE.standard, newVal);
                            closeSideBar?.();
                        },
                    }} />
            }
        </Stack>
    );
}