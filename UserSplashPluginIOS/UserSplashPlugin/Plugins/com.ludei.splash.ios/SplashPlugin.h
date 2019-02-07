#import <Foundation/Foundation.h>
#import <Cordova/CDVPlugin.h>

@interface SplashPlugin : CDVPlugin {
    UIImageView* _imageView;
    NSString* splashScaleMode;
    NSString* splashBackgroundColor;
    int splashDelay;
    NSString* splashPadding;
}

@end