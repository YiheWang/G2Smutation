package org.cbioportal.G2Smutation.util.models;

public class RSMutationRecord {
	
	private int rs_mutationId;
    //Inner seqId
    private int seqId;
    
    private String pdbNo;
    
    private int pdbResidueIndex;
    
    private String pdbResidueName;
    
    //alignmentId: id of Alignment
    private int alignmentId;
    
    /*
     * getter and setter methods 
     */

    public int getSeqId() {
        return seqId;
    }

    public void setSeqId(int seqId) {
        this.seqId = seqId;
    }

    public String getPdbNo() {
        return pdbNo;
    }

    public void setPdbNo(String pdbNo) {
        this.pdbNo = pdbNo;
    }

    public int getPdbResidueIndex() {
        return pdbResidueIndex;
    }

    public void setPdbResidueIndex(int pdbResidueIndex) {
        this.pdbResidueIndex = pdbResidueIndex;
    }

    public String getPdbResidueName() {
        return pdbResidueName;
    }

    public void setPdbResidueName(String pdbResidueName) {
        this.pdbResidueName = pdbResidueName;
    }

    public int getAlignmentId() {
        return alignmentId;
    }

    public void setAlignmentId(int alignmentId) {
        this.alignmentId = alignmentId;
    }

    public int getRs_mutationId() {
        return rs_mutationId;
    }

    public void setRs_mutationId(int rs_mutationId) {
        this.rs_mutationId = rs_mutationId;
    }

}
