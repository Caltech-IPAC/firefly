import { mergeConfig } from 'vite';
import {
    workerPlugin,
    muiIconsNormalizePlugin,
    muiIconsEsmPlugin,
    jsJsxPlugin,
    sharedConfig,
} from '../vite.config.js';

/** @type { import('@storybook/react-vite').StorybookConfig } */
const config = {
    stories: ['../stories/**/*.mdx', '../stories/**/*.stories.@(js|jsx)'],
    addons: [
        '@storybook/addon-essentials',
        '@storybook/addon-storysource',
    ],
    framework: {
        name: '@storybook/react-vite',
        options: {},
    },
    docs: {
        autodocs: 'tag',
    },
    async viteFinal(config) {
        return mergeConfig(config, {
            // @storybook/react-vite adds the React plugin itself, so omit it here.
            // muiIconsEsmPlugin is a renderChunk hook — no-op during dev, but
            // required for build-storybook to avoid the CJS isNodeMode=1 TDZ error.
            plugins: [workerPlugin, muiIconsNormalizePlugin, muiIconsEsmPlugin, jsJsxPlugin,
                // Stub out to resolve circular import issues.
                {
                    name: 'stub-resolve-circular-import',
                    enforce: 'pre',
                    load(id) {
                        if (!id.includes('/api/ApiUtilChart')) return null;
                        return 'export default {};';
                    },
                },
            ],
            ...sharedConfig,
        });
    },
};

export default config;
