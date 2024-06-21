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
                    disableInteractive: true,
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
                    underline: 'hover',
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
            },
            JoySkeleton: {
                defaultProps: {
                    animation: 'wave',
                }
            }
        }
    });
}


/*
 *  UI Notes: refer to /docs/ui-notes.md and add stuff there
 *
 */