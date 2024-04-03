import React, {useEffect} from 'react';
import {ScopedCssBaseline, extendTheme, CssVarsProvider, useColorScheme, GlobalStyles} from '@mui/joy';
import {AppPropertiesCtx} from './AppPropertiesCtx.jsx';

import {defaultTheme} from './ThemeSetup.js';
import {getAppOptions} from '../core/AppDataCntlr.js';
import {logger} from '../util/Logger.js';

import '@fontsource/inter/200.css'; // Lighter
import '@fontsource/inter/300.css'; // Light
import '@fontsource/inter/400.css'; // Regular
import '@fontsource/inter/500.css'; // Medium
import '@fontsource/inter/600.css'; // Semi-Bold
import '@fontsource/inter/700.css'; // Bold
import '@fontsource/inter/800.css'; // Bolder

export function FireflyRoot({sx, children, ctxProperties={}}) {

    const customTheme = getAppOptions().theme?.customized?.();
    const theme = extendTheme(customTheme || defaultTheme());

    return (
        <AppPropertiesCtx.Provider value={ctxProperties}>
            <CssVarsProvider defaultMode='system' theme={theme}>
                <GlobalStyles styles={{
                    html: {fontSize:'87.5%'}
                }}/>
                <ScopedCssBaseline sx={{
                    flexGrow:1,
                    height:1,
                    position:'relative',
                    fontSmooth: 'unset',
                    WebkitFontSmoothing: 'unset',
                    ...sx
                }}>
                    <App>{children}</App>
                </ScopedCssBaseline>
            </CssVarsProvider>
        </AppPropertiesCtx.Provider>
    );
}

function App({children}) { // provide a way to experiment with light and dark theme

    // Firefly is setup to take the device's color mode
    // To set device mode on your mac; AppleMenu -> System Settings -> Appearance

    const { activeMode, setMode } = useColorMode();

    const colorMode = getAppOptions().theme?.colorMode;
    // if colorMode is set; must use this mode
    // To add mode switching; make sure colorMode is not set.
    // Then, add a component(Button) to trigger 'setMode'.
    // setMode store the value in localStorage.  It will use this mode
    // until that value is cleared or kill/restart browser.

    logger.info({activeMode, colorMode});

    useEffect(() => {
        // only setMode when colorMode is different from the current activeMode
        if (colorMode && colorMode !== activeMode) {
            setMode(colorMode);
        }
    }, [colorMode, activeMode]);
    return ( children );
}


/**
 * A wrapper around useColorScheme to return the current active mode, which should be either light or dark
 * @return {object}
 */
export function useColorMode() {
    const { mode, systemMode, ...rest } = useColorScheme();
    const activeMode = mode === 'system' ? systemMode : mode;
    return {activeMode, isDarkMode:activeMode==='dark', mode, systemMode, ...rest};

}

/**
 * @param {*} light The value to be returned when the color mode is light.
 * @param {*} dark The value to be returned when the color mode is dark.
 * @param {object} theme The current theme
 * @returns {*} returns the appropriate value based on the color mode (light or dark) of the theme.
 */
export function mode(light, dark, theme) {
    return theme?.palette?.mode === 'dark' ? dark : light;
}

