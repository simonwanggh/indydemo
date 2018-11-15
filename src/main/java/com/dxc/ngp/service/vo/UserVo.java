package com.dxc.ngp.service.vo;

import java.io.Serializable;
import java.util.HashMap;

public class UserVo  implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -207118170446174553L;
	
	
	private String userDid;
	
	private HashMap<String, String> userMasterSecretMap = new HashMap<String, String>();

	public String getUserDid() {
		return userDid;
	}

	public void setUserDid(String userDid) {
		this.userDid = userDid;
	}

	public String getMasterSecret(String userId) {		
		return userMasterSecretMap.get(userId);
	}

	public void putMasterSecret(String userId, String masterSecret) {
		userMasterSecretMap.put(userId, masterSecret);
		
	}
	
	

}
