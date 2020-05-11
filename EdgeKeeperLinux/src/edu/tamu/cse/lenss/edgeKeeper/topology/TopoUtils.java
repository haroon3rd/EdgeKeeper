package edu.tamu.cse.lenss.edgeKeeper.topology;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import org.jgrapht.ext.JGraphXAdapter;

//import com.mxgraph.layout.mxCircleLayout;
//import com.mxgraph.layout.mxIGraphLayout;
//import com.mxgraph.layout.mxParallelEdgeLayout;
//import com.mxgraph.util.mxCellRenderer;

import edu.tamu.cse.lenss.edgeKeeper.utils.EKConstants;

public class TopoUtils {
	public static void toPng(TopoGraph topoGraph) {
		try {
			BufferedImage image = getImage(topoGraph);
			File imgFile = new File(EKConstants.graphPath);
			imgFile.createNewFile();
			ImageIO.write(image, "PNG", imgFile);

			// return image;
		} catch (Exception e) {
			TopoHandler.logger.warn("Problem in creating PNG for the graph", e);
		}
	}
	
	public static BufferedImage getImage(TopoGraph g) {
		try {
			JGraphXAdapter<TopoNode, TopoLink> graphAdapter = new JGraphXAdapter<TopoNode, TopoLink>(g);

			com.mxgraph.layout.mxIGraphLayout layout = new com.mxgraph.layout.mxCircleLayout(graphAdapter);
			layout.execute(graphAdapter.getDefaultParent());

			com.mxgraph.layout.mxParallelEdgeLayout layoutP = new com.mxgraph.layout.mxParallelEdgeLayout(graphAdapter);
			layoutP.execute(graphAdapter.getDefaultParent());

			BufferedImage image = com.mxgraph.util.mxCellRenderer.createBufferedImage(graphAdapter, null, 1, Color.WHITE, true, null);

			return image;
			// return image;
		} catch (Exception e) {
			TopoHandler.logger.warn("Problem in generating the Image for graph", e);
			return null;
		}
	}
	
	
}
