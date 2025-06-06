import {Box, Button, Divider, Sheet, Snackbar, Stack, Typography} from '@mui/joy';
import React, {useContext, useLayoutEffect, useRef, useState} from 'react';
import {elementType, shape, object, string, arrayOf, oneOf, node} from 'prop-types';
import QueryStats from '@mui/icons-material/QueryStats';
import TipsAndUpdates from '@mui/icons-material/TipsAndUpdates';

import {getBackgroundInfo, isMonitored, isSearchJob} from '../../core/background/BackgroundUtil.js';
import {dispatchShowDropDown} from '../../core/LayoutCntlr.js';
import {AppPropertiesCtx} from '../../ui/AppPropertiesCtx.jsx';
import {Slot, useStoreConnector} from '../../ui/SimpleComponent.jsx';
import {FileDropZone} from '../../visualize/ui/FileUploadViewPanel.jsx';
import {dispatchAddPreference, getPreference} from 'firefly/core/AppDataCntlr';

export const APP_HINT_IDS = {
    TABS_MENU: 'tabsMenu',
    BG_MONITOR: 'bgMonitor'
};

export const HINT_TIP_PLACEMENTS = {
    START: 'start',
    MIDDLE: 'middle',
    END: 'end'
};


export function LandingPage({slotProps={}, sx, ...props}) {
    const {appTitle,footer,
        fileDropEventAction='FileUploadDropDownCmd'} = useContext(AppPropertiesCtx);

    const defSlotProps = {
        tabsMenuHint: {appTitle, id: APP_HINT_IDS.TABS_MENU, hintText: 'Choose a tab to search for or upload data.'},
        bgMonitorHint: {appTitle, id: APP_HINT_IDS.BG_MONITOR, hintText: 'Load job results from background monitor', tipPlacement: HINT_TIP_PLACEMENTS.START},
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
            <Slot component={AppHint} {...defSlotProps.tabsMenuHint} slotProps={slotProps?.tabsMenuHint}/>
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

// An app hint needs to be shown only the first time user loads an app. So this is controlled by a flag saved as app preference
export const appHintPrefName = (appTitle, hintId) => `showAppHint__${appTitle}--${hintId}`;

/**Classname to identify the anchor (generally a Menu Tab) relative to which AppHint is positioned.**/
export const appHintAnchorClassName = (hintId) => `ff-AppHintAnchor-${hintId}`;

function AppHint({appTitle, id, hintText, tipPlacement=HINT_TIP_PLACEMENTS.MIDDLE, sx={}}) {
    const showAppHint = useStoreConnector(() => getPreference(appHintPrefName(appTitle, id), true));
    const appHintRef = useRef();

    // AppHint is rendered inside LandingPage, so we cannot yet compute top/bottom from the anchor element but only left/right
    const [anchorRelativePosSx, setAnchorRelativePosSx] = useState({});
    useLayoutEffect(() => {
        const timeout = setTimeout(() => {
            const anchorEl = document.querySelector(`.${appHintAnchorClassName(id)}`);
            if (anchorEl) {
                const anchorRect = anchorEl.getBoundingClientRect();
                const appHintRect = appHintRef.current?.getBoundingClientRect() ?? {width: 0};
                const posSx = {left: 'auto', right: 'auto'};
                switch (tipPlacement) {
                    case HINT_TIP_PLACEMENTS.START:
                        posSx.left = anchorRect.left;
                        break;
                    case HINT_TIP_PLACEMENTS.END:
                        posSx.right = `calc(100vw - ${anchorRect.right}px)`;
                        break;
                    case HINT_TIP_PLACEMENTS.MIDDLE:
                    default:
                        posSx.left = anchorRect.left + (anchorRect.width/2) - (appHintRect.width/2); // to center the hint
                        break;
                }
                setAnchorRelativePosSx(posSx);
                //TODO: change timeout to refs or mounted state flag if possible
        }}, id===APP_HINT_IDS.TABS_MENU ? 10 : 0);
        return () => clearTimeout(timeout);
    }, [id]);

    const arrowTip = {
        '&::before': {
            content: '""',
            width: '1rem',
            height: '1rem',
            backgroundColor: 'inherit',
            transform: 'rotate(-45deg)',
            position: 'absolute',
            top: '-0.5rem',
            left: 'calc(50% - 0.5rem)', //tipPlacement===HINT_TIP_PLACEMENTS.MIDDLE
            ...tipPlacement===HINT_TIP_PLACEMENTS.START && {left: 'var(--Snackbar-padding)'},
            ...tipPlacement===HINT_TIP_PLACEMENTS.END && {left: 'auto', right: 'var(--Snackbar-padding)'}
        }
    };

    // to undo positioning controlled by anchorOrigin prop of Snackbar, and to place it directly below MenuTabBar
    const defaultPositionSx = {
        position: 'absolute',
        top: '0.75rem', // to create space for the arrow tip (with height: sqrt(2) * 1 rem / 2)
        bottom: 'auto',
        left: 'auto',
        right: 'auto',
    };

    return (
        <Snackbar open={Boolean(showAppHint)}
                  ref={appHintRef}
                  size='lg'
                  variant='solid' //to make it look different from alerts
                  color='primary'
                  invertedColors={true}
                  onClose={(e, reason)=> {
                      //don't close a hint if the click made outside it (clickaway) originated from another hint
                      if (reason==='clickaway' && e?.target?.closest('.MuiSnackbar-root')) return;
                      dispatchAddPreference(appHintPrefName(appTitle, id), false);
                  }}
                  sx={{...defaultPositionSx, ...anchorRelativePosSx, ...sx, ...arrowTip}}
                  startDecorator={<TipsAndUpdates/>}
                  endDecorator={
                      <Button
                          onClick={() => dispatchAddPreference(appHintPrefName(appTitle, id), false)}
                          variant='outlined'
                          color='primary'>
                          Got it
                      </Button>
                  }>
            {hintText}
        </Snackbar>
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


AppHint.propTypes = {
    appTitle: string.isRequired,
    id: string.isRequired,
    hintText: string.isRequired,
    tipPlacement: oneOf(['start', 'middle', 'end']),
    sx: object,
};
