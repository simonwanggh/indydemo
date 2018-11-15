package com.dxc.ngp.service.vo;

import java.io.Serializable;

public class IssuerVo implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -4149101854649623459L;
	
	private String issuerDid;
	private String schemaId;
	private String schemaJson;
	private String credDefId;
	private String credDefJson;
	private String revRegId;


	public String getIssuerDid() {
		return issuerDid;
	}

	public void setIssuerDid(String issuerDid) {
		this.issuerDid = issuerDid;
	}

	public String getSchemaId() {
		return schemaId;
	}

	public void setSchemaId(String schemaId) {
		this.schemaId = schemaId;
	}

	public String getSchemaJson() {
		return schemaJson;
	}

	public void setSchemaJson(String schemaJson) {
		this.schemaJson = schemaJson;
	}

	public String getCredDefId() {
		return credDefId;
	}

	public void setCredDefId(String credDefId) {
		this.credDefId = credDefId;
	}

	public String getCredDefJson() {
		return credDefJson;
	}

	public void setCredDefJson(String credDefJson) {
		this.credDefJson = credDefJson;
	}

	public String getRevRegId() {
		return revRegId;
	}

	public void setRevRegId(String revRegId) {
		this.revRegId = revRegId;
	}

	
	

}
