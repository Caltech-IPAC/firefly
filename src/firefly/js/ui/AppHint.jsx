import {useStoreConnector} from 'firefly/ui/SimpleComponent';
import {dispatchAddPreference, getPreference} from 'firefly/core/AppDataCntlr';
import React from 'react';
import {Button, Snackbar} from '@mui/joy';
import {Popper} from '@mui/base/Popper'; // transitive dependency of @mui/joy; used internally by Tooltip
import TipsAndUpdates from '@mui/icons-material/TipsAndUpdates';
import {bool, func, object, string} from 'prop-types';


/**
 * A controlled hint anchored to a DOM element via a Popper portal. Caller manages visibility
 * via `open`/`onClose` — suitable for product tours and programmatic guidance flows.
 * Dismissal does not persist any preference; see {@link AppHint} for that variant.
 *
 * @param {object} props
 * @param {boolean}  props.open                       - Whether the hint is visible.
 * @param {Function} [props.onClose]                  - Called when dismissed (clickaway or "Got it").
 * @param {boolean}  [props.dismissOnClickaway=true]  - Whether clicking outside the hint dismisses it.
 *                                                       Set to false for strict product-tour steps where
 *                                                       only "Got it" or explicit open=false closes it.
 * @param {Element}  props.anchorEl                   - DOM node to anchor to. Pass a raw DOM node (not a React ref).
 *                                                       Caller is responsible for not rendering until the node exists.
 * @param {string}   props.hintText                   - The hint message shown to the user.
 * @param {string}   [props.placement='bottom']       - Popper placement string (e.g. 'bottom', 'bottom-start', 'bottom-end').
 *                                                       Arrow direction is derived from this value.
 * @param {object}   [props.sx={}]                    - Additional sx styles merged onto the Snackbar.
 */
export function GuidedHint({open, onClose, dismissOnClickaway=true, anchorEl, hintText, placement='bottom', sx={}}) {
    const handleClose = (e, reason) => {
        if (reason === 'clickaway') {
            if (!dismissOnClickaway) return;
            if (e?.target?.closest('.MuiSnackbar-root')) return; // don't dismiss if click came from another hint
        }
        onClose?.(e, reason);
    };

    const arrowSx = (placement) => {
        const isAbove = placement.startsWith('top');
        return {
            '&::before': {
                content: '""',
                width: '1rem',
                height: '1rem',
                backgroundColor: 'inherit',
                transform: 'rotate(-45deg)',
                position: 'absolute',
                ...(isAbove ? {bottom: '-0.5rem', top: 'auto'} : {top: '-0.5rem', bottom: 'auto'}),
                ...(placement.endsWith('-start') && {left: 'var(--Snackbar-padding)'}),
                ...(placement.endsWith('-end')   && {right: 'var(--Snackbar-padding)', left: 'auto'}),
                ...(!placement.endsWith('-start') && !placement.endsWith('-end') && {left: 'calc(50% - 0.5rem)'}),
            }
        };
    };

    return (
        <Popper // Docs: https://v5.mui.com/base-ui/react-popper/
            open={Boolean(open)}
            anchorEl={anchorEl}
            placement={placement}
            modifiers={[{name: 'offset', options: {offset: [0, 8]}}]}
            style={{zIndex: 'var(--joy-zIndex-snackbar)'}}
        >
            <Snackbar
                open={true}
                size='lg'
                variant='solid' //to make it look different from alerts
                color='primary'
                invertedColors={true}
                onClose={handleClose}
                slotProps={{root: {style: {position: 'static'}}}}
                sx={{...sx, ...arrowSx(placement)}}
                startDecorator={<TipsAndUpdates/>}
                endDecorator={
                    <Button
                        onClick={(e) => handleClose(e, 'button')}
                        variant='outlined'
                        color='primary'>
                        Got it
                    </Button>
                }>
                {hintText}
            </Snackbar>
        </Popper>
    );
}

GuidedHint.propTypes = {
    open: bool.isRequired,
    onClose: func,
    dismissOnClickaway: bool,
    anchorEl: object.isRequired,
    hintText: string.isRequired,
    placement: string,
    sx: object,
};


export const APP_HINT_IDS = {
    TABS_MENU: 'tabsMenu',
    BG_MONITOR: 'bgMonitor'
};

/**An app hint needs to be shown only the first time user loads an app. So this is controlled by a flag saved as app preference**/
export const appHintPrefName = (appTitle, hintId) => `showAppHint__${appTitle}--${hintId}`;

/**
 * Displays a one-time contextual hint anchored to a DOM element via a Popper portal.
 * Dismissed state is persisted as an app preference so the hint never reappears after dismissal.
 *
 * @param {object} props              - the props are forwarded to {@link GuidedHint} (anchorEl, hintText, placement, sx);
 *                                      those unique to this component are described below
 * @param {string} props.appTitle     - App name; scopes the preference key so hints are per-app.
 * @param {string} props.id           - Unique hint ID within the app (use APP_HINT_IDS constants).
 */
export function AppHint({appTitle, id, ...props}) {
    const open = useStoreConnector(() => Boolean(getPreference(appHintPrefName(appTitle, id), true)));
    return (
        <GuidedHint
            {...props}
            open={open}
            onClose={() => dispatchAddPreference(appHintPrefName(appTitle, id), false)}
        />
    );
}

AppHint.propTypes = {
    appTitle: string.isRequired,
    id: string.isRequired,
    ...GuidedHint.propTypes,
};
