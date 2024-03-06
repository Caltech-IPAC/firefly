import {Button, Divider, Sheet, Snackbar, Stack, Typography} from '@mui/joy';
import React, {useContext, useState} from 'react';
import QueryStats from '@mui/icons-material/QueryStats';
import TipsAndUpdates from '@mui/icons-material/TipsAndUpdates';
import PropTypes from 'prop-types';

import {getBackgroundInfo} from '../../core/background/BackgroundUtil.js';
import {dispatchShowDropDown} from '../../core/LayoutCntlr.js';
import {AppPropertiesCtx} from '../../ui/AppPropertiesCtx.jsx';
import {useStoreConnector} from '../../ui/SimpleComponent.jsx';
import {FileDropZone} from '../../visualize/ui/FileUploadViewPanel.jsx';
import {dispatchAddPreference, getPreference} from 'firefly/core/AppDataCntlr';


export function LandingPage({slots={}, slotProps={}}) {
    const {appTitle,footer,
        fileDropEventAction='FileUploadDropDownCmd'} = useContext(AppPropertiesCtx);

    const defaultSlots = {
        tabsMenuHint: {
            component: AppHint,
            props: { appTitle, id: 'tabsMenu', hintText: 'Choose a search from the tabs menu', sx: { left: '16rem' } }
        },
        bgMonitorHint: {
            component: AppHint,
            props: { appTitle, id: 'bgMonitor', hintText: 'Load job results from background monitor',
                tipPlacement: 'end', sx: { right: 8 } }
        },
        topSection: {
            component: DefaultAppBranding,
            props: { appTitle }
        },
        bottomSection: {
            component: EmptyResults,
            props: {
                icon: <QueryStats sx={{ width: '5rem', height: '5rem' }} />,
                text: 'No Search Results Yet',
                subtext: 'Submit a search to see results here',
                actionItems: [
                    { text: 'Choose a search', subtext: 'from the tabs above' },
                    { text: 'Browse all searches', subtext: 'from the side-menu' },
                    { text: 'Upload a file', subtext: 'drag & drop here' }
                ]
            }
        }
    };

    const renderSlot = (slotName) => {
        const Component = slots?.[slotName] ? slots[slotName] : defaultSlots[slotName].component;
        const props = slots?.[slotName]
            ? {...slotProps?.[slotName]} //if slot is changed, there's no default prop that needs to be present
            : {...defaultSlots[slotName].props, ...slotProps?.[slotName],
                sx: {...defaultSlots[slotName].props?.sx, ...slotProps?.[slotName]?.sx} //to allow deep merging for the sx prop
        };
        return (<Component {...props}/>);
    };

    const [dropEvent, setDropEvent] = useState(undefined);

    const {jobs = {}} = useStoreConnector(() => getBackgroundInfo());
    const items = Object.values(jobs)
        .filter((job) => job.jobInfo?.monitored && job.jobInfo?.type !== 'PACKAGE');
    const haveBgJobs = items.length > 0;

    return (
        <Sheet className='ff-ResultsPanel-StandardView' sx={{width: 1, height: 1}}>
            {renderSlot('tabsMenuHint')}
            {haveBgJobs && renderSlot('bgMonitorHint')}
            <FileDropZone {...{
                dropEvent, setDropEvent,
                setLoadingOp: () => {
                    const newEv = {type: 'drop', dataTransfer: {files: Array.from(dropEvent.dataTransfer.files)}};
                    dispatchShowDropDown({view:fileDropEventAction, initArgs: {searchParams: {dropEvent: newEv}}});
                },
            }}>
                <Stack justifyContent='space-between' width={1}>
                    <Stack spacing={1} width={1} px={4} py={3}>
                        {renderSlot('topSection')}
                        {renderSlot('bottomSection')}
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
    slots: PropTypes.shape({
        tabsMenuHint: PropTypes.elementType, //defaults to AppHint
        bgMonitorHint: PropTypes.elementType, //defaults to AppHint
        topSection: PropTypes.elementType, //defaults to DefaultAppBranding
        bottomSection: PropTypes.elementType //defaults to EmptyResults
    }),
    slotProps: PropTypes.shape({
        tabsMenuHint: PropTypes.object, //defaults to AppHint.propTypes
        bgMonitorHint: PropTypes.object, //defaults to AppHint.propTypes
        topSection: PropTypes.object, //defaults to DefaultAppBranding.propTypes
        bottomSection: PropTypes.object //defaults to EmptyResults.propTypes
    })
};


DefaultAppBranding.propTypes = {
    appTitle: PropTypes.string,
    appDescription: PropTypes.string,
};


EmptyResults.propTypes = {
    icon: PropTypes.element,
    text: PropTypes.string,
    subtext: PropTypes.string,
    actionItems: PropTypes.arrayOf(PropTypes.shape({
        text: PropTypes.string,
        subtext: PropTypes.string
    }))
};


AppHint.propTypes = {
    appTitle: PropTypes.string.isRequired,
    id: PropTypes.string.isRequired,
    hintText: PropTypes.string.isRequired,
    tipPlacement: PropTypes.oneOf(['start', 'middle', 'end']),
    sx: PropTypes.object,
};
