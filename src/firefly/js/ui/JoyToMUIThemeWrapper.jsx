import React, {useEffect} from 'react';
import {THEME_ID as MATERIAL_THEME_ID,
    experimental_extendTheme as materialExtendTheme,
    Experimental_CssVarsProvider as MaterialCssVarsProvider,
    useColorScheme as useMaterialColorScheme
} from '@mui/material/styles';
import {CssVarsProvider as JoyCssVarsProvider} from '@mui/joy/styles';
import {useTheme} from '@mui/material';


/**
 * This is needed to use MUI X components in JoyUI, otherwise they will throw errors because they expect MUI theme.
 * Adapted from the example code present at https://mui.com/x/react-date-pickers/custom-field/#using-another-input
 * and https://mui.com/joy-ui/integrations/material-ui/
 *
 * Note: When JoyUI is mature enough to support more MUI components, this wrapper won't be needed and should be removed.
 *
 * @param p
 * @param p.children
 */
export function JoyToMUIThemeWrapper({children}) {
    const joyTheme = useTheme();
    const joyMode = joyTheme.palette.mode;
    const joyPrimaryPalette = joyTheme.palette.primary;

    // Override the default MUI appearance of wrapped component (esp font, color, etc.) with JoyUI theme
    // Note: MUI and JoyUI theme objects are not identical by design, so following doesn't cover all the cases
    const materialTheme = materialExtendTheme({
        typography: {
            fontFamily: joyTheme.fontFamily.body
        },
        colorSchemes: Object.fromEntries(['light', 'dark'].map((k)=> [k, {
            palette: {
                // joyUI primary colors remain same for both dark and light mode
                primary: {
                    main: varColorStrToHex(joyTheme, joyPrimaryPalette.solidBg, joyPrimaryPalette[500]),
                    // 'dark' is used by MUI for hover
                    dark: varColorStrToHex(joyTheme, joyPrimaryPalette.solidHoverBg, joyPrimaryPalette[600]),
                }
            }
        }]))
    });

    return (
        <MaterialCssVarsProvider theme={{ [MATERIAL_THEME_ID]: materialTheme }}>
            <JoyCssVarsProvider>
                <SyncThemeMode mode={joyMode} />
                {children}
            </JoyCssVarsProvider>
        </MaterialCssVarsProvider>
    );
}


// To sync MUI theme's mode with JoyUI theme's mode
const SyncThemeMode = ({ mode }) => {
    const { setMode: setMaterialMode } = useMaterialColorScheme();
    useEffect(() => {
        setMaterialMode(mode);
    }, [mode, setMaterialMode]);
    return null;
};


// MUI doesn't allow CSS var() str for color, so convert that to hex color str
function varColorStrToHex(theme, colorStr, defaultHex) {
    const regex = /^var\(--joy-palette-(.+?)-(\d+), #([0-9a-fA-F]{6})\)$/;
    const match = colorStr.match(regex);

    if (match) {
        const [, paletteName, shade, hexCode] = match;
        return theme?.palette?.[paletteName]?.[shade] ?? hexCode;
    } else {
       return defaultHex;
    }
}