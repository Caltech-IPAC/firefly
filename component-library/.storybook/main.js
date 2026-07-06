import { mergeConfig } from 'vite';
import {
    workerPlugin,
    muiIconsNormalizePlugin,
    muiIconsEsmPlugin,
    jsJsxPlugin,
    sharedConfig,
} from '../vite.config.js';

// Stubs *.worker.js imports during esbuild's dep-scan phase (dev server only).
// workerPlugin rewrites them at serve time, but Vite plugins don't run during
// dep-scan — so esbuild needs its own stub to avoid a "missing default export" error.
export const workerDepScanStub = {
    name: 'worker-dep-scan-stub',
    config(_, { command }) {
        if (command !== 'serve') return;
        return {
            optimizeDeps: {
                esbuildOptions: {
                    plugins: [{
                        name: 'worker-stub',
                        setup(build) {
                            build.onResolve({ filter: /\.worker\.js$/ }, (args) => ({
                                path: args.path, namespace: 'worker-stub',
                            }));
                            build.onLoad({ filter: /.*/, namespace: 'worker-stub' }, () => ({
                                contents: 'export default class {}', loader: 'js',
                            }));
                        },
                    }],
                },
            },
        };
    },
};

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
            plugins: [workerPlugin, muiIconsNormalizePlugin, muiIconsEsmPlugin, jsJsxPlugin, workerDepScanStub],
            ...sharedConfig,
        });
    },
};

export default config;
