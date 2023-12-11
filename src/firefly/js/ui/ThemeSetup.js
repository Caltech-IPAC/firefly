import {extendTheme} from '@mui/joy';


export function getTheme() {
    return extendTheme({
        components: {
            JoyButton: {
                defaultProps: {
                    variant:'soft' ,
                    color:'neutral'
                }
            },
            JoyInput: {
                styleOverrides: {
                    root: {
                        minHeight: '1.75rem',
                    },
                },
            },
            JoyIconButton: {
                defaultProps: {
                    variant:'plain',
                    color:'neutral',
                }
            },
            JoyFormLabel: {
                // defaultProps: {
                //     sx : {
                //         '--FormLabel-lineHeight' : 1.1
                //     }
                // }
            },
            JoyRadioGroup: {
                defaultProps: {
                    sx : {
                        '--unstable_RadioGroup-margin': '0.2rem 0 0.2rem 0'
                    }
                },
            },
            JoyCheckbox: {
            },
            JoyToggleButtonGroup: {
                defaultProps: {
                    variant:'soft',
                },
            },
            JoyTooltip: {
                defaultProps: {
                    variant:'soft',
                    enterDelay:1500,
                    placement: 'bottom-start',
                    arrow: true,

                }

            },
            JoyLink: {
                defaultProps: {
                    underline: 'always',
                    color: 'primary'
                }
            }
        }
    });
}
