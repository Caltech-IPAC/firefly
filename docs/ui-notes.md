# UI Notes

### Miscellaneous
- toolbar background: `<Sheet variant='soft'>` or `<Sheet className='TapSearch__section' variant='outline' sx={{flexDirection: 'column', flexGrow: 1}}>`
- Use `Divider` instead of border where possible
- `sx` is mostly not necessary for `Box` and `Stack` as these are considered `CSS Utility` components in Joy UI
  - instead, if you have something like:
    - `<Stack justifyContent='space-between' spacing={1} sx={{resize: 'both', overflow: 'auto'}}>`
    - it could just become: `<Stack {...{justifyContent:'space-between', spacing:1, resize:'both', overflow: 'auto'}}>`
- Avoid using component with Typography like this unless there's a good reason to do so: `<Typography component='div'>`

### Buttons
- primary button:  `<Button size:'md', variant: 'solid' color: 'primary'/>`
- secondary button:  `<Button/>` (see above for default)
- significant action button: (eg file upload): `<Button color='success' variant='solid'/>`
- other buttons: `<Chip/>` and `<Link/>` (see above for defaults)

### Text
- information: `<Typography/>`
- labels: `<Typography/>`
- feedback: `<Typography color='warning'/>`
- Do not use bold or italic directly (in fact, avoid using fontStyle, fontWeight, fontSize)
  - Instead of bold or fontSize, use level with `<Typography/>`
  - Instead of italic, use a color (like warning) or `Link` / `Chip` if it's a link

### Colors
- Avoid setting color directly, instead use JoyUI colors like `color='success'`, `color='primary'`, `color='warning'`, `color='danger'`, etc.
- This is important especially for dark mode. If we use JoyUI colors, JoyUI handles the changes when switching to dark mode. 

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