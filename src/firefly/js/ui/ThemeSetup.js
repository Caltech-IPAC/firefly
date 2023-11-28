import {extendTheme} from '@mui/joy';

export function getTheme() {
    return extendTheme({
        components: {
            JoyInput: {
                defaultProps: { size:'sm' }
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
