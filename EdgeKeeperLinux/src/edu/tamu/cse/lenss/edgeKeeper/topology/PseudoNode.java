package edu.tamu.cse.lenss.edgeKeeper.topology;

import java.util.Map;

import edu.tamu.cse.lenss.edgeKeeper.topology.TopoNode.NodeType;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKUtils.NetworkInterfaceType;

public class PseudoNode {
	NodeType type;
	String guid = null;
	Map<String, NetworkInterfaceType> ipMaps = null;
}
