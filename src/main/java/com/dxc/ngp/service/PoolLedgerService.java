package com.dxc.ngp.service;

import java.io.File;
import java.util.concurrent.ExecutionException;

import org.hyperledger.indy.sdk.IndyException;
import org.hyperledger.indy.sdk.ledger.Ledger;
import org.hyperledger.indy.sdk.pool.Pool;
import org.hyperledger.indy.sdk.pool.PoolJSONParameters;
import org.springframework.stereotype.Service;

/**
 * @author zwang44
 *
 */
@Service
public class PoolLedgerService {
	
	public static final int PROTOCOL_VERSION = 2;
	
	private Pool openedPool;
	
	private String poolName;

	public Pool openPoolLedger(File confFile, String poolName) throws InterruptedException, ExecutionException, IndyException {
		PoolJSONParameters.CreatePoolLedgerConfigJSONParameter createPoolLedgerConfigJSONParameter
			= new PoolJSONParameters.CreatePoolLedgerConfigJSONParameter(confFile.getAbsolutePath());
		Pool.createPoolLedgerConfig(poolName, createPoolLedgerConfigJSONParameter.toJson()).get();
		
		Pool.setProtocolVersion(PROTOCOL_VERSION).get();

		//1. Create and Open Pool
		openedPool = Pool.openPoolLedger(poolName, "{}").get();	
		this.poolName = poolName;
		return openedPool;
	}

	public Pool getOpenedPool() {
		return openedPool;
	}

	public boolean hasOpenedPool() {
		return openedPool != null; 
	}

	public String getPoolName() {
		return poolName;
	}
	
	
	public String getCredDefJson(String submitterDid, String credDefId) throws InterruptedException, ExecutionException, IndyException {
		String request = Ledger.buildGetCredDefRequest(submitterDid, credDefId).get();
		String getCredDefResponse = Ledger.submitRequest(openedPool, request).get();
		return Ledger.parseGetCredDefResponse(getCredDefResponse).get().getObjectJson();
		              
	}

	public void closeOpenedPool() throws InterruptedException, ExecutionException, IndyException {
		if(openedPool != null) {
			openedPool.closePoolLedger().get();	
			openedPool.deletePoolLedgerConfig(poolName).get();
		}
	}


	
	

}
