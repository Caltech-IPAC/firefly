/** @jsx jsx */
import {jsx, css} from '@emotion/react'; //set jsx pragma for detection of "css" prop: https://emotion.sh/docs/css-prop#import-the-jsx-function-from-emotionreact
import {Box} from '@mui/joy';
import {useColorMode} from 'firefly/ui/FireflyRoot';
import {oneOfType, shape, string} from 'prop-types';

/**
 * Allows inserting css string to JoyUI components, with optional light/dark theme handling.
 * It can also be used to insert css file(s) if the file is imported as a string using raw-loader syntax.
 *
 * @param p
 * @param p.cssStr {string|Object} css string. If theme needs to be handled, pass `{light: cssString, dark: cssString}`.
 * @param p.children {React.ReactNode}
 */
export default function CssStrThemeWrapper({cssStr, children}) {
    const {isDarkMode} = useColorMode();
    const themeCssStr = (isDarkMode ? cssStr?.dark : cssStr?.light) ?? cssStr ?? '';

    //convert string styles to SerializedStyles that are understandable by emotion "css" prop, by using css`` template literal
    const themeCssStyles = css`${themeCssStr}`;

    return (
        // JoyUI Box accepts emotion "css" prop natively: https://mui.com/material-ui/integrations/interoperability/#the-css-prop
        <Box css={themeCssStyles}>
            {children}
        </Box>
    );
}

CssStrThemeWrapper.propTypes = {
    cssStr: oneOfType([
        string,
        shape({
            light: string,
            dark: string
        })
    ])
};