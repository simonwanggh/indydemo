package com.dxc.ngp.service;

import static org.hyperledger.indy.sdk.anoncreds.Anoncreds.proverCreateCredentialReq;
import static org.hyperledger.indy.sdk.anoncreds.Anoncreds.proverCreateMasterSecret;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.hyperledger.indy.sdk.IndyException;
import org.hyperledger.indy.sdk.anoncreds.Anoncreds;
import org.hyperledger.indy.sdk.anoncreds.AnoncredsResults;
import org.hyperledger.indy.sdk.did.Did;
import org.hyperledger.indy.sdk.did.DidResults;
import org.hyperledger.indy.sdk.ledger.Ledger;
import org.hyperledger.indy.sdk.ledger.LedgerResults;
import org.hyperledger.indy.sdk.ledger.LedgerResults.ParseResponseResult;
import org.hyperledger.indy.sdk.pool.Pool;
import org.hyperledger.indy.sdk.wallet.Wallet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import com.dxc.ngp.service.vo.UserVo;

@Service
public class UserService extends CommonService{
	
	private final String COMMON_MASTER_SECRET = "common_master_secret_name";
	
	private UserVo vo;

	public String generateCredentialRequest(Wallet wallet, String userId, String credOffer, String credDefJson) throws InterruptedException, ExecutionException, IndyException {
		String userDid = vo.getUserDid();
		
		proverCreateMasterSecret(wallet, COMMON_MASTER_SECRET).get();
		
		AnoncredsResults.ProverCreateCredentialRequestResult createCredReqResult =
				proverCreateCredentialReq(wallet, userDid, credOffer, credDefJson, COMMON_MASTER_SECRET).get();
		String credReqJson = createCredReqResult.getCredentialRequestJson();
		String credReqMetadataJson = createCredReqResult.getCredentialRequestMetadataJson();
	
		String ret = "\nCredReqJson : "+credReqJson+"\ncredReqMetadataJson :" +credReqMetadataJson;
		this.writeTofile("credentialRequest", ret);
		return ret;
		
	}	


	@SuppressWarnings("unused")
	private String generateUserSeed(String userId) {
		String[] prefix = new String[32-userId.length()];
		Arrays.fill(prefix, "0");
		
		return String.join("", prefix) + userId;
	}


	public String store(Pool pool, Wallet wallet , String credReqMetadataJson, String credentialJson, String credDefJson) throws InterruptedException, ExecutionException, IndyException {
		
		JSONObject credential = new JSONObject(credentialJson);
		String revocRegDefJson = getRevocRegDefJson(pool, credential.getString("rev_reg_id"));

		// Prover store received Credential
		return Anoncreds.proverStoreCredential(wallet, "credential1_id",
				credReqMetadataJson, credentialJson, credDefJson, revocRegDefJson).get();		
		
	}


	private String getRevocRegDefJson(Pool pool, String revRegIdf) throws JSONException, InterruptedException, ExecutionException, IndyException {
		// Prover gets RevocationRegistryDefinition
		String getRevRegDefRequest = Ledger.buildGetRevocRegDefRequest(vo.getUserDid(), revRegIdf).get();
		String getRevRegDefResponse = Ledger.submitRequest(pool, getRevRegDefRequest).get();

		ParseResponseResult revRegInfo1 = Ledger.parseGetRevocRegDefResponse(getRevRegDefResponse).get();
		String revocRegDefJson = revRegInfo1.getObjectJson();
		return revocRegDefJson;
	}


