import {Box, Divider, Sheet, Stack, Typography} from '@mui/joy';
import React, {useContext, useState} from 'react';
import {elementType, shape, object, string, arrayOf, node} from 'prop-types';
import QueryStats from '@mui/icons-material/QueryStats';

import {getBackgroundInfo, isMonitored, isSearchJob} from '../../core/background/BackgroundUtil.js';
import {dispatchShowDropDown, getHintAnchorNodes} from '../../core/LayoutCntlr.js';
import {AppPropertiesCtx} from '../../ui/AppPropertiesCtx.jsx';
import {Slot, useStoreConnector} from '../../ui/SimpleComponent.jsx';
import {FileDropZone} from '../../visualize/ui/FileUploadViewPanel.jsx';
import {APP_HINT_IDS, AppHint} from 'firefly/ui/AppHint';


/**
 * Full-page landing screen shown before the user has any results to display.
 * Wraps content in a {@link FileDropZone} so drag-and-drop file upload works everywhere.
 * AppHint overlays (tabs menu, background monitor) are mounted automatically when relevant.
 *
 * All visual regions are customizable via `slotProps`. Each slot accepts an optional `component`
 * key to replace the default renderer; all remaining keys are forwarded as props to that component.
 *
 * Slot layout:
 * ```
 *  tabsMenuHint  → AppHint  (anchored to first tab after Results tab)
 *  bgMonitorHint → AppHint  (anchored to last tab: Job Monitor)
 *  ┌─ bgContainer (Box) ──────────────────────────────────────┐
 *  │  ┌─ contentSection (Stack) ───────────────────────────┐  │
 *  │  │  topSection    → DefaultAppBranding  (title/desc)  │  │
 *  │  │  bottomSection → EmptyResults        (actions)     │  │
 *  │  └────────────────────────────────────────────────────┘  │
 *  └──────────────────────────────────────────────────────────┘
 * ```
 *
 * @param {object} props
 * @param {object} [props.slotProps={}]        - Per-slot overrides (see layout above).
 * @param {object} [props.sx]                  - sx forwarded to the root Sheet.
 */
export function LandingPage({slotProps={}, sx, ...props}) {
    const {appTitle,footer,
        fileDropEventAction='FileUploadDropDownCmd'} = useContext(AppPropertiesCtx);

    const {first: tabsMenuHintAnchor, last: bgMonitorHintAnchor} = useStoreConnector(getHintAnchorNodes);

    const defSlotProps = {
        tabsMenuHint: {appTitle, id: APP_HINT_IDS.TABS_MENU, anchorEl: tabsMenuHintAnchor, hintText: 'Choose a tab to search for or upload data.'},
        bgMonitorHint: {appTitle, id: APP_HINT_IDS.BG_MONITOR, anchorEl: bgMonitorHintAnchor, hintText: 'Load job results from background monitor', placement: 'bottom-start'},
        topSection: { title: `Welcome to ${appTitle}` },
        bottomSection: {
                icon: <QueryStats sx={{ width: '6rem', height: '6rem' }} />,
                text: 'Getting Started',
                subtext: undefined,
                summaryText: 'Visualizations of the results will appear in this tab',
                actionItems: [
                    { text: 'Search for data', subtext: 'using the tabs above' },
                    { text: 'Find more search options', subtext: 'in the side menu' },
                    { text: 'Upload a file', subtext: 'drag & drop here' }
                ]
        }
    };

    const [dropEvent, setDropEvent] = useState(undefined);

    const {jobs = {}} = useStoreConnector(() => getBackgroundInfo());
    const items = Object.values(jobs)
        .filter((job) => isMonitored(job) && isSearchJob(job));
    const haveBgJobs = items.length > 0;

    return (
        <Sheet className='ff-ResultsPanel-StandardView' sx={{width: 1, height: 1, ...sx}} {...props}>
            {tabsMenuHintAnchor && <Slot component={AppHint} {...defSlotProps.tabsMenuHint} slotProps={slotProps?.tabsMenuHint}/>}
            {haveBgJobs && <Slot component={AppHint} {...defSlotProps.bgMonitorHint} slotProps={slotProps?.bgMonitorHint}/>}
            <FileDropZone {...{
                dropEvent, setDropEvent,
                setLoadingOp: () => {
                    const newEv = {type: 'drop', dataTransfer: {files: Array.from(dropEvent.dataTransfer.files)}};
                    dispatchShowDropDown({view:fileDropEventAction, initArgs: {searchParams: {dropEvent: newEv}}});
                },
            }}>
                <Stack justifyContent='space-between' width={1} spacing={1}>
                    <Box {...slotProps?.bgContainer}>
                        <Stack spacing={2} width={1} px={4} py={3} {...slotProps?.contentSection}>
                            <Slot component={DefaultAppBranding} {...defSlotProps.topSection} slotProps={slotProps?.topSection}/>
                            <Slot component={EmptyResults} {...defSlotProps.bottomSection} slotProps={slotProps?.bottomSection}/>
                        </Stack>
                    </Box>
                    {footer ? footer : undefined}
                </Stack>
            </FileDropZone>
        </Sheet>
    );
}


