package com.dxc.ngp;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;
import org.hyperledger.indy.sdk.ledger.Ledger;


public class TestSomethimg {
	
	protected String DID = "CnEDk9HrMnmiHXEV1WFgbVCRteYnPqsJwrTdcZaNhFVW";
	protected String SIGNATURE_TYPE = "CL";
	protected String TAG = "tag1";
	
	
	@Test
	public void replaceTest() {
		String a = "\"";
		String b = a.replaceAll("\\\"", "\\\\\"");
		System.out.println(b);
	}
	
//	@Test
//	public void testBuildGetCredDefRequestWorks() throws Exception {
//		int seqNo = 1;
//		String id = DID + ":3:" + SIGNATURE_TYPE + ":" + seqNo + ":" + TAG;
//		DID = "Th7MpTaRZVRYnPiabds81Y";
//		id = "Th7MpTaRZVRYnPiabds81Y:3:CL:1:Tag1";
//		String expectedResult = String.format("\"identifier\":\"%s\"," +
//				"\"operation\":{" +
//				"\"type\":\"108\"," +
//				"\"ref\":%d," +
//				"\"signature_type\":\"%s\"," +
//				"\"origin\":\"%s\"," +
//				"\"tag\":\"%s\"" +
//				"}", DID, seqNo, SIGNATURE_TYPE, DID, TAG);
//
//		String getCredDefRequest = Ledger.buildGetCredDefRequest(DID, id).get();
//
//		//assertTrue(getCredDefRequest.replace("\\", "").contains(expectedResult));
//	}

	@Test
	public void testCreateFile() throws IOException {
		
		prepareFile();
		
		String fullPath = System.getProperty("user.home")+ "/.indy_client/demostore/simon/verifyrequst.json";	
		
		Path path = Paths.get(fullPath);
		
		boolean dirExists = Files.exists(path);
		
		if(!dirExists) {
			Files.createFile(path);
		}
	}

	private void prepareFile() throws IOException {
		String strorePath = System.getProperty("user.home")+ "/.indy_client/demostore";
		Path spath = Paths.get(strorePath);
		
		if(!Files.exists(spath)) {
			Files.createDirectories(spath);
		}
		
		String userPath = strorePath + "/simon";
		
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


}
