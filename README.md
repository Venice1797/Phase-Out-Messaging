Not ready yet as of 3/17/2026

# Phase Out Messaging: A messaging app for Android
Phase Out Messaging is an Android application designed to help users transition away from SMS, MMS, and RCS while maintaining their existing communication threads.

# The Goal
The primary purpose of this app is to "nudge" users away from legacy, insecure messaging protocols (SMS/MMS/RCS) and toward open-source, privacy-focused alternatives like Signal.
Why This Matters (The Motivation)
As a GrapheneOS user, privacy and security are the top priorities. GrapheneOS offers some of the most robust privacy guards available on mobile today. However, a significant irony exists:
    • The RCS Gap: The default messaging app on GrapheneOS lacks RCS support.
    • The Security Paradox: While RCS is a closed-source protocol owned by a tech monopoly, it is technically more secure than the "cleartext" nature of SMS and MMS.
    • The Dilemma: On GrapheneOS, users are often forced back to the least secure protocols (SMS/MMS) because we cannot (and should not) trust the proprietary implementations of RCS.
# The Solution
Phase Out Messaging acts as a bridge with a purpose. It serves as your SMS/MMS client on GrapheneOS, but with a built-in "nudge" mechanism. It actively encourages your contacts to stop sending insecure messages and invites them to join you on verified, open-source privacy apps—with Signal set as the default recommendation.

# Targeted Platforms:
This app is primariy for users on GrapheseOS, but should work on any Android with minSDK 35+ (Android 15+).
I have only tested this app on GrapheneOS

# Dev Notes:

## Built on:
Fedora KDE 43

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

# License
MIT License

