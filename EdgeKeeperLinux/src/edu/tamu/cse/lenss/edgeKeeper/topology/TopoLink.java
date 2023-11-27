package edu.tamu.cse.lenss.edgeKeeper.topology;

import java.io.Serializable;
import java.util.Set;
import java.util.UUID;

import org.jgrapht.graph.DefaultWeightedEdge;

import edu.tamu.cse.lenss.edgeKeeper.utils.EKConstants;

public class TopoLink extends DefaultWeightedEdge implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4700646982346319918L;

	public long lastUpdateTime;
	public Set<String> ipPair;
	public Double rtt = null;
	public double prcv;

	private int firstSeqRcvd = Integer.MIN_VALUE;
	private int lastSeqRcvd = Integer.MIN_VALUE;
	private int totalBroadcastRcvd = 0;
	private String linkSession = UUID.randomUUID().toString();

	/*
	 * I specially override this method to ensure that the graph is visualized well.
	 */
	@Override
	public String toString() {
//		return ipPair + "\n" + this.getWeight() + "," + this.rtt;
		return ipPair + "\n" + String.format("%.4f, %.4f", this.getWeight(),  this.rtt);

	}

	/*
	 * Recall the PRCV calculation for OLSR. This method basically keeps track of how many
	 * boradcast packets are received for this link. Probability of receiving packets
	 * successfully on this link is calculated accordingly
	 */
	void updatePrcv(String sessionID, int seqNo) {
		// Now calculate the prcv
		if ((!linkSession.equals(sessionID)) || firstSeqRcvd == Integer.MIN_VALUE) {
			this.linkSession = sessionID;
			this.firstSeqRcvd = seqNo;
			this.lastSeqRcvd = seqNo;
			this.totalBroadcastRcvd = 0;
		}
		this.lastSeqRcvd = Math.max(seqNo, this.lastSeqRcvd);
		this.totalBroadcastRcvd += 1;
		prcv = (double) this.totalBroadcastRcvd / ( (this.lastSeqRcvd - this.firstSeqRcvd)/EKConstants.BRD_SEQ_INCR + 1);
	}
	
	double updateStaleLink() {
		double prcvDest = 1.0 / (this.prcv * this.getEtx());
		this.lastSeqRcvd += EKConstants.BRD_SEQ_INCR;
		this.prcv = (double) this.totalBroadcastRcvd / ( (this.lastSeqRcvd - this.firstSeqRcvd)/EKConstants.BRD_SEQ_INCR + 1);
		return 1.0 / (prcvDest * this.prcv);
	}

	/*
	 * This function is only called upon receiving a reply message. The RTT is updated accordingly. 
	 * This function can be made sophisticated by using stanrd deviation and other methods.
	 */
	public void updateRTT(Long newRtt) {
		if (this.rtt == null)
			this.rtt = 1.0 * newRtt;
		else
			this.rtt = 0.75 * this.rtt + 0.25 * newRtt;
	}

	public double getEtx() {
		return this.getWeight();
	}
	
}
