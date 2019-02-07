#import "UserSplashPlugin.h"
#import <Cordova/CDVViewController.h>

#define kScaleAspectFit @"scaleAspectFit"
#define kScaleAspectFill @"scaleAspectFill"
#define kScaleToFill @"scaleToFill"

#define kSplashPaddingDefault @"0"
#define kDefaultSplashScreenDurationDefault 3000
#define kSplashScreenDurationDefault 5000
#define kSplashScreenBackgroundColorDefault @"black"

#define SPLASH_DEFAULT_IMAGE @"cocoonSplashImage.png"

#define SPLASH_SCALE_MODE @"CocoonSplashScaleMode"
#define SPLASH_BACKGROUND_COLOR @"CocoonSplashBackgroundColor"
#define SPLASH_DELAY @"CocoonSplashDelay"
#define SPLASH_PADDING @"CocoonSplashPadding"

@implementation UserSplashPlugin

- (void)pluginInitialize {
    NSString* scaleMode = [self.commandDelegate.settings objectForKey:[SPLASH_SCALE_MODE lowercaseString]];
    if (scaleMode == nil) {
        splashScaleMode = kScaleAspectFit;
    
    } else {
        if ([splashScaleMode isEqualToString:kScaleAspectFit] || [splashScaleMode isEqualToString:kScaleAspectFill] || [splashScaleMode isEqualToString:kScaleToFill]) {
            splashScaleMode = scaleMode;
            
        } else {
            splashScaleMode = kScaleAspectFit;
        }
    }
    
    NSString* backgroundColor = [self.commandDelegate.settings objectForKey:[SPLASH_BACKGROUND_COLOR lowercaseString]];
    if (backgroundColor == nil) {
        splashBackgroundColor = kSplashScreenBackgroundColorDefault;
        
    } else {
        splashBackgroundColor = backgroundColor;
    }
    
    NSString* delay = [self.commandDelegate.settings objectForKey:[SPLASH_DELAY lowercaseString]];
    if (delay == nil) {
        splashDelay = kSplashScreenDurationDefault;
        
    } else {
        splashDelay = [delay intValue] * 1000;
    }
    
    NSString* padding = [self.commandDelegate.settings objectForKey:[SPLASH_PADDING lowercaseString]];
    if (padding == nil) {
        splashPadding = kSplashPaddingDefault;
        
    } else {
        splashPadding = padding;
    }
    
    Class splashClass = NSClassFromString(@"SplashPlugin");
    if (splashClass != NULL) {
        splashDelay += kDefaultSplashScreenDurationDefault;
    }
    
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(orientationChange:)
                                                 name:UIApplicationDidChangeStatusBarOrientationNotification
                                               object:nil];
    
    [self showSplash];
}

- (void)showSplash {
    UIView* parentView = self.viewController.view;
    parentView.userInteractionEnabled = NO;
    
    UIImage* image = [self getSplashView];
    
    _imageView = [[UIImageView alloc] initWithFrame:[UIScreen mainScreen].bounds];
    _imageView.image = image;
    _imageView.backgroundColor = [UserSplashPlugin UIColorFromHTMLColor:splashBackgroundColor];
    [parentView addSubview:_imageView];
    
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, splashDelay*NSEC_PER_MSEC), dispatch_get_main_queue(), ^{
        [self hideSplash];
    });
}

- (void)hideSplash {
    [_imageView removeFromSuperview];
    self.viewController.view.userInteractionEnabled = YES;
    _imageView = nil;
    
    [[NSNotificationCenter defaultCenter] removeObserver:self];
}

- (void)orientationChange:(NSNotification *)notificacion {
    if (_imageView == nil)
        return;
    
    [_imageView removeFromSuperview];
    
    _imageView = [[UIImageView alloc] initWithFrame:[UIScreen mainScreen].bounds];
    _imageView.image = [self getSplashView];
    _imageView.backgroundColor = [UserSplashPlugin UIColorFromHTMLColor:splashBackgroundColor];
    UIView* parentView = self.viewController.view;
    [parentView addSubview:_imageView];
}

