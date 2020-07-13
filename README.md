## EdgeKeeper for DistressNet-NG

- EdgeKeeperAndroid: EdgeKeeper source code for Android. Most of the EdgeKeepr functions are inherited through the EdgeKeeper-Linux jar created after compiling the EdgeKeeperLinux project.
- EdgeKeeperCommandLine : EdgeKeeper command line interpretor that use EdgeKeeper client libraries.  
- EdgeKeeperLinux: The main EdgeKeepr code. It can produce the executable for linux as well as jar files for other projects.
- GNS-server-deployable.tar.gz : This one is used for deploying GNS servers in the cloud.
- GNS-server-source : This one contains the source code for GNS. I took the version 1.19.15 from UMASS github and then debugged for DNS.
- gns_source_code_android: This one is created with the GNS source code 1.19.15 following the similar approach that Karthik (UMASS) did couple of years back with old GNS source code. For details, see the last three commits in the UMASS GNS github code in the branch Android-lib.


### 1. Building GNS server from source
Go into `GNS-server-source` folder and run command `ant`. This will build the required jar and also an tarball for deployabale server build with minimal files.

#### Executing GNS servers
1. Prerequisit: `Java 8` and `MongodB`.
  ```
  sudo apt-get update -y
  sudo apt-get install openjdk-8-jdk mongodb -y
  sudo service mongodb start
  ```

1. Make sure that MongoDB is running.
  ```
  radu@cse-lenss-dt2:~/EdgeKeeper/EdgeKeeperLinux$ sudo service mongodb status
  ● mongodb.service - An object/document-oriented database
     Loaded: loaded (/lib/systemd/system/mongodb.service; enabled; vendor preset: enabled)
     Active: active (running) since Wed 2020-05-27 14:36:39 CDT; 1 months 16 days ago
       Docs: man:mongod(1)
   Main PID: 1000 (mongod)
     CGroup: /system.slice/mongodb.service
             └─1000 /usr/bin/mongod --config /etc/mongodb.conf

  May 27 14:36:39 cse-lenss-dt2 systemd[1]: Started An object/document-oriented database.
  May 27 14:36:39 cse-lenss-dt2 mongod[1000]: warning: bind_ip of 0.0.0.0 is unnecessary; listens on all ips by default
  ```

1. Download the deployable binary [`GNS-server-deployable.tar.gz`](GNS-server-deployable.tar.gz). It is tested on Ubuntu 16.04. Extract the files and then go into the created directory.

1. Configure the GNS-server properties in `conf/gigapaxos.properties` file. Modify the IP address in two fields: `active.GNSApp1` and `reconfigurator.reconfigurator1` according to your host configuration. Do not change the port numbers.  This IP must be static and the DNS servier field on all DNS queries should point to this IP. If running on cloud, use the public IP.


1. The GNS server uses certificate based authentication mechanism to register clients. Store the certificate of the CA in the GNS trustStore.
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
	sudo .gns-server_startup.sh start
	```
1. Stop GNS server using the command:
    ```
    sudo .gns-server_startup.sh stop
    ```

#### Special configuration for running GNS server in cloud:
 If you are running the server in cloud, please configure the incoming traffic port filters.
  - GNS server uses ports: 24403, 24503, 24603, 2178, 2278, 2378
  - EdgeKeeper uses ports: 22220-22229

##### Check if GNS using CLI:
GNS provides a command line interface for managging or viewing GUID records.
- run `./bin/cli.sh`
- Use command `account_create [accountname] [password]`.


## 2. PKCS12 Certificate Generation:

##### Generating Root certificate:
  The root certificate is to be stored at the GNS server's trust store.
  ```
  radu@cse-lenss-dt2:~/Desktop/certs$ ./makeRootCa.sh
  Please give a name for your CA (no spaces).  It should be unique.  If you don't
  enter anything, or try something under 5 characters, I will make one for you
  lenss_root
  Making a CA for  /C=US/O=LENSS/CN=lenss_root
  Generating a 2048 bit RSA private key
  .............................+++
  ........+++
  writing new private key to 'ca-do-not-share.key'
  -----
  Certificate was added to keystore
  Using configuration from ../config.cfg
  ```

##### Generating client Certificates:
  Client certificates are used for authentication for all the client devices.
  ```
  radu@cse-lenss-dt2:~/Desktop/certs$ ./makeCert.sh client sb1.distressnet.org
  Making a client cert for  /C=US/O=LENSS/CN=sb1.distressnet.org
  Generating a 2048 bit RSA private key


  writing new private key to 'sb1.distressnet.org.key'
  Signature ok
  subject=/C=US/O=LENSS/CN=sb1.distressnet.org
  Getting CA Private Key
  Importing keystore sb1.distressnet.org.p12 to sb1.distressnet.org.jks...

  Warning:
  The JKS keystore uses a proprietary format. It is recommended to migrate to PKCS12
  which is an industry standard format using "keytool -importkeystore -srckeystore
  sb1.distressnet.org.jks -destkeystore sb1.distressnet.org.jks -deststoretype pkcs12".

  ```

## 3. Some useful stuff:
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
