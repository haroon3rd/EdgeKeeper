package edu.tamu.cse.lenss.edgeKeeper.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

import org.apache.curator.framework.state.ConnectionState;
import org.apache.zookeeper.server.quorum.QuorumPeer.ServerState;
import org.json.JSONException;
import org.json.JSONObject;
import org.xbill.DNS.ResolverConfig;

import edu.tamu.cse.lenss.edgeKeeper.server.GNSClientHandler;
import edu.tamu.cse.lenss.edgeKeeper.server.RequestTranslator;

public class EKUtilsDesktop extends EKUtils {

	public EKUtilsDesktop(EKProperties prop) {
		super(prop);
		// TODO Auto-generated constructor stub
	}

	// this function fetches this devices status information
	@Override
	public JSONObject getDeviceStatus() {

		JSONObject jo = null;

		try {
			// getEdgeStatus cpu available memory in bytes
			long availmemory = LINUXgetAvailJVMMemoryInBytes();

			// getEdgeStatus battery percentage
			int batteryperc = LINUXgetBatteryPercentage();

			// getEdgeStatus number of cpu cores
			int numofcores = LINUXgetNumberOfCores();

			// getEdgeStatus size of free external memory
			long freeexternalmemory = LINUXgetFreeExternalMemoryInBytes();

			// create a new json object for device status report
			jo = new JSONObject();

			// put entries into it
			jo.put(RequestTranslator.requestField, RequestTranslator.putDeviceStatus);
			jo.put("DEVICE", "DESKTOP");
			jo.put(availableJVMmemory, availmemory);
			jo.put(batteryPercentage, batteryperc);
			jo.put(numberOfCores, numofcores);
			jo.put(freeExternalMemory, freeexternalmemory);

		} catch (JSONException e) {
			e.printStackTrace();
		}

		// return
		return jo;
	}

	// gets available JVM memory in bytes in linux desktop
	private long LINUXgetAvailJVMMemoryInBytes() {
		return Runtime.getRuntime().freeMemory();
	}

	// gets battery percentage in linux desktop
	private int LINUXgetBatteryPercentage() {
		return 100;
	}

	// returns number of cores in a linux desktop
	private int LINUXgetNumberOfCores() {
		return Runtime.getRuntime().availableProcessors();
	}

	// getEdgeStatus free space in disk in linux desktop
	private long LINUXgetFreeExternalMemoryInBytes() {
		File f = new File("/");
		return f.getFreeSpace();
	}

	String gnsState;
	String zkClientState;
	String zkServerState;

	@Override
	public void onStart() {
		showNotification("EdgeKeeper starting");
	}

	@Override
	public void onStop() {
		showNotification("EdgeKeeper stopped");
	}

	public void onGNSStateChnage(GNSClientHandler.ConnectionState connectionState) {
		gnsState = connectionState.toString();
		putNotice();
	}

	@Override
	public void onCuratorStateChange(ConnectionState newState) {
		zkClientState = newState.toString();
		putNotice();
	}

	@Override
	public void onZKServerStateChange(ServerState newServStatus) {
		zkServerState = newServStatus.toString();
		putNotice();
	}

	@Override
	public void onError(String string) {
		showNotification(string);

	}

	private void showNotification(String str) {
		BufferedWriter writer;
		try {
			writer = new BufferedWriter(new FileWriter(EKConstants.STATUS_FILE_NAME));
			writer.write(str);
			writer.close();
		} catch (IOException e) {
			logger.error("Problem in writing status to file " + EKConstants.STATUS_FILE_NAME);
		}
	}

	void putNotice() {
		showNotification("GNS:" + gnsState + ", ZKClient:" + zkClientState + ", ZKServer:" + zkServerState);
	}

	/**
	 * This function gets the IPs of DNS servers. First thing to do is getEdgeStatus the GNS
	 * server address. GNS server may be running in the current machine in which
	 * case, the dnsmasq is running in the current machine. Thus, we need to check
	 * if the dnsmasq is running in the current machine. If the dnsmasq is running
	 * in the current machine then the DNS server query will return only one address
	 * 127.0.1.1 which can not be used as GNS server address. If it is not the case
	 * then we shall use the DNS server IPs as the GNS server IPs
	 * 
	 * @return
	 * @throws SocketException
	 */
	public Set<String> getDnsAddress() {
		Set<String> gnsServerList = new HashSet<String>();
		String[] dnsServerAddr = ResolverConfig.getCurrentConfig().servers();
		if (dnsServerAddr == null) {
			logger.error("[GNS] Could not retrieve the DNS information");
			return gnsServerList;
		}
		for (String addrStr : dnsServerAddr) {
			try {
				InetAddress addr = InetAddress.getByName(addrStr);
				if (!(addr.isLoopbackAddress() || addr.isAnyLocalAddress() || addr.isLinkLocalAddress()))
					gnsServerList.add(addr.getHostAddress());
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		}
		// logger.debug("[GNS] Obtained a list of DNS as:"+gnsServerList.toString());
		return gnsServerList;
	}

	// TO Do: Implement this method
	public Set<String> getDefaultGateway() {
		return new HashSet<String>();
	}

	@Override
	public void bindSocketToInterface(DatagramSocket sock) throws IOException {
		// TODO Auto-generated method stub
		
	}

}
