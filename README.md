Not ready as of 3/16/2026

Updated code was uploaded on 3/16/2026


## Built on:
Build on Fedora KDE 43

## Tested on:
Android pixel 8 running GrapheneOS Build 2026030701 (Android 16)

## Setup to clone and build:

### Install git
```
sudo dnf install git-all
```

### Install Android tools 
```
sudo dnf install android-tools
```

### Install Android SDK 
```
sudo dnf install java-21-openjdk-devel

mkdir -p ~/Android/sdk/cmdline-tools
cd ~/Android/sdk/cmdline-tools
wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip commandlinetools-linux-*.zip
mv cmdline-tools latest 

export ANDROID_HOME=$HOME/Android/sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools
# You may want to save the above two lines in your ~/.bashrc for future use

sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
```

### Install Java libs:
```
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 17.0.10-tem
sdk list java
sdk  current java
```

### Clone repo:
```
git clone https://github.com/Venice1797/Phase-Out-Messaging.git
cd Phase-Out-Messaging
git submodule update --init --recursive
```

### Build
```
cd Phase-Out-Messaging
./gradlew clean
./gradlew assembleDebug

```

### Install on a physical device
Enable debug mode on an Android device, enable USB debugging

Connect Android device using a USB cable (high quality cable)

Verify device is connected using adb, you should see your device listed as an ID

Install:
```
./gradlew installDebug
```

