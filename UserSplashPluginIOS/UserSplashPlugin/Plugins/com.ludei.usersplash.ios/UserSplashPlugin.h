#import <Foundation/Foundation.h>
#import <Cordova/CDVPlugin.h>

@interface UserSplashPlugin : CDVPlugin {
    UIImageView* _imageView;
    NSString* splashScaleMode;
    NSString* splashBackgroundColor;
    int splashDelay;
    NSString* splashPadding;
}

@end