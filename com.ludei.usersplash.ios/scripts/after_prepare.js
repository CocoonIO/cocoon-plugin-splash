var fs = require('fs');
var path = require('path');
var Canvas = require('canvas');
var sizeOf = require('image-size');

var SCALEMODE_ASPECTFIT = 'scaleAspectFit';
var SCALEMODE_ASPECTFILL = 'scaleAspectFill';
var SCALEMODE_FILL = 'scaleToFill';

module.exports = function(context) {
    if (context.opts.cordova.platforms.indexOf('ios') <= -1)
        return;

    // Get the ios project name
    var ios_path = fs.readdirSync(path.join(context.opts.projectRoot, "platforms", "ios"));
    var xcodeproj_path = ios_path.filter(function(item) { 
        return item.indexOf("xcodeproj") !== -1; 
    })[0];
    var project_name = xcodeproj_path.substring(0, xcodeproj_path.lastIndexOf("."));

    // Read the prepared config.xml
    var config_xml_path = path.join(context.opts.projectRoot, 'platforms', 'ios', project_name, 'config.xml');

    var et = context.requireCordovaModule('elementtree');
    var data = fs.readFileSync(config_xml_path).toString();
    var etree = et.parse(data);
    var root = etree.getroot();

    var CocoonSplashImage = path.join(context.opts.projectRoot, '..', 'splashes', 'ios.png');
    var CocoonSplashBackgroundColor = "black";
    var CocoonSplashScaleMode = SCALEMODE_ASPECTFIT;
    var CocoonSplashPadding = 0;

    var preferences = etree.findall('./preference');
    for (var i=0; i<preferences.length; i++) {
        var preference = preferences[i];
        var name = preference.get("name", null);
        if (name !== null) {
            if (name.indexOf("CocoonSplashImage") > -1) {
                var value = preference.get("value", null);
                if (value != null && value.indexOf("cocoon://backend") === -1) {
                    CocoonSplashImage = path.join(context.opts.projectRoot, value);                
                }
          
            } else if (name.indexOf("CocoonSplashBackgroundColor") > -1) {
                CocoonSplashBackgroundColor = preference.get("value", "black");

            } else if (name.indexOf("CocoonSplashScaleMode") > -1) {
                CocoonSplashScaleMode = preference.get("value", SCALEMODE_ASPECTFIT);

            } else if (name.indexOf("CocoonSplashPadding") > -1) {  
                CocoonSplashPadding = preference.get("value", "0");
            }
        }
    }

    try {
        var src_image_file = fs.readFileSync(CocoonSplashImage);
        var dst_image_path = path.join(context.opts.projectRoot, 'platforms', 'ios', project_name, 'Resources', 'cocoonSplashImage.png');
        fs.writeFileSync(dst_image_path, src_image_file);

    } catch (err) {
        throw new Error('Cannot find image referenced in preference "CocoonSplashImage": ' + err);
    }

    // Now we create the Launch Images from the user's splash screen
    var splashes_path = path.join(context.opts.projectRoot, 'platforms', 'ios', project_name, 'Images.xcassets', 'LaunchImage.launchimage');
    if (!fs.existsSync(splashes_path))
        throw Error("Can't find splashes directory: " + splashes_path);

    var splashes = fs.readdirSync(splashes_path);
    splashes.forEach(function(splash) {
        try {
            var splash_path = path.join(splashes_path, splash);
            var dimensions = sizeOf(splash_path);
            var imageData = fs.readFileSync(CocoonSplashImage);
            var dataURL = renderSplash(imageData, dimensions.width, dimensions.height, CocoonSplashScaleMode, CocoonSplashBackgroundColor, CocoonSplashPadding);
            var base64Data = dataURL.replace(/^data:image\/png;base64,/, "");
            fs.writeFileSync(splash_path, base64Data, 'base64');
            
        } catch (e) {
            console.error(splash + " is not a valid image");
        }
    });
}

function renderSplash(data, width, height, mode, bgcolor, paddingValue) {
    var canvas = new Canvas(width, height);

    mode = mode || SCALEMODE_ASPECTFIT;
    var padding = parseInt(paddingValue) || 0;
    if ((paddingValue + '').indexOf('%') >=0) {
      padding = Math.min(canvas.width, canvas.height) * padding/100.0;
    }
    bgcolor = bgcolor || '#000000  ';
    mode = mode || 'scaleAspectFill'.toLowerCase();

    var ctx = canvas.getContext("2d");
    var w = canvas.width;
    var h = canvas.height;

    ctx.fillStyle = bgcolor;
    ctx.fillRect(0, 0, w, h);

    var image = new Canvas.Image();
    image.src = data;
    var tw = w - padding * 2;
    var th = h - padding * 2;
    var canvasAspectRatio = tw/th;
    var imageAspectRatio = image.width/image.height;

    var dx, dy, dw, dh;

    if (mode.toLowerCase() === SCALEMODE_ASPECTFIT.toLowerCase()) {
      if (canvasAspectRatio > imageAspectRatio) {
        dw = th/image.height * image.width;
        dh = th;
      }
      else {
        dw = tw;
        dh = tw/image.width * image.height;
      }
      dx = canvas.width/2 - dw/2;
      dy = canvas.height/2 - dh/2;
    }
    else if (mode.toLowerCase() === SCALEMODE_ASPECTFILL.toLowerCase()) {
      if (imageAspectRatio > canvasAspectRatio) {
        dw = th/image.height * image.width;
        dh = th;
      }
      else {
        dw = tw;
        dh = tw/image.width * image.height;
      }
      dx = canvas.width/2 - dw/2;
      dy = canvas.height/2 - dh/2;
    }
    else { //SCALEMODE_FILL
      dw = tw;
      dh = th;
      dx = padding;
      dy = padding;
    }

    ctx.drawImage(image, dx, dy, dw, dh);

    return canvas.toDataURL("image/png");
  }