- (UIImage*)getSplashView {
    UIImage * originalImage = [UIImage imageNamed:SPLASH_DEFAULT_IMAGE];
    
    CGRect rect = [UIScreen mainScreen].bounds;
    CGSize size = rect.size;
    CGFloat scale = [[UIScreen mainScreen] scale];
    
    CGFloat padding = 0.0f;
    NSUInteger location = [splashPadding rangeOfString:@"%"].location;
    if (location != NSNotFound) {
        padding = [[splashPadding substringToIndex:location] floatValue];
        padding = MIN(size.width, size.height) * padding/100.0;
        
    } else {
        padding = [splashPadding floatValue];
    }
    
    CGFloat tw = size.width - padding * 2;
    CGFloat th = size.height - padding * 2;
    CGFloat canvasAspectRatio = tw/th;
    CGFloat imageAspectRatio = originalImage.size.width/originalImage.size.height;
    
    CGFloat dx = 0.0f;
    CGFloat dy = 0.0f;
    CGFloat dw = 0.0f;
    CGFloat dh = 0.0f;
    
    if ([[splashScaleMode lowercaseString] compare:[kScaleAspectFit lowercaseString]] == NSOrderedSame) {
        if (canvasAspectRatio > imageAspectRatio) {
            dw = th/originalImage.size.height * originalImage.size.width;
            dh = th;
        }
        else {
            dw = tw;
            dh = tw/originalImage.size.width * originalImage.size.height;
        }
        dx = size.width/2 - dw/2;
        dy = size.height/2 - dh/2;
        
    } else if ([[splashScaleMode lowercaseString] compare:[kScaleAspectFill lowercaseString]] == NSOrderedSame) {
        if (imageAspectRatio > canvasAspectRatio) {
            dw = th/originalImage.size.height * originalImage.size.width;
            dh = th;
        }
        else {
            dw = tw;
            dh = tw/originalImage.size.width * originalImage.size.height;
        }
        dx = size.width/2 - dw/2;
        dy = size.height/2 - dh/2;
        
    } else if ([[splashScaleMode lowercaseString] compare:[kScaleToFill lowercaseString]] == NSOrderedSame) {
        dw = tw;
        dh = th;
        dx = padding;
        dy = padding;
    }
    
    UIGraphicsBeginImageContextWithOptions(size, NO, scale);
    CGContextRef context = UIGraphicsGetCurrentContext();
    CGContextSetFillColorWithColor(context, [[UserSplashPlugin UIColorFromHTMLColor:splashBackgroundColor] CGColor]);
    CGContextFillRect(context, rect);
    [originalImage drawInRect:CGRectMake(dx, dy, dw, dh)];
    UIImage *image = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    
    return image;
}

