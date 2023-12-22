import {extendTheme} from '@mui/joy';


export function getTheme() {
    return extendTheme({
        components: {
            JoyButton: {
                defaultProps: {
                    variant:'soft' ,
                    // variant:'outlined' ,
                    // color:'neutral',
                    color:'primary',
                    // color:'warning',
                    size: 'md'
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
                defaultProps: {
                    sx : {
                        '--FormLabel-lineHeight' : 1.1
                    }
                }
            },
            JoyRadioGroup: {
                defaultProps: {
                    sx : {
                        '--unstable_RadioGroup-margin': '0.2rem 0 0.2rem 0'
                    }
                },
            },
            JoyToggleButtonGroup: {
                defaultProps: {
                    variant:'soft',
                    color: 'neutral'
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
            JoyTypography: {
                defaultProps: {
                    level:'body-md',
                }
            },
            JoyLink: {
                defaultProps: {
                    underline: 'always',
                    color: 'primary'
                }
            },
            JoyBadge: {
                defaultProps: {
                    size:'sm',
                    color:'primary',
                    sx:{'.MuiBadge-badge': {top:9, right:6}}
                }
            }
        }
    });
}


/*
 *  UI Notes
 *      - toolbar background: <Sheet variant='soft'>
 */