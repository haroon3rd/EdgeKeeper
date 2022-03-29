package edu.tamu.cse.lenss.edgeKeeper.clusterHealth;




import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.util.ServerRunner;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import edu.tamu.cse.lenss.edgeKeeper.client.EKClient;
import edu.tamu.cse.lenss.edgeKeeper.client.EdgeKeeperAPI;
import edu.tamu.cse.lenss.edgeKeeper.server.EKHandler;
import edu.tamu.cse.lenss.edgeKeeper.server.RequestTranslator;
import edu.tamu.cse.lenss.edgeKeeper.topology.TopoUtils;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKConstants;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKProperties;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKUtils;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKUtilsDesktop;
import edu.tamu.cse.lenss.edgeKeeper.utils.Terminable;

public class HealthWebView extends NanoHTTPD implements Terminable{
	static final Logger logger = Logger.getLogger(HealthWebView.class);
	public static EdgeKeeperAPI mEKClient = new EKClient();
	
	public HealthWebView() {
        super(EKConstants.HEALTH_VIEW_PORT);
    }

    public static void main(String[] args) {
        ServerRunner.run(HealthWebView.class);
    }

    @Override public Response serve(IHTTPSession session) {
        Map<String, List<String>> decodedQueryParameters =
            decodeParameters(session.getQueryParameterString());

        StringBuilder sb = new StringBuilder();
        putHeader(sb);
        
        JSONObject edgeStatus = mEKClient.getEdgeStatus();
        
        try {
        	logger.log(Level.ALL, "Obtained Edge Health= "+edgeStatus.toString( ));
        	
			JSONObject devStatus = edgeStatus.getJSONObject(RequestTranslator.deviceStatus);
			buildTable(sb, "Device Status", devStatus);
		} catch (Exception e) {
			sb.append("<p>Problem in parsing APPStatus "+ EKUtils.getErrorString(e)+"</p>");
		}
        
        try {
			JSONObject appStatus = edgeStatus.getJSONObject(RequestTranslator.appStatus);
			
			Iterator<String> apps = appStatus.keys();
			while(apps.hasNext()) {
				String appName = apps.next();
				JSONObject appStatusJSON = appStatus.getJSONObject(appName);
				buildTable(sb, appName, appStatusJSON);
			}
		} catch (Exception e) {
			sb.append("<p>Problem in parsing App status "+ EKUtils.getErrorString(e)+"</p>");
		}
        

//        sb.append("<p><blockquote><b>URI</b> = ").append(
//            String.valueOf(session.getUri())).append("<br />");
//
//        sb.append("<b>Method</b> = ").append(
//            String.valueOf(session.getMethod())).append("</blockquote></p>");

//        sb.append("<h3>Headers</h3><p><blockquote>").
//            append(toString(session.getHeaders())).append("</blockquote></p>");
//
//        sb.append("<h3>Parms</h3><p><blockquote>").
//            append(toString(session.getParms())).append("</blockquote></p>");
//
//        sb.append("<h3>Parms (multi values?)</h3><p><blockquote>").
//            append(toString(decodedQueryParameters)).append("</blockquote></p>");

//        try {
//            Map<String, String> files = new HashMap<String, String>();
//            session.parseBody(files);
//            sb.append("<h3>Files</h3><p><blockquote>").
//                append(toString(files)).append("</blockquote></p>");
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        putGraph(sb);
        
        putDate(sb);
        sb.append("</body>");
        sb.append("</html>");
        return newFixedLengthResponse(sb.toString());
    }

