#
# To learn more about a Podspec see http://guides.cocoapods.org/syntax/podspec.html.
# Run `pod lib lint tinkoff_acquiring.podspec` to validate before publishing.
#
Pod::Spec.new do |s|
  s.name             = 'tinkoff_acquiring'
  s.version          = '0.0.1'
  s.summary          = 'horum implementation of tinkoff Acquiring SDK to flutter'
  s.description      = <<-DESC
horum implementation of tinkoff Acquiring SDK to flutter
                       DESC
  s.homepage         = 'http://example.com'
  s.license          = { :type => 'BSD' }
  s.author           = { 'Horum.co' => 'contact@horum.co' }
  s.source           = { :git => 'https://github.com/horum/tinkoff_acquiring.git', :tag => 'v0.0.1' }
  s.source_files = 'Classes/**/*'
  s.dependency 'Flutter'
  s.dependency 'TinkoffASDKCore', '2.2.2'
  s.dependency 'TinkoffASDKUI', '2.2.2'
  s.platform = :ios, '11.0'

  # Flutter.framework does not contain a i386 slice.
  s.pod_target_xcconfig = { 'DEFINES_MODULE' => 'YES', 'EXCLUDED_ARCHS[sdk=iphonesimulator*]' => 'i386' }
  s.swift_version = '5.0'
end
