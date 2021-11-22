#import "TinkoffAcquiringPlugin.h"
#if __has_include(<tinkoff_acquiring/tinkoff_acquiring-Swift.h>)
#import <tinkoff_acquiring/tinkoff_acquiring-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "tinkoff_acquiring-Swift.h"
#endif

@implementation TinkoffAcquiringPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftTinkoffAcquiringPlugin registerWithRegistrar:registrar];
}
@end
