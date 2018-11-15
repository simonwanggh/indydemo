package com.dxc.ngp.service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.hyperledger.indy.sdk.IndyException;
import org.hyperledger.indy.sdk.wallet.Wallet;
import org.springframework.stereotype.Service;

@Service
public class WalletService {
	


	Map<String, Wallet> walletMap = new HashMap<String, Wallet>();
	
	
	public Wallet openIssuerWallet() throws InterruptedException, ExecutionException, IndyException {
		return openWallet("issuer_Wallet","issuer_wallet_key");
		
	}

	public Wallet openWallet(String walletId, String walletKey) throws InterruptedException, ExecutionException, IndyException {
		
		if(!walletMap.containsKey(walletId)){
		
			//2. Issuer Create and Open Wallet
			String walletConfig = String.format("{ \"id\":\"%s\", \"storage_type\":\"default\" }",walletId);
			String walletCredentials = String.format("{\"key\":\"%s\"}", walletKey);
			Wallet issuerWallet = null;
			try {
				Wallet.createWallet(walletConfig, walletCredentials).get();
				issuerWallet = Wallet.openWallet(walletConfig,walletCredentials).get();				
			}catch(Exception e) {
				e.printStackTrace();
			}	
			walletMap.put(walletId, issuerWallet);
		}
		return walletMap.get(walletId);
	}
	
	public void closeAllWallet() {
		if(walletMap.isEmpty()) {
			return;
		}
		
		walletMap.values().stream().forEach(w->{
			try {
				w.closeWallet().get();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
		walletMap.clear();
	}

	public boolean hasOpenedWallet(String walletId) {
		return walletMap.containsKey(walletId);
	}

}
