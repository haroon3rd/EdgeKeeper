## EdgeKeeper - Linux / Desktop
This Java application acts as the middleware between Distressent applications and the Java based GNS server. It also provide interface to other application that want to communicate with GNS server.

It uses a certification authority to certify the users. The app uses p12 certificates to certify a user. The user needs to locate the p12 certificate stored in the device and provide the corresponding password.

### 1. Building EdgeKeeper from source:
- The code is written in Java and and can easily be imported into Eclipse editor. For building the required jar and deployable tar, use the `build.xml` script. Simply run `ant` command.
  ```
  radu@cse-lenss-dt2:~/EdgeKeeper/EdgeKeeperLinux$ ant
  Buildfile: /home/radu/EdgeKeeper/EdgeKeeperLinux/build.xml

  clean:
     [delete] Deleting directory /home/radu/EdgeKeeper/EdgeKeeperLinux/bin
     [delete] Deleting directory /home/radu/EdgeKeeper/EdgeKeeperLinux/jar

  compile:
      [mkdir] Created dir: /home/radu/EdgeKeeper/EdgeKeeperLinux/bin
      [mkdir] Created dir: /home/radu/EdgeKeeper/EdgeKeeperLinux/jar
      [javac] /home/radu/EdgeKeeper/EdgeKeeperLinux/build.xml:40: warning: 'includeantruntime' was not set, defaulting to build.sysclasspath=last; set to false for repeatable builds
      [javac] Compiling 62 source files to /home/radu/EdgeKeeper/EdgeKeeperLinux/bin
      [javac] Note: /home/radu/EdgeKeeper/EdgeKeeperLinux/src/edu/tamu/cse/lenss/edgeKeeper/server/EKMainLinux.java uses or overrides a deprecated API.
      [javac] Note: Recompile with -Xlint:deprecation for details.
      [javac] Note: Some input files use unchecked or unsafe operations.
      [javac] Note: Recompile with -Xlint:unchecked for details.

  jar:
        [jar] Building jar: /home/radu/EdgeKeeper/EdgeKeeperLinux/jar/dependencies.jar
        [jar] Building jar: /home/radu/EdgeKeeper/EdgeKeeperLinux/jar/EdgeKeeper-Linux.jar
        [jar] Building jar: /home/radu/EdgeKeeper/EdgeKeeperLinux/jar/EdgeKeeper-cli.jar
     [delete] Deleting: /home/radu/EdgeKeeper/EdgeKeeperLinux/jar/dependencies.jar
        [jar] Building jar: /home/radu/EdgeKeeper/EdgeKeeperLinux/jar/EdgeKeeper-client.jar
        [jar] Building jar: /home/radu/EdgeKeeper/EdgeKeeperLinux/jar/EdgeKeeper-android.jar
       [copy] Copying 1 file to /home/radu/EdgeKeeper/EdgeKeeperAndroid/app/libs
       [copy] Copying 1 file to /home/radu/EdgeKeeper/EdgeKeeperCommandLine/libs

  tar:
     [delete] Deleting: /home/radu/EdgeKeeper/EdgeKeeperLinux/deployable/EdgeKeeper-Linux.tar.gz
        [tar] Building tar: /home/radu/EdgeKeeper/EdgeKeeperLinux/deployable/EdgeKeeper-Linux.tar.gz

  build:

  BUILD SUCCESSFUL
  Total time: 22 seconds
  ```

- The ant command compile the source code and creates multiple jar files to be used by other project. It also creates a tar file as a deployable for Linux. The tar file is stored under `deployable` directory.

### 2. Executing EdgeKeeper
1. Extract [EdgeKeeper-Linux.tar.gz](deployable/EdgeKeeper-Linux.tar.gz) which was obtained during the compilation step, mentioned above.   
1. Configure the `ek.properties` file for authentication.
  - Get the p12 certificate that this machine is going to use and write it at `P12_FILE_PATH=[certificatePath.p12]`. Also, provide the password for this p12 file.  
  - If this node will act as master, the put `ENABLE_MASTER = true`. Otherwise put false.
  - Specify the number of replica. `NUMBER_OF_REPLICA = 1`
  - If this node is not acting as EdgeKeeper master, then put the IP address of the master. If the master can be found automatically through DNS put `EDGE_KEEPER_MASTER = auto`.
  - If ther EdgeKeeper masters can be reached through mesh link, put those master IPs: `NEIGHBOR_IPS = 192.168.2.131,192.168.1.131`
  - Change other settings only if those are necessary.
1. Configure the logging in `log4j.properties` and `logging.properties`. The main logging for EdgeKeeper is done through log4j.
1. To run EdgeKeeper, execute,
  ```
  ./edgekeeper_startup.sh.sh start
  ```
1. to stop EdgeKeeper, execute,
  ```
  ./edgekeeper_startup.sh stop
  ```

### 3. EdgeHealth status viewing
EdgeKeeper provides a comprehensive status of the edge using HTML. Open a browser and go to the website `[IP address]:22225`

### 4. API DOC:
- The API Java doc can be found [here](doc/index.html).
- To prepare the Javadoc, run `ant doc` command in EdgeKeeperLinux folder.

### 5. Useful comments:

1. EdgeKeeper master runs a DNS server by default. On some Ubuntu versions, the OS runs `systemd-resolved` which occupies the DNS port of 53. Here is the procedure to Stop `systemd-resolved` in Ubuntu for starting DNS server
  - If there exist a file `/etc/NetworkManager/NetworkManager.conf:`, then  put `dns=default` in the `[main]` section of this file
  - Now, disable and stop the systemd-resolved:
  ```
  sudo systemctl disable systemd-resolved
  sudo systemctl stop systemd-resolved
  sudo rm /etc/resolv.conf
  sudo systemctl restart NetworkManager
  ```
  - When you destry your DNS and want to allow your desktop to work with Google DNS server temporarily.
  ```
  nameserver 8.8.8.8
  ubuntu@ip-172-31-44-71:~$ vi /etc/resolv.conf
  ubuntu@ip-172-31-44-71:~$ sudo service network-manager restart
  ubuntu@ip-172-31-44-71:~$ nslookup google.com
  ```
