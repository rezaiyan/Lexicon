// Stub Node.js modules that sql.js references but doesn't need in the browser
config.resolve = config.resolve || {};
config.resolve.fallback = Object.assign(config.resolve.fallback || {}, {
    "path": false,
    "fs": false,
    "crypto": false
});

// Copy sql-wasm.wasm to the output directory so the web worker can load it
const CopyWebpackPlugin = require("copy-webpack-plugin");
config.plugins.push(
    new CopyWebpackPlugin({
        patterns: [
            {
                from: "../../node_modules/sql.js/dist/sql-wasm.wasm",
                to: "."
            }
        ]
    })
);
