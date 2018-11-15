package com.dxc.ngp.service;


import static org.hyperledger.indy.sdk.anoncreds.Anoncreds.issuerCreateCredential;
import static org.hyperledger.indy.sdk.anoncreds.Anoncreds.issuerCreateCredentialOffer;
import static org.hyperledger.indy.sdk.anoncreds.Anoncreds.issuerCreateSchema;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.lang3.StringUtils;
import org.hyperledger.indy.sdk.IndyException;
import org.hyperledger.indy.sdk.anoncreds.Anoncreds;
import org.hyperledger.indy.sdk.anoncreds.AnoncredsResults;
import org.hyperledger.indy.sdk.blob_storage.BlobStorageReader;
import org.hyperledger.indy.sdk.blob_storage.BlobStorageWriter;
import org.hyperledger.indy.sdk.did.Did;
import org.hyperledger.indy.sdk.did.DidResults;
import org.hyperledger.indy.sdk.ledger.Ledger;
import org.hyperledger.indy.sdk.ledger.LedgerResults.ParseResponseResult;
import org.hyperledger.indy.sdk.pool.Pool;
import org.hyperledger.indy.sdk.wallet.Wallet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import com.dxc.ngp.service.vo.IssuerVo;



@Service
public class IssuerService extends CommonService{
	
	private static final Logger LOG = Logger.getLogger(IssuerService.class.getName());
	
	private static final String TRUSTEE_SEED = "000000000000000000000000Trustee1";
	
	private IssuerVo vo;
	
	private static final int RESUBMIT_REQUEST_CNT = 3;
	private static final int RESUBMIT_REQUEST_TIMEOUT = 5_000;
	
	private static final String REVOC_REG_TYPE = "CL_ACCUM";
	
	
	
	public interface PoolResponseChecker {
		boolean check(String response);
	}
	
	public String ensurePreviousRequestApplied(Pool pool, String checkerRequest, PoolResponseChecker checker) throws IndyException, ExecutionException, InterruptedException {
		for (int i = 0; i < RESUBMIT_REQUEST_CNT; i++) {
			String response = Ledger.submitRequest(pool, checkerRequest).get();
			try {
				if (checker.check(response)) {
					return response;
				}
			} catch (JSONException e) {
				e.printStackTrace();
				System.err.println(e.toString());
				System.err.println(response);
			}
			Thread.sleep(RESUBMIT_REQUEST_TIMEOUT);
		}
		throw new IllegalStateException();
	}

	public String issueCredential(Pool pool,Wallet issuerWallet ,String credOffer,String credReqJson, String poolName) throws InterruptedException, ExecutionException, IndyException {
	
		// Issuer creates TailsReader
		BlobStorageReader blobStorageReaderHandle =  getBlobStorageReaderHandle(); 
		
		//Issuer create Credential
		//   note that encoding is not standardized by Indy except that 32-bit integers are encoded as themselves. IS-786
		String credValuesJson = new JSONObject("{\n" +
				"        \"sex\": {\"raw\": \"male\", \"encoded\": \"5944657099558967239210949258394887428692050081607692519917050\"},\n" +
				"        \"name\": {\"raw\": \"Alex\", \"encoded\": \"1139481716457488690172217916278103335\"},\n" +
				"        \"height\": {\"raw\": \"175\", \"encoded\": \"175\"},\n" +
				"        \"age\": {\"raw\": \"28\", \"encoded\": \"28\"}\n" +
				"    }").toString();

		AnoncredsResults.IssuerCreateCredentialResult createCredentialResult =
				issuerCreateCredential(issuerWallet, credOffer, credReqJson, credValuesJson, vo.getRevRegId(), blobStorageReaderHandle.getBlobStorageReaderHandle()).get();
		
		
		
		// Issuer posts RevocationRegistryDelta to Ledger
		String revRegEntryRequest = Ledger.buildRevocRegEntryRequest(vo.getIssuerDid(), vo.getRevRegId(), REVOC_REG_TYPE, createCredentialResult.getRevocRegDeltaJson()).get();
		Ledger.signAndSubmitRequest(pool, issuerWallet, vo.getIssuerDid(), revRegEntryRequest).get();

		String ret = "\nCredential : "+createCredentialResult.getCredentialJson() + "\n credRevId : " + createCredentialResult.getRevocId();
		this.writeTofile("credention.json", ret);
		return ret;
		
	}
	
	
	