	private void putHeader(StringBuilder sb) {
        sb.append("<html>");
        sb.append("<head>"
        		+ "<meta http-equiv=\"refresh\" content=\"10\">"
        		+ "<title>Edge health status</title>"
        		+ "<style>\n" + 
        		"table {\n" + 
        		//"  font-family: arial, sans-serif;\n" + 
        		"  border-collapse: collapse;\n" + 
        		"  width: 100%;\n" + 
        		"}\n" + 
        		"\n tr:first-child \n" + 
        		"	{font-weight: bold \n" + 
        		"    }" + 
        		"td, th {\n" + 
        		"  border: 1px solid #dddddd;\n" + 
        		"  text-align: left;\n" + 
        		"  padding: 8px;\n" + 
        		"}\n" + 
        		"\n" + 
        		"tr:nth-child(even) {\n" + 
        		"  background-color: #dddddd;\n" + 
        		"}\n" + 
        		"</style>"
        		+ "</head>");
        sb.append("<body>");
        sb.append("<h3 align=\"center\">Edge Health for Each Device</h3>");
	}

	private void buildTable(StringBuilder sb, String heading, JSONObject wholeRecord) {
		
		//First put the Header
		Set<String> guidList = new HashSet<String>();
		Iterator<String> iterator = wholeRecord.keys();
		while (iterator.hasNext())
		    guidList.add(iterator.next());
		
		if(guidList.isEmpty()) {
			sb.append("<p>GUID list is empty for table "+heading+"</p>");
			return;
		}
		//Start the table 
		sb.append("<h4 align=\"center\">"+heading+"</h4>"+ "<table>\n");
		
		Set<String> columnList = new HashSet<String>();
		for (String guid: guidList) {
		    try {
				//Prepare a list of columns and add GUID at the beginning 
		    	JSONObject guidRec = wholeRecord.getJSONObject( guid );
		    	Iterator<String> ci = guidRec.keys();
		    	while(ci.hasNext())
		    		columnList.add(ci.next());
			} catch (Exception e) {
			}
		}
	    
	    if(columnList.isEmpty()) {
	    	sb.append("<p>Column list is empty for table "+heading+"</p>");
			return;
	    }

		//Now prepare the heading
		String rowString="<tr>";
		rowString+= "<td>" + RequestTranslator.fieldGUID  + "</td>";
		for (String column: columnList)
			rowString+= "<td>" + column  + "</td>";
		rowString+="</tr>";
		sb.append(rowString);
	    
		// Now add one GUID at a time as a separate row
		JSONObject guidRec = null;
		for (String guid: guidList){
			
			rowString ="<tr>"+"<td>" + guid + "</td>";
			
		    try {
		    	guidRec = wholeRecord.getJSONObject( guid);
			} catch (Exception e) {
				rowString+= "<td>" + "NA" + "</td></tr>";
				continue;
			}
		    
			for(String column: columnList)
				try {
					rowString+= "<td>" + guidRec.getString(column ) + "</td>";
				} catch (Exception e) {
					rowString+= "<td>" + "NA" + "</td>";
				}
			rowString+="</tr>";
			sb.append(rowString);
		}
		//end table
		sb.append("</table>");
	}

	private void putDate(StringBuilder sb) {
		SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
		Date date = new Date(System.currentTimeMillis());
		String dateStr = formatter.format(date);
		
		sb.append("<p>Observation time: "+dateStr+"</p>");
	}
	
	void putGraph(StringBuilder sb) {
		if(EKUtils.isAndroid()) {
			sb.append("<p>Android can not show the graph for Java package issue</p>");
			return; //Android do not support manu packages for building the image from graph
		}
		
		String imageString = null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        try {
        	BufferedImage image = TopoUtils.getImage(EKHandler.getTopoHandler().getGraph());
            ImageIO.write(image, "PNG", bos);
            byte[] imageBytes = bos.toByteArray();

            imageString = new String(Base64.getEncoder().encode(imageBytes), "UTF-8");

            bos.close();
            
            sb.append( "<img  src=\"data:image/png;base64," + imageString +"\"/>");
            
        } catch (Exception e) {
        	sb.append("<p>Problem in showing the graph"+ EKUtils.getErrorString(e)+"</p>");
        }
	}


	@Override
	public void terminate() {
		this.stop();
		logger.info("Terminated"+this.getClass().getSimpleName());

	}

	@Override
	public void run() {
		try {
			this.start();
		} catch (IOException e) {
			logger.fatal("Could not start the Webserver ", e);
		}
	}
}