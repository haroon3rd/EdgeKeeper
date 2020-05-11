This codebase is created for GNS service that is running at a node for
updating IPs and interacting with the GNS server.

The code has 3 sections:

1. server code for GNS service written in Java
2. client code written in Java
3. Client code written in cpp

### Ant script

`build.xml` is the Ant script for building the project. Run `ant` and it will generate all the required jar files.

## API DOC:

The API Java doc can be found [here](doc/index.html).


##### Stopping `systemd-resolved` in Ubuntu for starting DNS server

If there exist a file `/etc/NetworkManager/NetworkManager.conf:`, then  put `dns=default` in the `[main]` section of this file

Now, disable and stop the systemd-resolved:
```
sudo systemctl disable systemd-resolved
sudo systemctl stop systemd-resolved
sudo rm /etc/resolv.conf
sudo systemctl restart NetworkManager
```

When you destry your DNS and want to allow your desktop to work with Google DNS server temporarily. 
```
meserver 8.8.8.8
ubuntu@ip-172-31-44-71:~$ vi /etc/resolv.conf 
ubuntu@ip-172-31-44-71:~$ sudo service network-manager restart
ubuntu@ip-172-31-44-71:~$ nslookup google.com

```
