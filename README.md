## GNS for DistressNet-NG
This repository contains everything related to GNS for DistressNet-NG.

##### GNS server


##### GNS-service

##### User authentication mechanism through Certificates and CA:


## Folder path associativity
- GNS-client-code-with-android: This one is created with the GNS source code 1.19.15 following the similar approach that Karthik did couple of years back with old GNS source code. For details, see the last three commits in the UMASS GNS github code in the branch Android-lib.
- GNSServiceAndroid: This is the code for GNS Service-Android
- GNS-server-deployable: This one is used for deploying GNS servers
- GNS-server-source: This one contains the source code for GNS. I took the version 1.19.15 from UMASS github and then debugged for DNS.
- GNSServiceDesktop: This one contains the code Desktop version of GNS Service for Distressnet-NG
- gns_source_code_android: I created this one to compile the Gigapaxos and GNS side by side. This way, I have some control over which jar libraries to be included in the final jar.



### GNS server running at Field server:

#### Building GNS server from source
Go into `GNS-server-source` folder and run command `ant`. This will build the required jar and also an tarball for deployabale server build with minimal files.

#### Executing GNS servers

1. Prerequisit: `Java 8` and `MongodB`.
  ```
  sudo apt-get install openjdk-8-jdk
  sudo apt-get install mongodb
  sudo service mongodb start
  ```

1. Download the deplyable binary [`GNS-server-deployable.tar.gz`](GNS-server-deployable.tar.gz). It is tested on Ubuntu 16.04. Extract the files and then go into the created directory.

1. Configure the GNS-server properties in `conf/gigapaxos.properties` file. Modify the IP address in two fields: `active.GNSApp1` and `reconfigurator.reconfigurator1` according to your host configuration. Do not change the port numbers.  This IP must be static and the DNS servier field on all DNS queries should point to this IP.

1. The GNS server uses certificate based authentication mechanism to register clients. Store the certificate of the CA in the GNS truststore.
  - First check for the stored certificates in `conf/trustStore.jks` and `keyStore.jks`. The defult pssword for these two files are `qwerty` (which can be changed in the GNS server startup script).
  - The first command lists all the aliases in a keystore. Repeat the next command for every alias to delete that entry. Do these steps for both `keyStore` and `trustStore`:
    ```
    keytool -list -v -keystore [key_store_name.jks]
    keytool -delete -alias [alias_to_remove] -keystore [key_store_name.jks]
    ```
	- Run the following command to import the root CA's certificate in the keystore. For our architecture, `[root_CA_certificate.pem]` is `root-ca.pem` and `[alias_of_the_CA]` is `lenss_tak_cert`.
	```
	keytool -keystore conf/trustStore.jks -import -file [root_CA_certificate.pem] -alias [alias_of_the_CA]
	```

1. Now, configure the logging properties in `conf/log4j.properties` and `conf/logging.properties`. known Issue: The `log4j.properties` does not work well.

1. Run the GNS server. It will run the DNS server at the host machine as well. GNS server can run without sudo permission but running DNS alongwith GNS requires sudo permission.
	```
	sudo ./bin/gpServer.sh start all
	```
	If you are getting an error for MongoDB such as *"RuntimeException: Mongo DB initialization failed likely because a mongo DB server is not listening at the expected port; exiting"*, then a workaround is to let the GNS server use in-momory database instead of MongoDB. Just uncomment `IN_MEMORY_DB=true` line in the gnsserver properties file.

1. Stop GNS server using the command:
    ```
    sudo ./bin/gpServer.sh stop all
    ```


##### Check if GNS using CLI:
GNS provides a command line interface for managging or viewing GUID records.
- run `./bin/cli.sh`
- Use command `account_create [accountname] [password]`.
- Useful commands ``


## GNS service - Linux / Desktop
This Java application acts as the middle-ware between C++ based RSock daemon and the Java based GNS server. It also provide interface to other application that want to communicate with GNS server.

It uses a certification authority to certify the users. The app uses p12 certificates to certify a user. The user needs to locate the p12 certificate stored in the device and provide the corresponding password.

#### Building GNS-service from source:
The [code](GNSServiceDesktop) is written in Java and and can easily be imported into Eclipse editor. For building the required jar and deployable tar, use the `build.xml` script. Simply run `ant`.

#### Executing GNS-service
1. Get the `GnsServiceDesktop.tar.gz` from github. and extract it.   
2. Configure the gnsservice.properties file for authentication. Get the p12 certificate that this machine is going to use and write it at `p12file=`. Also, provide the password for this p12 file.  
3. Configure the logging in `log4j.properties` and `logging.properties`. The main logging for GNSService is done through log4j.
4. to run gns-service, execute,
  ```
  ./gns_service.sh start
  ```
5. to stop gns-service, execute,
  ```
  ./gns_service.sh stop
  ```

## GNS service - android
This is the Android version of the GNS-service. The basic The app has two major component:

- GNS activity which is to be started by an user. This page connects with the local GNS server at the starting and creates the GUID.
- GNS service which is initiated by the activity and runs as a service in the background to provide response server for the RSock daemon.

#### Preparing deployable from source code:
The [code](GNSServiceAndroid/) is prepared using Android studio 3.3.1.  Use Android studio to build the deployable apk.


#### Executing GNS service - Android
- Download the [GNS-service-debug.apk](GNSServiceAndroid/deployable/GNS-service-debug.apk) for GNS service
- Connect the phone(s) to a desktop, swipe down the screen from top to bottom, and allow USB file transfer. 
- open a terminal and do `adb devices`. If you have multiple devices connected it will show their device id. If some of the 
  devices says `Permission Denied`, then you did not give usb for file transfer properly. follow the last step.
- Install the APK using `adb -s [device id] install GNS-service-debug.apk`
- copy the client p12 certificate that the phone will use to the phone's internal memory by doing 
  `adb -s [device id] push [certificate-file-name.p12] [location_to_store_certificate]`
- Open the GNSService app and then upon asked, choose the correct certificate. You might have to enable `show internal storage`.
- Verify that the username is accurate.
- Put the password. and press log in



#### Some useful stuffs:
- git commands that I used:
  - I commited before deleting any files using commands: `git init`, `git add .` and `git commit -m "<some message>"`. Before running `git init`, I removed all `gitignore` rules and `.git` folders.
  - `git clean -nd` to see which files / directories are created after the last commit.
  - run `sudo git clean -fd` to delet the files. sudo is required because some files were created when I ran the gns server in sudo mode.

  - To recover some deleted files, or if I want to go back to last commit, I used the command: `git reset --hard HEAD`

- OpenSSL commands:
  - Guide: https://www.sslshopper.com/article-most-common-java-keytool-keystore-commands.html
  - To view the certificates in a keystore:
  ```
  keytool -list -v -keystore [enter keystore name] -storepass [enter keystore password]
  ```
   The default password is qwerty
  - To add a trusted CA’s certificate:
  ```
  keytool -keystore trustStore.jks -storepass [password for the truststore qwerty] -import -file [root-ca.pem] -alias  [lenss_tak_cert]
  ```
