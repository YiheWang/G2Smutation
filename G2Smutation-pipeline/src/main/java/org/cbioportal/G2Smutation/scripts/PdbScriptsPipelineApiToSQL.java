package org.cbioportal.G2Smutation.scripts;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.cbioportal.G2Smutation.util.ReadConfig;
import org.cbioportal.G2Smutation.util.models.RSMutationRecord;
import org.apache.log4j.Logger;
//import org.json.JSONArray;
//import org.json.JSONException;
//import org.json.JSONObject;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;


public class PdbScriptsPipelineApiToSQL {
	final static Logger log = Logger.getLogger(PdbScriptsPipelineMakeSQL.class);

	/**
	 * 
	 * @param filename
	 * @return
	 */
	public List<String> getDbSNPIdFromMappingFile(String filename) {
	    List<String> snpIds = new ArrayList<>();
		try {		    
    		// open "SNP3D_PDB_GRCH37" file
    		String SNPfilepwd = new String(filename);
    		File SNPfile = new File(SNPfilepwd);
    		List<String> SNPfilelines = FileUtils.readLines(SNPfile, StandardCharsets.UTF_8.name());
    		int SNPNum = SNPfilelines.size()-1;
    		for (int i = 1; i <= SNPNum; i++) {
    		    snpIds.add(SNPfilelines.get(i).split("\\s+")[2]);
    		}
    		snpIds= PdbScriptsPipelineMakeSQL.removeStringListDupli(snpIds);
			} catch (Exception ex) {
				log.error(ex.getMessage());
				ex.printStackTrace();
			}
		return snpIds;
		
	}
	
	public List<String> callUrl(String urlName, List<String> bufferLines, String snpId){
		try {
			URL url = new URL(urlName);
			BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
			String s;
			String HMkey;
			HashMap<String, String> lineHM = new HashMap<>();
			RSMutationRecord rmr = new RSMutationRecord();
			if ((s = reader.readLine()) != null) {
			    JSONArray jsonarray = JSONArray.fromObject(s);
				for (int i = 0; i < jsonarray.size(); i++) {
				    JSONObject jsonobj = JSONObject.fromObject(jsonarray.get(i));
				    JSONArray residueMapping = JSONArray.fromObject(jsonobj.get("residueMapping"));
					//log.info(residueMapping);
					HMkey = jsonobj.get("pdbNo").toString() + jsonobj.get("pdbSeg").toString();
					if(lineHM.containsKey(HMkey)) {
						continue;
					}
					else {
						lineHM.put(HMkey, "");						
						if(!residueMapping.isEmpty()) {
						    JSONObject residueobj = JSONObject.fromObject(residueMapping.get(0));							
							rmr.setRs_mutationId(Integer.parseInt(snpId));
							rmr.setSeqId(Integer.parseInt(jsonobj.get("seqId").toString()));
							rmr.setPdbNo(jsonobj.get("pdbNo").toString());
							rmr.setPdbResidueIndex(Integer.parseInt(residueobj.get("pdbPosition").toString()));
							rmr.setPdbResidueName(residueobj.get("pdbAminoAcid").toString());
							rmr.setAlignmentId(Integer.parseInt(jsonobj.get("alignmentId").toString()));
							bufferLines.add(makeTable_rs_mutation_insert(rmr));
						}
						else {
							rmr.setRs_mutationId(Integer.parseInt(snpId));
							rmr.setSeqId(Integer.parseInt(jsonobj.get("seqId").toString()));
							rmr.setPdbNo(jsonobj.get("pdbNo").toString());
							rmr.setPdbResidueIndex(-1);
							rmr.setPdbResidueName("");
							rmr.setAlignmentId(Integer.parseInt(jsonobj.get("alignmentId").toString()));
							bufferLines.add(makeTable_rs_mutation_insert(rmr));
						}
					}
				}
			}
				reader.close();
		} catch(Exception ex) {
			ex.printStackTrace();
		}
		return bufferLines;
	}
	
	public int generateRsSQLfile() {
		List<String> tempLines = new ArrayList<String>();
		int fileCount = 0;
		String rssqlfilepwd = new String(ReadConfig.workspace + ReadConfig.rsSqlInsertFile + "." + fileCount);
		File rssqlfile = new File(rssqlfilepwd);
		List<String> snpIds = getDbSNPIdFromMappingFile(ReadConfig.workspace + ReadConfig.dbsnpFile);	
		int sql_insert_output_interval = Integer.parseInt(ReadConfig.sqlInsertOutputInterval);
		List<String> outputLines = new ArrayList<String>();
		outputLines.add("SET autocommit = 0;");
		outputLines.add("start transaction;");
		log.info("Begin to generate sql file");		
		for (int j = 0; j < snpIds.size(); j++) {	//20
			String snpId = snpIds.get(j);
			//snpId = "1800369";
			String url = ReadConfig.getGnApiDbsnpInnerUrl();
			url = url.replace("DBSNPID", snpId);
			log.info(j + "th URL:" + url);
			if (j % sql_insert_output_interval != 0 || j ==0) {
				tempLines.clear();
				tempLines = callUrl(url, tempLines, snpId);
				outputLines.addAll(tempLines);
			}
			else {
				outputLines.add("commit;");
				try {
					FileUtils.writeLines(rssqlfile, StandardCharsets.UTF_8.name(), outputLines);
				} catch (IOException e) {
					log.info("input " + rssqlfilepwd + " failed");
				}
				log.info("Finish the " + fileCount +" file");
				fileCount++;
				rssqlfilepwd = new String(ReadConfig.workspace + ReadConfig.rsSqlInsertFile + "." + fileCount);
				rssqlfile = new File(rssqlfilepwd);
				outputLines.clear();
				outputLines.add("SET autocommit = 0;");
				outputLines.add("start transaction;");
				tempLines.clear();
				tempLines = callUrl(url, tempLines, snpId);
				outputLines.addAll(tempLines);
			}
		}
		outputLines.add("commit;");
		try {
			FileUtils.writeLines(rssqlfile, StandardCharsets.UTF_8.name(), outputLines);
		} catch (IOException e) {
			log.info("input " + rssqlfilepwd + " failed");
		}
		log.info("Finish the " + fileCount +" file");
		log.info("insert rssql successful!");
		return fileCount;
	}

	public String makeTable_rs_mutation_insert(RSMutationRecord rmr) {
        String str = "INSERT INTO `rs_mutation_entry` (`RS_MUTATION_ID`,`SEQ_ID`,`PDB_NO`,`PDB_INDEX`,`PDB_RESIDUE`,`ALIGNMENT_ID`)VALUES ("
                + rmr.getRs_mutationId() + ","+ rmr.getSeqId() + ",'" + rmr.getPdbNo() + "',"
                + rmr.getPdbResidueIndex() + ",'" + rmr.getPdbResidueName() + "'," + rmr.getAlignmentId() + ");\n";
        return str;
//		String str = "INSERT INTO `mutation_entry` VALUES ("
//                + rmr.getRs_mutationId() + ","+ rmr.getSeqId() + ",'" + rmr.getPdbNo() + "',"
//                + rmr.getPdbResidueIndex() + ",'" + rmr.getPdbResidueName() + "'," + rmr.getAlignmentId() + ");\n";
//        return str;
    }
	
}