function DefaultAppBranding({title, desc}) {
    return (
        <Stack spacing={.25} alignItems='center'>
            <Typography fontSize='xl3' color='neutral'>{title}</Typography>
            {desc && <Typography level='body-md' textAlign='center'>{desc}</Typography>}
        </Stack>
    );
}


function EmptyResults({icon, text, subtext, summaryText, actionItems, slotProps}) {
    const renderActionItem = ({text, subtext}) => (
        <Stack spacing={.5} alignItems='center'>
            <Typography level={'title-lg'} color={'primary'}>{text}</Typography>
            <Typography level={'body-md'}>{subtext}</Typography>
        </Stack>
    );

    return (
        <Sheet variant='soft' sx={{pt: 8, pb: 4, px: 2}} {...slotProps?.root}>
            <Stack spacing={10} alignItems='center'>
                <Stack spacing={Boolean(subtext) ? 6 : 3}>
                    <Stack spacing={2} alignItems='center'>
                        {icon}
                        <Stack spacing={.5} alignItems='center'>
                            <Typography level='h2' fontWeight='md'>{text}</Typography>
                            {Boolean(subtext) && <Typography level={'body-lg'}>{subtext}</Typography>}
                        </Stack>
                    </Stack>
                    <Stack direction='row' spacing={6}>
                        {actionItems.map((actionItem, idx) => (
                            <React.Fragment key={idx}>
                                {idx > 0 && <Divider orientation='vertical' />}
                                {renderActionItem(actionItem)}
                            </React.Fragment>
                        ))}
                    </Stack>
                </Stack>
                {Boolean(summaryText) &&
                    <Stack spacing={2} alignItems='center'>
                        <Typography component='div' level={'body-lg'}>{summaryText}</Typography>
                    </Stack>}
            </Stack>
        </Sheet>
    );
}



LandingPage.propTypes = {
    sx: object,
    slotProps: shape({
        tabsMenuHint: shape({
            component: elementType,         // defaults to AppHint.  null to skip
            ...AppHint.propTypes,           // defaults to AppHint.propTypes
        }),
        bgMonitorHint: shape({
            component: elementType,         // defaults to AppHint.  null to skip
            ...AppHint.propTypes,           // defaults to AppHint.propTypes
        }),
        topSection: shape({
            component: elementType,         // defaults to DefaultAppBranding.  null to skip
            ...DefaultAppBranding.propTypes,// defaults to DefaultAppBranding.propTypes
        }),
        bottomSection: shape({
            component: elementType,         // defaults to EmptyResults.  null to skip
            ...EmptyResults.propTypes,      // defaults to EmptyResults.propTypes
        }),
        contentSection: object,
        bgContainer: object,
    })
};


DefaultAppBranding.propTypes = {
    title: string,
    desc: string,
};


EmptyResults.propTypes = {
    icon: node,
    text: string,
    subtext: string,
    actionItems: arrayOf(shape({
        text: string,
        subtext: string
    })),
    slotProps: object,
};
