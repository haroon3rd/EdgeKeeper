package edu.tamu.cse.lenss.edgeKeeper.zk;

import java.util.ArrayList;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.CuratorFrameworkFactory.Builder;
import org.apache.curator.framework.api.CreateBuilder;
import org.apache.curator.retry.RetryUntilElapsed;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.server.auth.DigestAuthenticationProvider;

public class EKEphemerical {
	
	

	public static void main(String[] args) {
		
        RetryUntilElapsed retry = new RetryUntilElapsed(10000, 2000);

        // The simplest way to getEdgeStatus a CuratorFramework instance. This will use default values.
        // The only required arguments are the connection string and the retry policy
        //return CuratorFrameworkFactory.newClient(connectionString, retryPolicy);
        //client = CuratorFrameworkFactory.newClient(zkServerString, retry);
        
        
        
        CuratorFramework client = CuratorFrameworkFactory.newClient("172.30.30.44:2181", retry);
        
        
        client.start();
        
        
        DigestAuthenticationProvider digp = new DigestAuthenticationProvider();
        
        
        
        
        
        
        try {
        	
        	CreateBuilder builder = client.create();
            ArrayList<ACL> aList = new ArrayList<ACL>();
            
            String authString = "suman2:password";
            String path = "/sumanACL9";
        	
//        	String digest = DigestAuthenticationProvider.generateDigest(authString);
//			System.out.println("Digest=>" +digest);
//
//        	//aList.add(new ACL(ZooDefs.Perms.ALL, new Id("digets",  "Suman:0JScWIJiFFZ2UlZlPiVnLX2aKe0=")));
//            aList.add(new ACL(ZooDefs.Perms.ALL, new Id("digest",	digest)));
//        	aList.add(new ACL(ZooDefs.Perms.READ, Ids.ANYONE_ID_UNSAFE));
//            
//            
//        	
//            System.out.println(aList.toString());
//
//			builder.withACL(aList).forPath(path);
			
			
			
			Builder x = CuratorFrameworkFactory.builder()
	        .connectString("172.30.30.44:2181")
	        .retryPolicy(retry)
	        .authorization("digest",	authString.getBytes());
			
			CuratorFramework y = x.build();
			y.start();
			y.setData().forPath(path, "new".getBytes());
			
			
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        
        
        
        
        
//
//        PersistentNode eph = new PersistentNode(client, CreateMode.EPHEMERAL, false, "/gg/suman_test_node3", "".getBytes());
//        
//        eph.start();
//        
//        
//        Scanner userInput = new Scanner(System.in);
        //userInput.nextLine();
//        
//        try {
//			eph.close();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
        client.close();
		

	}

}