	private String retrieveIssuerDid(Pool pool, Wallet issuerWallet) throws InterruptedException, ExecutionException, IndyException {
				
		if(vo == null) {
			vo = new IssuerVo();
		}
		
		if(StringUtils.isEmpty(vo.getIssuerDid())) {		
			try {			
				String listDidsWithMetaJson = Did.getListMyDidsWithMeta(issuerWallet).get();
				JSONArray listDidsWithMeta = new JSONArray(listDidsWithMetaJson);
				vo.setIssuerDid(listDidsWithMeta.getJSONObject(0).getString("did"));
			}catch(Exception e) {
				System.out.println("Warning ! no issuerdid saved");
			}		
			
		}
		
		if(StringUtils.isEmpty(vo.getIssuerDid())) {
						
			// Issuer create DID
			DidResults.CreateAndStoreMyDidResult trusteeDidInfo = Did.createAndStoreMyDid(issuerWallet, new JSONObject().put("seed", TRUSTEE_SEED).toString()).get();
			DidResults.CreateAndStoreMyDidResult issuerDidInfo = Did.createAndStoreMyDid(issuerWallet, "{}").get();
			String nymRequest = Ledger.buildNymRequest(trusteeDidInfo.getDid(), issuerDidInfo.getDid(),
					issuerDidInfo.getVerkey(), null, "TRUSTEE").get();
			Ledger.signAndSubmitRequest(pool, issuerWallet, trusteeDidInfo.getDid(), nymRequest).get();

			vo.setIssuerDid(issuerDidInfo.getDid());
		}
		
		
		return vo.getIssuerDid();
	}
	

	
	


