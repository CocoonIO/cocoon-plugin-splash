cordova.define('cordova/plugin_list', function(require, exports, module) {
module.exports = [
    {
        "file": "plugins/cordova-plugin-whitelist/whitelist.js",
        "id": "cordova-plugin-whitelist.whitelist",
        "runs": true
    },
    {
        "file": "plugins/com.ludei.cocoon.common/www/cocoon.js",
        "id": "com.ludei.cocoon.common.Cocoon",
        "merges": [
            "window.Cocoon"
        ]
    },
    {
        "file": "plugins/com.ludei.canvasplus.common/www/cocoon_canvasplus.js",
        "id": "com.ludei.canvasplus.common.CanvasPlus",
        "runs": true
    }
];
module.exports.metadata = 
// TOP OF METADATA
{
    "cordova-plugin-whitelist": "1.0.0",
    "com.ludei.canvasplus.android": "1.0.1",
    "com.ludei.canvasplus.ios": "2.1.2",
    "com.ludei.splash.android": "1.0.0",
    "com.ludei.cocoon.common": "1.0.0",
    "com.ludei.canvasplus.common": "1.0.1"
}
// BOTTOM OF METADATA
});