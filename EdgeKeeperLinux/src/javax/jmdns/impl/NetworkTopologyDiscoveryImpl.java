/**
 *
 */
package javax.jmdns.impl;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.*;

import javax.jmdns.NetworkTopologyDiscovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements NetworkTopologyDiscovery.
 *
 * @author Pierre Frisch
 */
public class NetworkTopologyDiscoveryImpl implements NetworkTopologyDiscovery {
    private final static Logger logger = LoggerFactory.getLogger(NetworkTopologyDiscoveryImpl.class.getName());

    /**
     *
     */
    public NetworkTopologyDiscoveryImpl() {
        super();
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.JmmDNS.NetworkTopologyDiscovery#getInetAddresses()
     */
    @Override
    public InetAddress[] getInetAddresses() {
        //String allIPs = "";
        Set<InetAddress> result = new HashSet<InetAddress>();
        try {
            for (Enumeration<NetworkInterface> nifs = NetworkInterface.getNetworkInterfaces(); nifs.hasMoreElements();) {
                NetworkInterface nif = nifs.nextElement();
                for (Enumeration<InetAddress> iaenum = nif.getInetAddresses(); iaenum.hasMoreElements();) {
                    InetAddress interfaceAddress = iaenum.nextElement();
                    //allIPs += interfaceAddress.getHostAddress() + " ";
                    if (logger.isTraceEnabled()) {
                        logger.trace("Found NetworkInterface/InetAddress: " + nif + " -- " + interfaceAddress);
                    }
                    if (useInetAddress(nif, interfaceAddress)) {
                        result.add(interfaceAddress);
                    }
                }
            }
        } catch (SocketException se) {
            logger.warn("Error while fetching network interfaces addresses: " + se);
        }

        //System.out.println("ALLIPS: " + allIPs);
        return result.toArray(new InetAddress[result.size()]);
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.NetworkTopologyDiscovery#lockInetAddress(java.net.InetAddress)
     */
    @Override
    public void lockInetAddress(InetAddress interfaceAddress) {
        // Default implementation does nothing.
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.NetworkTopologyDiscovery#unlockInetAddress(java.net.InetAddress)
     */
    @Override
    public void unlockInetAddress(InetAddress interfaceAddress) {
        // Default implementation does nothing.
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.JmmDNS.NetworkTopologyDiscovery#useInetAddress(java.net.NetworkInterface, java.net.InetAddress)
     */
    @Override
    public boolean useInetAddress(NetworkInterface networkInterface, InetAddress interfaceAddress) {
        try {
            if (!networkInterface.isUp()) {
                return false;
            }

            if (!networkInterface.supportsMulticast()) {
                return false;
            }

            if (networkInterface.isLoopback()) {
                return false;
            }

            //disregard LTE ip
            if (networkInterface.getName().startsWith("rmnet") || networkInterface.getName().startsWith("pgwtun")) {
                return false;
            }

            //disregard ipv6 address with semi-colon
            if(interfaceAddress.getHostAddress().contains(":")){
                return false;
            }

            return true;
        } catch (Exception exception) {
            return false;
        }
    }



}