	public String generateVerifyRequestInfo(Pool pool, Wallet wallet) throws Exception {
		

		long to = System.currentTimeMillis() / 1000;
		String proofRequest = new JSONObject().
				put("nonce", "123432421212").
				put("name", "proof_req_1").
				put("version", "0.1").
				put("requested_attributes", new JSONObject().
						put("attr1_referent", new JSONObject().
								put("name", "name"))).
				put("requested_predicates", new JSONObject().
						put("predicate1_referent", new JSONObject().
								put("name", "age").put("p_type", ">=").put("p_value", 18))).
				put("non_revoked", new JSONObject().
						put("to", to)).toString();


		// Prover gets Claims for Proof Request
		String credsJson = Anoncreds.proverGetCredentialsForProofReq(wallet, proofRequest).get();

		JSONObject credentials = new JSONObject(credsJson);
		JSONArray credsForReferent = credentials.getJSONObject("attrs").getJSONArray("attr1_referent");
		JSONObject cred_info = credsForReferent.getJSONObject(0).getJSONObject("cred_info");

		// Prover gets RevocationRegistryDelta from Ledger
		String revRegIdfromCred = cred_info.getString("rev_reg_id");
		String getRevRegDeltaRequest = Ledger.buildGetRevocRegDeltaRequest(vo.getUserDid(), revRegIdfromCred, - 1, (int) to).get();
		String getRevRegDeltaResponse = Ledger.submitRequest(pool, getRevRegDeltaRequest).get();

		LedgerResults.ParseRegistryResponseResult revRegInfo2 = Ledger.parseGetRevocRegDeltaResponse(getRevRegDeltaResponse).get();

		String revRegId = revRegInfo2.getId();
		String revocRegDeltaJson = revRegInfo2.getObjectJson();
		long timestamp = revRegInfo2.getTimestamp();
		
		// Prover gets RevocationRegistryDefinition
		
		String getRevRegDefRequest = Ledger.buildGetRevocRegDefRequest(vo.getUserDid(), revRegIdfromCred).get();
		String getRevRegDefResponse = Ledger.submitRequest(pool, getRevRegDefRequest).get();

		ParseResponseResult revRegInfo1 = Ledger.parseGetRevocRegDefResponse(getRevRegDefResponse).get();
		String revocRegDefJson = revRegInfo1.getObjectJson();

		// Prover creates RevocationState
		int blobStorageReaderHandle = getBlobStorageReaderHandle().getBlobStorageReaderHandle();
		String revStateJson = Anoncreds.createRevocationState(blobStorageReaderHandle,
				revocRegDefJson, revocRegDeltaJson, timestamp,  cred_info.getString("cred_rev_id")).get();

		// Prover gets Schema from Ledger
		String getSchemaRequest = Ledger.buildGetSchemaRequest(vo.getUserDid(), cred_info.getString("schema_id")).get();
		String getSchemaResponse = Ledger.submitRequest(pool, getSchemaRequest).get();

		ParseResponseResult schemaInfo2 = Ledger.parseGetSchemaResponse(getSchemaResponse).get();
		String schemaId = schemaInfo2.getId();
		String schemaJson = schemaInfo2.getObjectJson();

		// Prover creates Proof
		String requestedCredentialsJson = new JSONObject().
				put("self_attested_attributes", new JSONObject()).
				put("requested_attributes", new JSONObject().
						put("attr1_referent", new JSONObject().
								put("cred_id", cred_info.get("referent")).
								put("timestamp", timestamp).
								put("revealed", true))).
				put("requested_predicates", new JSONObject().
						put("predicate1_referent", new JSONObject().
								put("cred_id", cred_info.get("referent")).
								put("timestamp", timestamp))).toString();

		String schemasJson = new JSONObject().put(schemaId, new JSONObject(schemaJson)).toString();
		String credDefId = cred_info.getString("cred_def_id");
		String credDefsJson = new JSONObject().put(credDefId, new JSONObject(this.getCredDefJson(pool, credDefId))).toString();
		String revStatesJson = new JSONObject().put(revRegId, new JSONObject().put(String.valueOf(timestamp), new JSONObject(revStateJson))).toString();

		String proofJson = Anoncreds.proverCreateProof(wallet, proofRequest,
				requestedCredentialsJson, COMMON_MASTER_SECRET,
				schemasJson, credDefsJson, revStatesJson).get();


		String ret = "{\"proofRequestJson\":"+proofRequest + ",\"proofJson\":"+proofJson+"}";
		this.writeTofile("verifyRequest.json", ret);
		return ret;
	}
	
	
	@PreDestroy
	public void storeVo() throws FileNotFoundException, IOException {
		if(vo == null) {
			return;
		}
		
		String homePath = System.getProperty("user.home");
		String demoStrorePath = homePath + "/.indy_client/demostore";
		Path dirPathObj = Paths.get(demoStrorePath);
		
		if(!Files.exists(dirPathObj)) {
			try {
				Files.createDirectories(dirPathObj);
			} catch (IOException e) {
				System.out.println("create store dir error!");
			}
		}
		
		String fileStorePath = homePath + "/.indy_client/demostore/uservo.ser";
		
		Path filePath = Paths.get(fileStorePath);
		if(Files.exists(filePath)){
			Files.delete(Paths.get(fileStorePath));
		}
		Files.createFile(filePath);

		try(FileOutputStream fileOutputStream = new FileOutputStream(fileStorePath);
				ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream)){
		    objectOutputStream.writeObject(vo);
		    objectOutputStream.flush();
		}
		
	}
	
	
	@PostConstruct
	public void initialize() throws IOException, ClassNotFoundException {
		String homePath = System.getProperty("user.home");
		String demoStrorePath = homePath + "/.indy_client/demostore/uservo.ser";
		Path dirPathObj = Paths.get(demoStrorePath);
		if(!Files.exists(dirPathObj)) {
			return;
		}		
		
		
		try(FileInputStream fileInputStream = new FileInputStream(demoStrorePath);
				ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream)){
				vo = (UserVo) objectInputStream.readObject();	    
		}
	}


	public String  createDid(Wallet wallet,String userId) throws InterruptedException, ExecutionException, IndyException {
		if(vo == null) {
			vo = new UserVo();
		}		

		DidResults.CreateAndStoreMyDidResult userResult = Did.createAndStoreMyDid(wallet, "{}").get();
		vo.setUserDid(userResult.getDid());			
		
		return "\nPlease take the DID and verkey to publish by Issuer\nDID : " + vo.getUserDid() +"\nverkey : "+ userResult.getVerkey();
		
	}


	public String getUserDid() {
		// TODO Auto-generated method stub
		return vo.getUserDid();
	}

}
