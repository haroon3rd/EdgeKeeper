package edu.tamu.cse.lenss.edgeKeeper.client;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONException;
import org.json.JSONObject;

import edu.tamu.cse.lenss.edgeKeeper.server.RequestServer;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKConstants;

/**
 * This executioner service is implemented for ANdroid. ANdroid does not allow 
 * in-App network communication.
 * @author sbhunia
 *
 */
class MessageTransferoverSocket implements Callable<JSONObject>{
    JSONObject reqJSON;
    MessageTransferoverSocket(JSONObject reqJSON){
        this.reqJSON = reqJSON;
    }

    /**
     * This function is called when a task is submitted.
     */
    public JSONObject call() throws IOException, JSONException {
    	Socket cSocket  = null;
    	DataInputStream in = null;
    	DataOutputStream out = null;
    	try {
            cSocket = new Socket();
            cSocket.connect(new InetSocketAddress(EKClient.SERVER_IP,RequestServer.JAVA_PORT), EKConstants.ClientConnectTimeout);
            cSocket.setSoTimeout(EKConstants.ClientSoTimeout);
            in = new DataInputStream (new BufferedInputStream(cSocket.getInputStream()));
            out = new DataOutputStream(cSocket.getOutputStream());

            EKClient.logger.trace("Request to server: "+reqJSON.toString());

            out.writeUTF(reqJSON.toString());
            String rep = in.readUTF().trim();
            
            EKClient.logger.trace("Reply from Server: "+rep);
            return new JSONObject(rep);
    	}catch(Exception e) {
    		EKClient.logger.debug("Could not communicate with EdgeKeeper.");
    		throw e;
    	}
        finally {
			try {
				in.close();
			} catch (Exception e) {}
			try {
				out.close();
			} catch (Exception e) {}
			try {
				cSocket.close();
			} catch (Exception e) {}
        }
    }
}