	public String createCredentialOffer(Wallet issuerWallet, Pool pool) throws InterruptedException, ExecutionException, IndyException {
		
		String issuerDid = retrieveIssuerDid(pool, issuerWallet);
		//4. Issuer Creates Credential Schema	
		
		
		if(vo == null) {
			vo  = new IssuerVo();
		}
		
		if(StringUtils.isEmpty(vo.getSchemaId())) {
			String schemaName = "gvt";
			String schemaVersion = "1.0";
			String schemaAttributes = "[\"name\", \"age\", \"sex\", \"height\"]";
			AnoncredsResults.IssuerCreateSchemaResult createSchemaResult =
					issuerCreateSchema(issuerDid, schemaName, schemaVersion, schemaAttributes).get();
			
			
			// Issuer posts Schema to Ledger
			String schemaRequest = Ledger.buildSchemaRequest(issuerDid, createSchemaResult.getSchemaJson()).get();
			Ledger.signAndSubmitRequest(pool, issuerWallet, issuerDid, schemaRequest).get();

			// Issuer get Schema from Ledger
			String getSchemaRequest = Ledger.buildGetSchemaRequest(issuerDid, createSchemaResult.getSchemaId()).get();
			String getSchemaResponse = ensurePreviousRequestApplied(pool, getSchemaRequest, response -> {
				JSONObject getSchemaResponseObject = new JSONObject(response);
				return ! getSchemaResponseObject.getJSONObject("result").isNull("seqNo");
			});

			// !!IMPORTANT!!
			// It is important to get Schema from Ledger and parse it to get the correct schema JSON and correspondent id in Ledger
			// After that we can create CredentialDefinition for received Schema(not for result of indy_issuer_create_schema)

			ParseResponseResult schemaInfo1 = Ledger.parseGetSchemaResponse(getSchemaResponse).get();
						
			vo.setSchemaId(schemaInfo1.getId());
			vo.setSchemaJson(schemaInfo1.getObjectJson());
		}
		

		String schemaJson = vo.getSchemaJson();
		
		if(StringUtils.isEmpty(vo.getCredDefId())) {
			String credDefTag = "tag1";
			AnoncredsResults.IssuerCreateAndStoreCredentialDefResult createCredDefResult =
					Anoncreds.issuerCreateAndStoreCredentialDef(issuerWallet, issuerDid, schemaJson,
							credDefTag, null, new JSONObject().put("support_revocation", true).toString()).get();
			
			
			
			vo.setCredDefId(createCredDefResult.getCredDefId());
			vo.setCredDefJson(createCredDefResult.getCredDefJson());
			
			String credDefReqJson = Ledger.buildCredDefRequest(issuerDid, vo.getCredDefJson()).get();
			Ledger.signAndSubmitRequest(pool, issuerWallet, issuerDid, credDefReqJson).get();
			
			
			// Issuer creates RevocationRegistry
			/* FIXME: getIndyHomePath hard coded forward slash "/". It will not work for Windows. */
			String tailsWriterConfig =  getTailsWriterConfig();
			BlobStorageWriter tailsWriterHandle = BlobStorageWriter.openWriter("default", tailsWriterConfig).get();

			AnoncredsResults.IssuerCreateAndStoreRevocRegResult revRegInfo =
					Anoncreds.issuerCreateAndStoreRevocReg(issuerWallet, issuerDid, null, credDefTag,
							vo.getCredDefId(),
							new JSONObject().put("max_cred_num", 5).put("issuance_type", "ISSUANCE_ON_DEMAND").toString(),
							tailsWriterHandle).get();

			String revRegId = revRegInfo.getRevRegId();
			String revRegDefJson = revRegInfo.getRevRegDefJson();
			String revRegEntryJson = revRegInfo.getRevRegEntryJson();

			// Issuer posts RevocationRegistryDefinition to Ledger
			String revRegDefRequest = Ledger.buildRevocRegDefRequest(issuerDid, revRegDefJson).get();
			Ledger.signAndSubmitRequest(pool, issuerWallet, issuerDid, revRegDefRequest).get();

			// Issuer posts RevocationRegistryEntry to Ledger
			String revRegEntryRequest = Ledger.buildRevocRegEntryRequest(issuerDid, revRegId,
					REVOC_REG_TYPE, revRegEntryJson).get();
			Ledger.signAndSubmitRequest(pool, issuerWallet, issuerDid, revRegEntryRequest).get();
			
			vo.setRevRegId(revRegId);
		}
		
		
		String credDefId = vo.getCredDefId();
		
	
		//7. Issuer Creates Credential Offer
		
		String ret = issuerCreateCredentialOffer(issuerWallet, credDefId).get();
		this.writeTofile("credentialOffer.json", ret);
		return 	ret;	
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
		
		String fileStorePath = homePath + "/.indy_client/demostore/issuervo.ser";
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
		String demoStrorePath = homePath + "/.indy_client/demostore/issuervo.ser";
		Path dirPathObj = Paths.get(demoStrorePath);
		if(!Files.exists(dirPathObj)) {
			return;
		}		
		
		
		try(FileInputStream fileInputStream = new FileInputStream(demoStrorePath);
				ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream)){
				vo = (IssuerVo) objectInputStream.readObject();	    
		}
	}



	public String publishDid(Pool pool, Wallet issuerWallet, String did, String verkey) throws InterruptedException, ExecutionException, IndyException {
		String nymRequest = Ledger.buildNymRequest(vo.getIssuerDid(), did, verkey, null, null).get();
		Ledger.signAndSubmitRequest(pool, issuerWallet, vo.getIssuerDid(), nymRequest).get();
		return "Success";
	}
	

	

}
