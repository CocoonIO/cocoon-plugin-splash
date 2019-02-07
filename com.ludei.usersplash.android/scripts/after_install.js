var fs = require('fs');
var path = require('path');

module.exports = function(context) {
    if (context.opts.cordova.platforms.indexOf('android') <= -1)
        return;

    var config_xml_path = path.join(context.opts.projectRoot, 'platforms', 'android', 'app', 'src', 'main', 'res', 'xml', 'config.xml');

    var et = context.requireCordovaModule('elementtree');
    var data = fs.readFileSync(config_xml_path).toString();
    var etree = et.parse(data);
    var root = etree.getroot();

    var CocoonSplashImage = path.join(context.opts.projectRoot, '..', 'splashes', 'android.png');

    var found = false;
    var preferences = etree.findall('./preference');
    for (var i=0; i<preferences.length && !found; i++) {
        var preference = preferences[i];
        var name = preference.get("name", null);
        if (name !== null && name.indexOf("CocoonSplashImage") > -1) {
            var value = preference.get("value", null);
            if (value != null && value.indexOf("cocoon://backend") === -1) {
                CocoonSplashImage = path.join(context.opts.projectRoot, value);
            }

            found = true;
        }
    }

    try {
        var src_image_file = fs.readFileSync(CocoonSplashImage);
        var dst_image_path = path.join(context.opts.projectRoot, 'platforms', 'android', 'app', 'src', 'main', 'assets', 'cocoonSplashImage.png');
        fs.writeFileSync(dst_image_path, src_image_file);
    
    } catch(err) {
        throw new Error('Cannot find image referenced in preference "CocoonSplashImage": ' + err);
    }
}