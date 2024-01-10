/**
 * Returns the overrides portion of JoyUI's theme.
 * This will be used to extend(extendTheme) JoyUI's default theme.
 *
 * To customize Firefly's theme, start with the defaultTheme.  Then, update or add as needed.
 * Next, assign the customized theme to the application options.  e.g. firefly.theme.customized = () => ({<custom-theme>})

 * @return {object}
 */
export function defaultTheme() {
    return ({
        components: {
            JoyButton: {
                defaultProps: {
                    variant:'soft' ,
                    color:'primary',
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
            },
            JoyChip: {
                defaultProps: {
                    size:'sm',
                }
            }
        }
    });
}


/*
 *  UI Notes
 * - toolbar background: <Sheet variant='soft'> or <Sheet>
        <Sheet className='TapSearch__section' variant='outline' sx={{flexDirection: 'column', flexGrow: 1}}>
 *
 * Buttons
 *    - primary button:  <Button size:'md', variant: 'solid' color: 'primary'/>
 *    - secondary button:  <Button/> (see above for default)
 *    - significant action button: (eg file upload): <Button color='success' variant='solid'/>k
 *    - other buttons: <Chip/> (see above for defaults)
 *
 * Text
 *    - information: <Typography/>
 *    - labels:  <Typography/>
 *    - feedback:  <Typography color='warning'/>
 */