import {Box, Link, Stack, Typography} from '@mui/joy';
import {array, arrayOf, bool, func, node, object, oneOfType, string} from 'prop-types';
import React, {useContext, useEffect, useState} from 'react';
import {CONE_CHOICE_KEY} from '../../visualize/ui/CommonUIKeys.js';
import {CheckboxGroupInputField} from '../CheckboxGroupInputField.jsx';
import {FieldGroupCtx} from '../FieldGroup';
import {FieldGroupTabs, Tab} from '../panel/TabPanel';
import {showInfoPopup} from '../PopupUtil.jsx';
import {useFieldGroupValue} from '../SimpleComponent.jsx';
import {getServiceMetaOptions, loadSiaV2Meta, makeObsCoreMetadataModel} from '../tap/SiaUtil';
import {hasSpatialTypes, isSpatialTypeSupported} from './DLGenAnalyzeSearch.js';
import {DLSearchTitle} from './DLuiDecoration';
import {CONE_AREA_KEY} from './DynamicDef.js';
import {DynLayoutPanelTypes} from './DynamicUISearchPanel';
import {ConstraintContext} from '../tap/Constraints';
import {isSIAStandardID} from './ServiceDefTools';


const HIPS_PLOT_ID= 'dlGeneratedHipsPlotId';
const DocRows= ({docRows=[], showLabel=true}) => (
    <Box key='help' sx={{px:1, pb:1/2, alignSelf: 'flex-end'}}>
        {
            docRows.map((row, idx) => (
                <Link level='body-sm'
                      href={row.accessUrl} key={idx + ''} target='documentation'>
                    {showLabel && `Documentation: ${row.desc}`}
                </Link>)
            )
        }
    </Box>
);



export function DLuiServDescPanel({fds, sx, desc, setSideBarShowing, sideBarShowing, docRows, setClickFunc,
                                  submitSearch, isAllSky, qAna={},dataServiceId, slotProps})  {
    const [obsCoreMetadataModel, setObsCoreMetadataModel]= useState(undefined);
    const {getVal,setVal} = useContext(FieldGroupCtx);
    const {concurrentSearchDef}= qAna;
    const {standardID='', accessURL}= qAna?.primarySearchDef?.[0]?.serviceDef ?? {};

    useEffect( () => {
        if (accessURL && isSIAStandardID(standardID)) {
            loadSiaV2Meta(accessURL).then( (siaMeta) =>{
                const fallbackMetaOptions= getServiceMetaOptions(dataServiceId) ?? [];
                setObsCoreMetadataModel(makeObsCoreMetadataModel(siaMeta, fallbackMetaOptions));
            });
        }
    },[standardID,accessURL]);

    const coneAreaChoice = useFieldGroupValue(CONE_AREA_KEY)?.[0]() ?? CONE_CHOICE_KEY;
    let cNames, disableNames;
    if (concurrentSearchDef?.length && coneAreaChoice) {
        cNames = concurrentSearchDef
            .filter((c) => hasSpatialTypes(c.serviceDef) && isSpatialTypeSupported(c.serviceDef, coneAreaChoice))
            .map((c) => c.desc);
        disableNames = concurrentSearchDef
            .filter((c) => hasSpatialTypes(c.serviceDef) && !isSpatialTypeSupported(c.serviceDef, coneAreaChoice))
            .map((c) => c.desc);
    }

    const disDesc = coneAreaChoice === CONE_AREA_KEY ? 'w/ cone' : 'w/ polygon';

    const constraintCtx= {
        setConstraintFragment: (key,value) => {
            const fragments= getVal('SIA_CTX') ?? new Map();
            value ? fragments.set(key,value) : fragments.delete(key);
            setVal('SIA_CTX',fragments);
        }
    };

    const options = (cNames || disableNames) ? (
        <Stack {...{alignItems: 'flex-start',}}>
            {Boolean(cNames?.length) &&
                <CheckboxGroupInputField
                    fieldKey='searchOptions'
                    alignment='horizontal' labelStyle={{fontSize: 'larger'}}
                    label={`Include Search${cNames.length > 1 ? 'es' : ''} of: `}
                    options={cNames.map((c) => ({label: c, value: c}))}
                    initialState={{value: cNames.join(' '), tooltip: 'Additional Searches', label: ''}}
                />}
            {Boolean(disableNames?.length) && (
                <Stack {...{direction: 'row'}}>
                    <Typography color='warning'>
                        <span>{`Warning - search${disableNames.length > 1 ? 'es' : ''} disabled ${disDesc}:`}</span>
                    </Typography>
                    {disableNames.map((d) => (<Typography level='body-sm'>{d}</Typography>))}
                </Stack>)
            }
        </Stack>

    ) : undefined;

    return  (
        <Stack {...{justifyContent:'space-between', sx}}>
            <DLSearchTitle {...{desc,isAllSky,sideBarShowing,setSideBarShowing,...slotProps.searchTitle}}/>
            <ConstraintContext.Provider value={constraintCtx}>
                <DynLayoutPanelTypes.Inset {...{fieldDefAry:fds, plotId:HIPS_PLOT_ID, obsCoreMetadataModel,
                    style:{height:'100%', marginTop:4},
                    dataServiceId,
                    toolbarHelpId:'dlGenerated.VisualSelection',
                    slotProps:{
                        FormPanel: {
                            onSuccess: (r) => submitSearch(r,getVal('SIA_CTX') ?? new Map()),
                            onError: () => showInfoPopup('Fix errors and search again', 'Error'),
                            help_id:'search-collections-general',
                            slotProps:{
                                input: {border:0, p:0, mb:1},
                                searchBar: {actions: <DocRows key='root' docRows={docRows}/>},
                                completeBtn: {
                                    getDoOnClickFunc:(f) => setClickFunc({onClick: f})
                                }
                            }
                        }} }}>
                    <Stack {...{alignItems: 'flex-start',}}>
                        {options}
                    </Stack>
                </DynLayoutPanelTypes.Inset>
            </ConstraintContext.Provider>
        </Stack>
    );
}


