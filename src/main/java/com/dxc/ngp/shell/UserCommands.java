package com.dxc.ngp.shell;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;

import org.hyperledger.indy.sdk.IndyException;
import org.hyperledger.indy.sdk.wallet.Wallet;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.Availability;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import com.dxc.ngp.service.PoolLedgerService;
import com.dxc.ngp.service.UserService;
import com.dxc.ngp.service.WalletService;

@ShellComponent
public class UserCommands {
	
	@Autowired
	private PoolLedgerService poolLedgerService;	
	
	@Autowired
	private WalletService walletService;
	
	@Autowired
	private UserService userService;
	
	private boolean isWalletOpened = false;
	
	private boolean isDidCreated = false;
	
	private boolean isCredentialStored = false;
	
	private boolean isCreatedVerifyRequst = false;
	
	private boolean isCreateCredRequest = false;
	
	private Wallet wallet;
	
	private String credDefJson;
	
	private String userId;
	
	
	@ShellMethod(value="open a wallet with wallet id and key")
	public String openWallet(String userId, String userKey) throws InterruptedException, ExecutionException, IndyException {
		this.wallet = walletService.openWallet(userId,userKey);
		this.userId = userId;
		this.isWalletOpened = true;
		init();
		return wallet.toString() + "is opened";
	}
	
	private void init() {
		isDidCreated = false;
		isCredentialStored = false;
		isCreatedVerifyRequst =false;
		isCreateCredRequest = false;
	}

	@ShellMethod(value="Create DID")
	public String createDid() throws InterruptedException, ExecutionException, IndyException {
		String ret = userService.createDid(wallet,userId);
		isDidCreated = true;
		return ret;
	}
	
	@ShellMethod(value="generate request for credential by providing credential offer from the issuer")
	public String credRequest(String credOffer, String userId) throws InterruptedException, ExecutionException, IndyException {
		String submitterDid = userService.getUserDid();
		JSONObject credOfferObj = new JSONObject(credOffer);
			
		credDefJson = poolLedgerService.getCredDefJson(submitterDid, getCredDefId(credOfferObj));
		
		String ret = userService.generateCredentialRequest(wallet,userId,credOffer,credDefJson);
		
		isCreateCredRequest = true;
		return ret;
	}
	
	

	@ShellMethod(value="store credential")
	public String store(String credReqMetadataJson, String credential) throws InterruptedException, ExecutionException, IndyException {
		String ret = userService.store(poolLedgerService.getOpenedPool(),wallet, credReqMetadataJson, credential, credDefJson);
		isCredentialStored = true;
		return ret;	
	}
	
	@ShellMethod(value="generate request for verification (credential must be stored first)")
	public String verifyRequest() throws Exception {
		String proof = userService.generateVerifyRequestInfo(poolLedgerService.getOpenedPool(),wallet);
		
		prepareFile(userId);
		
		String fullPath = System.getProperty("user.home")+ "/.indy_client/demostore/"+userId+"/verifyrequst.json";
		
		Path path = Paths.get(fullPath);		

		try (BufferedWriter writer = Files.newBufferedWriter(path)) {
		    writer.write(proof);
		}
		isCreatedVerifyRequst = true;
		return "save to file : " + fullPath;
		
	}
	
	private void prepareFile(String userId) throws IOException {
		String strorePath = System.getProperty("user.home")+ "/.indy_client/demostore";
		Path spath = Paths.get(strorePath);
		
		if(!Files.exists(spath)) {
			Files.createDirectories(spath);
		}
		
		String userPath = strorePath + "/" + userId;
		
		Path upath = Paths.get(userPath);
		
		if(!Files.exists(upath)) {
			Files.createDirectories(upath);
		}
		
		String reqPath = userPath + "/verifyrequst.json";
		
		Path rpath = Paths.get(reqPath);
		
		if(!Files.exists(rpath)) {
			Files.createFile(rpath);
		}else {
			Files.delete(rpath);
			Files.createFile(rpath);
		}
		
	}
	
	
	private String getSchemaId(JSONObject credOfferObj) {
		return credOfferObj.getString("schema_id");

	}
	
	private String getCredDefId(JSONObject credOfferObj) {
		return credOfferObj.getString("cred_def_id");
	}
	
	public Availability createDidAvailability() {
        return isWalletOpened
                ? Availability.available()
                : Availability.unavailable("you are not open a wallet");
    }

    public Availability openWalletAvailability() {
        return poolLedgerService.hasOpenedPool()
            ? Availability.available()
            : Availability.unavailable("you are not open pool");
    }
    
    public Availability createCredRequestAvailability() {
        return isWalletOpened && isDidCreated
            ? Availability.available()
            : Availability.unavailable("you are not open a wallet or create DID for wallet");
    }
    
    public Availability storeAvailability() {
        return isCreateCredRequest
            ? Availability.available()
            : Availability.unavailable("you are not create a credential request");
    }
    
    public Availability verifyAvailability() {
        return (isCreatedVerifyRequst)
            ? Availability.available()
            : Availability.unavailable("you are not create a verify request");
    }
    
    public Availability verifyRequestAvailability() {
        return (isCredentialStored)
            ? Availability.available()
            : Availability.unavailable("you are not store credential");
    }


}