+ (UIColor *)UIColorFromHTMLColor:(NSString *)htmlColor {
    NSDictionary *colorHashMap = @{
        @"aqua":@"#00ffff",
        @"black":@"#000000",
        @"blue":@"#0000ff",
        @"fuchsia":@"#ff00ff",
        @"green":@"#008000",
        @"gray":@"#808080",
        @"grey":@"#808080",
        @"lime":@"#00ff00",
        @"maroon":@"#800000",
        @"navy":@"#000080",
        @"olive":@"#808000",
        @"purple":@"#800080",
        @"red":@"#ff0000",
        @"silver":@"#c0c0c0",
        @"teal":@"#008080",
        @"white":@"#ffffff",
        @"yellow":@"#ffff00",
        @"aliceblue":@"#f0f8ff",
        @"antiquewhite":@"#faebd7",
        @"aquamarine":@"#7fffd4",
        @"azure":@"#f0ffff",
        @"beige":@"#f5f5dc",
        @"bisque":@"#ffe4c4",
        @"blanchedalmond":@"#ffebcd",
        @"blueviolet":@"#8a2be2",
        @"brown":@"#a52a2a",
        @"burlywood":@"#deb887",
        @"cadetblue":@"#5f9ea0",
        @"chartreuse":@"#7fff00",
        @"chocolate":@"#d2691e",
        @"coral":@"#ff7f50",
        @"cornflowerblue":@"#6495ed",
        @"cornsilk":@"#fff8dc",
        @"crimson":@"#dc143c",
        @"cyan":@"#00ffff",
        @"darkblue":@"#00008b",
        @"darkcyan":@"#008b8b",
        @"darkgoldenrod":@"#b8860b",
        @"darkgray":@"#a9a9a9",
        @"darkgreen":@"#006400",
        @"darkkhaki":@"#bdb76b",
        @"darkmagenta":@"#8b008b",
        @"darkolivegreen":@"#556b2f",
        @"darkorange":@"#ff8c00",
        @"darkorchid":@"#9932cc",
        @"darkred":@"#8b0000",
        @"darksalmon":@"#e9967a",
        @"darkseagreen":@"#8fbc8f",
        @"darkslateblue":@"#483d8b",
        @"darkslategray":@"#2f4f4f",
        @"darkturquoise":@"#00ced1",
        @"darkviolet":@"#9400d3",
        @"deeppink":@"#ff1493",
        @"deepskyblue":@"#00bfff",
        @"dimgray":@"#696969",
        @"dodgerblue":@"#1e90ff",
        @"firebrick":@"#b22222",
        @"floralwhite":@"#fffaf0",
        @"forestgreen":@"#228b22",
        @"gainsboro":@"#dcdcdc",
        @"ghostwhite":@"#f8f8ff",
        @"gold":@"#ffd700",
        @"goldenrod":@"#daa520",
        @"gray":@"#808080",
        @"greenyellow":@"#adff2f",
        @"honeydew":@"#f0fff0",
        @"hotpink":@"#ff69b4",
        @"indianred":@"#cd5c5c",
        @"indigo":@"#4b0082",
        @"ivory":@"#fffff0",
        @"khaki":@"#f0e68c",
        @"lavender":@"#e6e6fa",
        @"lavenderblush":@"#fff0f5",
        @"lawngreen":@"#7cfc00",
        @"lemonchiffon":@"#fffacd",
        @"lightblue":@"#add8e6",
        @"lightcoral":@"#f08080",
        @"lightcyan":@"#e0ffff",
        @"lightgoldenrodyellow":@"#fafad2",
        @"lightgreen":@"#90ee90",
        @"lightgrey":@"#d3d3d3",
        @"lightpink":@"#ffb6c1",
        @"lightsalmon":@"#ffa07a",
        @"lightseagreen":@"#20b2aa",
        @"lightskyblue":@"#87cefa",
        @"lightslategray":@"#778899",
        @"lightsteelblue":@"#b0c4de",
        @"lightyellow":@"#ffffe0",
        @"limegreen":@"#32cd32",
        @"linen":@"#faf0e6",
        @"magenta":@"#ff00ff",
        @"mediumblue":@"#0000cd",
        @"mediumorchid":@"#ba55d3",
        @"mediumpurple":@"#9370db",
        @"midnightblue":@"#191970",
        @"mistyrose":@"#ffe4e1",
        @"moccasin":@"#ffe4b5",
        @"oldlace":@"#fdf5e6",
        @"orange":@"#ffa500",
        @"orchid":@"#da70d6",
        @"peachpuff":@"#ffdab9",
        @"peru":@"#cd853f",
        @"pink":@"#ffc0cb",
        @"plum":@"#dda0dd",
        @"purple":@"#800080",
        @"rosybrown":@"#bc8f8f",
        @"royalblue":@"#4169e1",
        @"salmon":@"#fa8072",
        @"sandybrown":@"#f4a460",
        @"seagreen":@"#2e8b57",
        @"sienna":@"#a0522d",
        @"skyblue":@"#87ceeb",
        @"slateblue":@"#6a5acd",
        @"steelblue":@"#4682b4",
        @"tan":@"#d2b48c",
        @"thistle":@"#d8bfd8",
        @"tomato":@"#ff6347",
        @"violet":@"#ee82ee",
        @"wheat":@"#f5deb3",
        @"whitesmoke":@"#f5f5f5",
        @"yellow":@"#ffff00",
        @"yellowgreen":@"#9acd32ff"
    };

    NSString* color = [colorHashMap objectForKey:htmlColor];
    if (color != nil) {
        htmlColor = color;
    
    } else if(![htmlColor hasPrefix:@"#"]) {
        htmlColor = [@"#" stringByAppendingString:htmlColor];
    }
    
    unsigned rgbValue = 0;
    NSScanner *scanner = [NSScanner scannerWithString:htmlColor];
    [scanner setScanLocation:1]; // bypass '#' character
    [scanner scanHexInt:&rgbValue];
    return [UIColor colorWithRed:((rgbValue & 0xFF0000) >> 16)/255.0 green:((rgbValue & 0xFF00) >> 8)/255.0 blue:(rgbValue & 0xFF)/255.0 alpha:1.0];
}

@end