DLuiServDescPanel.propTypes= {
    isAllSky: bool,
    slotProps: object,
    qAna: object,
    children: oneOfType([node,arrayOf(node)]),
    submitSearch: func,
    setClickFunc: func,
    docRows: array,
    fds: array,
    sx: object,
    desc: string,
    dataServiceId: string,
    setSideBarShowing: func,
    sideBarShowing: bool,
};


export const DLuiTabView = ({
                                tabsKey,
                                setSideBarShowing,
                                sideBarShowing,
                                searchObjFds,
                                qAna,
                                docRows,
                                isAllSky,
                                setClickFunc,
                                submitSearch,
                                slotProps,
                                dataServiceId
                            }) => (
    <FieldGroupTabs style={{height: '100%', width: '100%'}} initialState={{value: searchObjFds[0]?.ID}}
                    fieldKey={tabsKey}>
        {
            searchObjFds.map((sFds) => {
                const {fds, idx, ID, desc} = sFds;
                return (
                    <Tab name={`${qAna.primarySearchDef[idx].desc}`} id={ID} key={idx + ''}>
                        <DLuiServDescPanel{...{
                            fds, setSideBarShowing, sideBarShowing, sx: {width: 1}, desc, docRows,
                            isAllSky, setClickFunc, submitSearch, qAna, slotProps, dataServiceId,
                        }}/>
                    </Tab>
                );
            })
        }
    </FieldGroupTabs>
);

DLuiServDescPanel.propTypes= {
    isAllSky: bool,
    slotProps: object,
    qAna: object,
    submitSearch: func,
    setClickFunc: func,
    docRows: array,
    fds: array,
    sx: object,
    setSideBarShowing: func,
    sideBarShowing: bool,
};
