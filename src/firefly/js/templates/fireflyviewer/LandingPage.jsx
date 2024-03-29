import {Button, Divider, Sheet, Snackbar, Stack, Typography} from '@mui/joy';
import React, {useContext, useState} from 'react';
import {elementType, shape, object, string, arrayOf, element, oneOf} from 'prop-types';
import QueryStats from '@mui/icons-material/QueryStats';
import TipsAndUpdates from '@mui/icons-material/TipsAndUpdates';

import {getBackgroundInfo} from '../../core/background/BackgroundUtil.js';
import {dispatchShowDropDown} from '../../core/LayoutCntlr.js';
import {AppPropertiesCtx} from '../../ui/AppPropertiesCtx.jsx';
import {Slot, useStoreConnector} from '../../ui/SimpleComponent.jsx';
import {FileDropZone} from '../../visualize/ui/FileUploadViewPanel.jsx';
import {dispatchAddPreference, getPreference} from 'firefly/core/AppDataCntlr';


export function LandingPage({slotProps={}, sx, ...props}) {
    const {appTitle,footer,
        fileDropEventAction='FileUploadDropDownCmd'} = useContext(AppPropertiesCtx);

    const defSlotProps = {
        tabsMenuHint: {appTitle, id: 'tabsMenu', hintText: 'Choose a search from the tabs menu', sx: { left: '16rem' }},
        bgMonitorHint: {appTitle, id: 'bgMonitor', hintText: 'Load job results from background monitor', tipPlacement: 'end', sx: { right: 8 }},
        topSection: { appTitle },
        bottomSection: {
                icon: <QueryStats sx={{ width: '5rem', height: '5rem' }} />,
                text: 'No Search Results Yet',
                subtext: 'Submit a search to see results here',
                actionItems: [
                    { text: 'Choose a search', subtext: 'from the tabs above' },
                    { text: 'Browse all searches', subtext: 'from the side-menu' },
                    { text: 'Upload a file', subtext: 'drag & drop here' }
                ]
        }
    };

    const [dropEvent, setDropEvent] = useState(undefined);

    const {jobs = {}} = useStoreConnector(() => getBackgroundInfo());
    const items = Object.values(jobs)
        .filter((job) => job.jobInfo?.monitored && job.jobInfo?.type !== 'PACKAGE');
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
                <Stack justifyContent='space-between' width={1}>
                    <Stack spacing={1} width={1} px={4} py={3} {...slotProps?.contentSection}>
                        <Slot component={DefaultAppBranding} {...defSlotProps.topSection} slotProps={slotProps?.topSection}/>
                        <Slot component={EmptyResults} {...defSlotProps.bottomSection} slotProps={slotProps?.bottomSection}/>
                    </Stack>
                    {footer ? footer : undefined}
                </Stack>
            </FileDropZone>
        </Sheet>
    );
}


function DefaultAppBranding({appTitle, appDescription}) {
    return (
        <Stack spacing={.25}>
            <Typography level={'h4'}>{`Welcome to ${appTitle}`}</Typography>
            {appDescription && <Typography level={'body-md'}>{appDescription}</Typography>}
        </Stack>
    );
}


function EmptyResults({icon, text, subtext, actionItems}) {
    const renderActionItem = ({text, subtext}) => (
        <Stack spacing={.5} alignItems='center'>
            <Typography level={'title-lg'} color={'primary'}>{text}</Typography>
            <Typography level={'body-md'}>{subtext}</Typography>
        </Stack>
    );

    return (
        <Sheet variant='soft' sx={{py: 8}}>
            <Stack spacing={6} alignItems='center'>
                <Stack spacing={2} alignItems='center'>
                    {icon}
                    <Stack spacing={.5} alignItems='center'>
                        <Typography level={'h3'} fontWeight={'var(--joy-fontWeight-md)'}>{text}</Typography>
                        <Typography level={'body-lg'}>{subtext}</Typography>
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
        </Sheet>
    );
}


function AppHint({appTitle, id, hintText, tipPlacement='middle', sx={}}) {
    // An app hint needs to be shown only the first time user loads an app. So this is controlled by a flag saved as app preference
    const appHintPrefName = `showAppHint__${appTitle}--${id}`;
    const showAppHint = useStoreConnector(() => getPreference(appHintPrefName, true));

    const arrowTip = {
        '&::before': {
            content: '""',
            width: '1rem',
            height: '1rem',
            backgroundColor: 'inherit',
            transform: 'rotate(-45deg)',
            position: 'absolute',
            top: '-0.5rem',
            left: 'calc(50% - 0.5rem)',
            ...tipPlacement==='start' && {left: 'var(--Snackbar-padding)'},
            ...tipPlacement==='end' && {left: 'auto', right: 'var(--Snackbar-padding)'}
        }
    };

    // to undo positioning controlled by anchorOrigin prop of Snackbar, and to place it directly below Banner
    const positioningSx = {
        position: 'absolute',
        top: '0.75rem',
        left: 'auto',
        right: 'auto',
        bottom: 'auto'
    };

    return (
        <Snackbar open={Boolean(showAppHint)}
                  size='lg'
                  variant='solid' //to make it look different from alerts
                  color='primary'
                  invertedColors={true}
                  onClose={(e, reason)=> {
                      //don't close a hint if the click made outside it (clickaway) originated from another hint
                      if (reason==='clickaway' && e?.target?.closest('.MuiSnackbar-root')) return;
                      dispatchAddPreference(appHintPrefName, false);
                  }}
                  sx={{...positioningSx, ...sx, ...arrowTip}}
                  startDecorator={<TipsAndUpdates/>}
                  endDecorator={
                      <Button
                          onClick={() => dispatchAddPreference(appHintPrefName, false)}
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
    })
};


DefaultAppBranding.propTypes = {
    appTitle: string,
    appDescription: string,
};


EmptyResults.propTypes = {
    icon: element,
    text: string,
    subtext: string,
    actionItems: arrayOf(shape({
        text: string,
        subtext: string
    }))
};


AppHint.propTypes = {
    appTitle: string.isRequired,
    id: string.isRequired,
    hintText: string.isRequired,
    tipPlacement: oneOf(['start', 'middle', 'end']),
    sx: object,
};
