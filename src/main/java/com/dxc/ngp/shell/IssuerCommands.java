package com.dxc.ngp.shell;


import java.util.concurrent.ExecutionException;

import org.hyperledger.indy.sdk.IndyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.Availability;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;

import com.dxc.ngp.service.IssuerService;
import com.dxc.ngp.service.PoolLedgerService;
import com.dxc.ngp.service.WalletService;

@ShellComponent
public class IssuerCommands {
	
	@Autowired
	private IssuerService issuerService;	
	
	@Autowired
	private PoolLedgerService poolLedgerService;
	
	@Autowired
	private WalletService walletService;
	
	
	
	@ShellMethod(value="create credential offer, user apply it to create credential request")
	public String credentialOffer() throws InterruptedException, ExecutionException, IndyException {
		return "credentialOffer : "+issuerService.createCredentialOffer(walletService.openIssuerWallet(), poolLedgerService.getOpenedPool());
		
	}
	
	@ShellMethod(value="issue a credential. it will be available after the pool is opened")
	public String issueCredential(String credReqJson, String credOffer) throws InterruptedException, ExecutionException, IndyException {
		return issuerService.issueCredential(poolLedgerService.getOpenedPool(),walletService.openIssuerWallet(), credOffer,credReqJson, poolLedgerService.getPoolName());
	}
	
	@ShellMethod(value="publish DID")
	public String publishDid(String did, String verkey) throws InterruptedException, ExecutionException, IndyException {
		return issuerService.publishDid(poolLedgerService.getOpenedPool(),walletService.openIssuerWallet(),did, verkey);		
	}
	
	public Availability publishDidAvailability() {
        return walletService.hasOpenedWallet("issuer_Wallet")
            ? Availability.available()
            : Availability.unavailable("you are not open a wallet");
    }
	
	
	@ShellMethodAvailability()
    public Availability availabilityCheck() {
        return poolLedgerService.hasOpenedPool()
            ? Availability.available()
            : Availability.unavailable("you are not open pool");
    }

}
