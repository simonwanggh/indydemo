package com.dxc.ngp.shell;

import java.io.File;
import java.util.concurrent.ExecutionException;

import org.hyperledger.indy.sdk.IndyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import com.dxc.ngp.service.PoolLedgerService;

@ShellComponent
public class PoolCommands {
	
	@Autowired
	private PoolLedgerService poolLedgerService;
	
	@ShellMethod(value="create pool configuration and open pool by given configuration file path and poolName")
	public String openPool(String confPath, String poolName) throws InterruptedException, ExecutionException, IndyException {
		File conffile = new File(confPath);
		poolLedgerService.openPoolLedger(conffile, poolName);
		return String.format("pool(%s) is opened",poolName);
		
	}

}
