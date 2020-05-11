package edu.tamu.cse.lenss.edgeKeeper.topology;

import java.util.HashSet;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class TopoParser {
	private static final Gson gson = new GsonBuilder()
			.serializeNulls()
			.serializeSpecialFloatingPointValues() 
			.create();
	
	public static String exportGraph(TopoGraph graph) {
		
		TopoGraph g = TopoGraph.getGraph(graph.getByteArray());
		
		PseudoGraph psg = new PseudoGraph();
		psg.ownNodeGuid = g.ownNode.guid;
		psg.nodeSet = new HashSet<PseudoNode>();
		
		for (TopoNode v:g.vertexSet()) {
			PseudoNode pn = new PseudoNode();
			pn.guid = v.guid;
			pn.type = v.type;
			pn.ipMaps = v.ipMaps;
			psg.nodeSet.add(pn);
		}
		
		psg.linkSet = new HashSet<PseudoLink>();
		for(TopoLink e: g.edgeSet()) {
			PseudoLink pl = new PseudoLink();
			pl.ipPair = e.ipPair;
			pl.rtt = e.rtt;
			pl.etx = e.getEtx();
			pl.src = g.getEdgeSource(e).guid;
			pl.dst = g.getEdgeTarget(e).guid;
			
			psg.linkSet.add(pl);
			//TopoHandler.logger.log(Level.ALL, "edge="+e);
		}
		//TopoHandler.logger.log(Level.ALL, "Graph JSON = "+gson.toJson(psg));
		return gson.toJson(psg);
	}

	static public TopoGraph importGraph(String jsonString) {
		PseudoGraph psg = gson.fromJson(jsonString, PseudoGraph.class);
		
		TopoNode ownNode = null;
		
		Set<TopoNode> vertexSet = new HashSet<TopoNode>();
		
		if(psg.nodeSet!=null && !psg.nodeSet.isEmpty()) {
			for(PseudoNode pn: psg.nodeSet) {
				TopoNode v = new TopoNode(pn.guid, pn.type);
				v.ipMaps = pn.ipMaps;
				
				vertexSet.add(v);
				
				if(v.guid.equals(psg.ownNodeGuid))
					ownNode = v;
			}
		}
		
		TopoGraph g = new TopoGraph(TopoLink.class, ownNode);
		
		for(TopoNode v: vertexSet)
			g.addVertex(v);
		
		for (PseudoLink pl: psg.linkSet) {
			TopoNode sv = g.getVertexByGuid(pl.src);
			TopoNode tv = g.getVertexByGuid(pl.dst);
			TopoLink e = g.addEdge(sv, tv);
			g.setEdgeWeight(e, pl.etx);
			e.rtt=pl.rtt;
			e.ipPair=pl.ipPair;
		}

		return g;
	}

}
