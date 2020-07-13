## EdgeKeeper - android
This is the Android version of the EdgeKeeper. The app has two major components:

- Activity which is to be started by a user (by clicking on the app)
- Service which is initiated by the activity and runs as a service in the background.

### 1. Preparing deployable from source code:
 - The code is prepared using Android studio 3.3.1.  Use Android studio to build the deployable apk.
 - The built debug APK, `EdgeKeeper-debug.apk` is stored at `app/build/outputs/apk/debug`

### 2. Executing GNS service - Android
- Download the APK. The prebuilt version is stored inside [deployable](deployable) directory.
- Connect the phone(s) to a desktop, swipe down the screen from top to bottom, and allow USB file transfer.
- open a terminal and do `adb devices`. If you have multiple devices connected it will show their device id. If some of the
  devices says `Permission Denied`, then you did not give usb for file transfer properly. follow the last step.
- Install the APK using `adb -s [device id] install EdgeKeeper-debug.apk`
- copy the client p12 certificate that the phone will use to the phone's internal memory (preferably to distressnet) by doing
  `adb -s [device id] push [certificate-file-name.p12] [location_to_store_certificate]`
- Open the EdgeKeeper app and then go to Menu-> Setting. In the setting window choose the correct parameters:
    - choose the correct certificate. You might have to enable `show internal storage`.
    - Verify that the username is accurate.
    - Put the password.
    - GNS Server address (usually running in the cloud).
    - EdgeKeeper master IP: Put `auto` if the master can be found through DNS nameresolve.
    - Change other parameters if needed
-
