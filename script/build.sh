#!/bin/bash
set -x
basepath=$(cd `dirname $0`; pwd)
cd $basepath

adb uninstall com.example.demo
adb install -r ../bin/Demo-release.apk
adb shell am start -W -n com.example.demo/.MainActivity