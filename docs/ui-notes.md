# UI Notes

### Miscellaneous
- toolbar background: `<Sheet variant='soft'>` or `<Sheet className='TapSearch__section' variant='outline' sx={{flexDirection: 'column', flexGrow: 1}}>`

### Buttons
- primary button:  `<Button size:'md', variant: 'solid' color: 'primary'/>`
- secondary button:  `<Button/>` (see above for default)
- significant action button: (eg file upload): `<Button color='success' variant='solid'/>`
- other buttons: `<Chip/>` (see above for defaults)

### Text
- information: `<Typography/>`
- labels: `<Typography/>`
- feedback: `<Typography color='warning'/>`

### Spacing and Layout:
- **Control spacing from the top:** Unless absolutely necessary, avoid applying padding/margin to individual elements for creating space from their sibling elements. Instead, set spacing at the parent element by using `<Stack spacing={number}>`, which also enforces visual consistency and maintainability.
- **Match spacing with visual relationships:**
  - Decrease spacing gradually from parent to child elements in the code to visually reinforce hierarchy. E.g.:
    ```jsx
    <Stack spacing={2}>
        <Stack spacing={1}> {/* Visual group 1 */}
            <Component1/>
            <Component1HelperText/>
        </Stack>
        <Stack spacing={1}> {/* Visual group 2 */}
            <Component2/>
            <Component2HelperText/>
        </Stack>
    </Stack>
    ```
  - Enforce a sibling relationship by setting equal spacing between elements, even if they have a parent-child relationship in the code. E.g.:
    ```jsx
    <Stack spacing={1}>
        <ComponentModeSelection/>
        <ComponentWithHelperText/> {/* Internally uses <Stack spacing={1}> */}
    </Stack>
    ```
    Intentionally setting spacing=1 instead of 2 will make `ComponentModeSelection` appear on same level as constituents of `ComponentWithHelperText`.