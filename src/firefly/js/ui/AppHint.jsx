import {useStoreConnector} from 'firefly/ui/SimpleComponent';
import {dispatchAddPreference, getPreference} from 'firefly/core/AppDataCntlr';
import React, {useLayoutEffect, useRef, useState} from 'react';
import {Button, Snackbar} from '@mui/joy';
import TipsAndUpdates from '@mui/icons-material/TipsAndUpdates';
import {object, oneOf, string} from 'prop-types';


export const APP_HINT_IDS = {
    TABS_MENU: 'tabsMenu',
    BG_MONITOR: 'bgMonitor'
};

export const HINT_TIP_PLACEMENTS = {
    START: 'start',
    MIDDLE: 'middle',
    END: 'end'
};

/**An app hint needs to be shown only the first time user loads an app. So this is controlled by a flag saved as app preference**/
export const appHintPrefName = (appTitle, hintId) => `showAppHint__${appTitle}--${hintId}`;

export function AppHint({appTitle, id, anchorNode, hintText, tipPlacement=HINT_TIP_PLACEMENTS.MIDDLE, sx={}}) {
    const showAppHint = useStoreConnector(() => getPreference(appHintPrefName(appTitle, id), true));
    const appHintRef = useRef();

    // AppHint is rendered inside LandingPage, so we cannot yet compute top/bottom from the anchor node but only left/right
    const [anchorRelativePosSx, setAnchorRelativePosSx] = useState({});
    useLayoutEffect(() => {
        if (anchorNode) {
            const anchorRect = anchorNode.getBoundingClientRect();
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
        }
    }, [anchorNode]);

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

AppHint.propTypes = {
    appTitle: string.isRequired,
    id: string.isRequired,
    anchorNode: object, // generally a Menu Tab DOM node
    hintText: string.isRequired,
    tipPlacement: oneOf(['start', 'middle', 'end']),
    sx: object,
};

