import {extendTheme} from '@mui/joy';

export function getTheme() {
    return extendTheme({
        components: {
            JoyButton: {
                defaultProps: {
                    size:'sm' ,
                    variant:'soft' ,
                    color:'neutral'
                }
            },
            JoyInput: {
                defaultProps: { size:'sm' }
            },
            JoyIconButton: {
                defaultProps: {
                    size:'sm' ,
                    variant:'plain',
                    color:'neutral',
                }
            },
            JoyFormControl: {
                defaultProps: { size:'sm' }
            },
            JoyFormLabel: {
                // styleOverrides: {
                //     root: ({theme}) => ({lineHeight:'50px'})
                // }
                defaultProps: {
                    size:'sm',
                    sx : {
                        '--FormLabel-lineHeight' : 1.1
                    }
                }
            },
            JoySelect: {
                defaultProps: { size:'sm' }
            },
            JoyRadio: {
                defaultProps: { size:'sm' }
            },
            JoyRadioGroup: {
                defaultProps: {
                    size:'sm',
                    sx : {
                        '--unstable_RadioGroup-margin': '0.2rem 0 0.2rem 0'
                    }
                },
            },
            JoyCheckbox: {
                defaultProps: {
                    size:'sm',
                },
            },
            JoyToggleButtonGroup: {
                defaultProps: {
                    color:'primary',
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

            }
        }
    });
}
