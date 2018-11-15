package com.dxc.ngp.service;

import static org.hyperledger.indy.sdk.anoncreds.Anoncreds.verifierVerifyProof;

import java.util.Iterator;
import java.util.concurrent.ExecutionException;

import org.hyperledger.indy.sdk.IndyException;
import org.hyperledger.indy.sdk.ledger.Ledger;
import org.hyperledger.indy.sdk.ledger.LedgerResults;
import org.hyperledger.indy.sdk.ledger.LedgerResults.ParseResponseResult;
import org.hyperledger.indy.sdk.pool.Pool;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

@Service
public class VerifierService extends CommonService{

	public boolean verify(Pool pool, String verifyReq) throws InterruptedException, ExecutionException, IndyException {
		JSONObject verifyReqObj = new JSONObject(verifyReq);
		
		JSONObject proofObj = verifyReqObj.getJSONObject("proofJson");
		JSONArray identArray = proofObj.getJSONArray("identifiers");
		Iterator<Object> itr = identArray.iterator();
		JSONObject schemasObj = new JSONObject();
		JSONObject credDefsObj = new JSONObject();
		JSONObject revRegDefsObj = new JSONObject();
		JSONObject revRegsObj = new JSONObject();
		while(itr.hasNext()) {
			JSONObject obj =  (JSONObject) itr.next();
			String schemaId = obj.getString("schema_id");
			schemasObj.put(schemaId, new JSONObject(this.getSchemaJson(pool, schemaId)));
			String credDefId = obj.getString("cred_def_id");
			credDefsObj.put(credDefId, new JSONObject(this.getCredDefJson(pool, credDefId)));
			
			
			
			String revRegId = obj.getString("rev_reg_id");
			String getRevRegDefReq = Ledger.buildGetRevocRegDefRequest(null, revRegId).get();
			String getRevRegDefResp = Ledger.submitRequest(pool, getRevRegDefReq).get();
			ParseResponseResult revRegDefInfo3 = Ledger.parseGetRevocRegDefResponse(getRevRegDefResp).get();
			String revRegDefId = revRegDefInfo3.getId();
			String revRegDefJson = revRegDefInfo3.getObjectJson();
			revRegDefsObj.put(revRegDefId, new JSONObject(revRegDefJson));
			
			String getRevRegReq = Ledger.buildGetRevocRegRequest(null, revRegId, obj.getInt("timestamp")).get();
			String getRevRegResp = Ledger.submitRequest(pool, getRevRegReq).get();
			LedgerResults.ParseRegistryResponseResult revRegInfo3 = Ledger.parseGetRevocRegResponse(getRevRegResp).get();
			revRegId = revRegInfo3.getId();
			String revRegJson = revRegInfo3.getObjectJson();
			long timestamp = revRegInfo3.getTimestamp();
			revRegsObj.put(revRegId, new JSONObject().
					put(String.valueOf(timestamp), new JSONObject(revRegJson)));
		}
		
		
		String schemas = schemasObj.toString();
		String credentialDefs =credDefsObj.toString();
		

		String proofRequestJson = verifyReqObj.getJSONObject("proofRequestJson").toString();
		String proofJson = proofObj.toString();

		String revRegDefsJson = revRegDefsObj.toString();
		String revRegsJson = revRegsObj.toString();
		return verifierVerifyProof(proofRequestJson, proofJson, schemas, credentialDefs, revRegDefsJson, revRegsJson).get();
	}

	

	

	
}
