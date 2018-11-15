package com.dxc.ngp.shell;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;

import org.hyperledger.indy.sdk.IndyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.Availability;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import com.dxc.ngp.service.PoolLedgerService;
import com.dxc.ngp.service.VerifierService;

@ShellComponent
public class VerifierCommands {
	
	@Autowired
	private VerifierService verifierService;
	
	@Autowired
	private PoolLedgerService poolLedgerService;	
	

	
	@ShellMethod(value="verify the given credential")
	public String verify(String userId) throws InterruptedException, ExecutionException, IndyException, IOException {
		String fullPath = System.getProperty("user.home")+ "/.indy_client/demostore/"+userId+"/verifyrequst.json";
		String verifyReq = new String(Files.readAllBytes(Paths.get(fullPath)));
		
		boolean res = verifierService.verify(poolLedgerService.getOpenedPool(),verifyReq);
		return res? "Success" : "Failed";
	}
	
	
	public Availability verifyAvailability() {
        return poolLedgerService.hasOpenedPool()
            ? Availability.available()
            : Availability.unavailable("you are not open pool");
    }

}
