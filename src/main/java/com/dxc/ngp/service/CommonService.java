package com.dxc.ngp.service;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;

import org.hyperledger.indy.sdk.IndyException;
import org.hyperledger.indy.sdk.blob_storage.BlobStorageReader;
import org.hyperledger.indy.sdk.ledger.Ledger;
import org.hyperledger.indy.sdk.pool.Pool;
import org.json.JSONObject;

public class CommonService {
	
	private static final String TYPE = "default";
	
	protected String getSchemaJson(Pool pool, String schemaId) throws InterruptedException, ExecutionException, IndyException {
		String requestSchemaJson =  Ledger.buildGetSchemaRequest(null, schemaId).get();
		
		String getSchemaResponse = Ledger.submitRequest(pool, requestSchemaJson).get();
		
		return Ledger.parseGetSchemaResponse(getSchemaResponse).get().getObjectJson();
	}
	
	
	protected String getCredDefJson(Pool pool, String credDefId) throws InterruptedException, ExecutionException, IndyException {
		String requestCredDefJson = Ledger.buildGetCredDefRequest(null, credDefId).get();
		
		String getCredDefResponse =  Ledger.submitRequest(pool, requestCredDefJson).get();
		
		return Ledger.parseGetCredDefResponse(getCredDefResponse).get().getObjectJson();
	}
	
	protected BlobStorageReader getBlobStorageReaderHandle() throws InterruptedException, ExecutionException, IndyException {
		return BlobStorageReader.openReader(TYPE, getTailsWriterConfig()).get();
	}
	
	protected String getTailsWriterConfig() {
		return new JSONObject(String.format("{\"base_dir\":\"%s\", \"uri_pattern\":\"\"}",
				getIndyHomePath("tails")).replace('\\', '/')).toString();
	}

	protected String getIndyHomePath() {
        return System.getProperty("user.home") + "/.indy_client/";
    }

    protected String getIndyHomePath(String filename) {
        return getIndyHomePath() + filename;
    }
    
    
    protected void writeTofile(String fileName, String content) {
    	try {
    		
    		String filePath = getIndyHomePath() + fileName;
    		Path filePathObj = Paths.get(filePath);
    		if(Files.exists(filePathObj)) {
    			Files.delete(filePathObj);
    		}
    		
    		Files.createFile(filePathObj);
    		
    		String saved = content.replaceAll("\\\"", "\\\\\"");
    		
    		try (BufferedWriter writer = Files.newBufferedWriter(filePathObj)) {
    		    writer.write(saved);
    		}
    		
    	}catch(Exception e) {
    		System.out.println(String.format("Write to %s failed", fileName));
    	}
    }

}
