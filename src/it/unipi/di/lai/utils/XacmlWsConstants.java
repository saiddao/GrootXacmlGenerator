package it.unipi.di.lai.utils;

public class XacmlWsConstants {
	// POLICY ELEMENTS NAME
	public static final String XACML_POLICY = "XacmlPolicy";
	public static final String XACML_POLICY_NAME = "XacmlPolicyName";
	public static final String POLICY = "Policy";
	public static final String POLICY_SET = "PolicySet";
	public static final String XACML_POLICIES_LIST = "XacmlPoliciesList";
	
	public static final String XACML_POLICY_ID = "xacmlPolicyID";
	
	// REQUEST ELEMENTS NAME
	public static final String XACML_REQUEST = "XacmlRequest";
	
	public static final String XACML_REQUEST_NAME = "XacmlRequestName";
	public static final String XACML_REQUEST_ID = "XacmlRequestID";
	public static final String REQUEST = "Request";
	public static final String XACML_REQUESTS_LIST = "XacmlRequestsList";
	
	
	// OPERATIONS NAME 
	public static final String OPERATION_GET_XACML_POLICIES = "GetXacmlPolicies";
	public static final String OPERATION_CREATE_XACML_REQUESTS = "CreateXacmlRequests";
	public static final String OPERATION_GET_XACML_REQUESTS = "GetXacmlRequests";
	
	// ERROR
	public static final String XACML_ERROR = "XacmlError";
	
	
	// DATABASE 
	public static final String DATABASE_NAME = "lai16xacmltestingdb";
	// XacmlPolicies Table
	public static final String XacmlPolicies = "XacmlPolicies";
	public static final String PK_XacmlPolicy = "PK_XacmlPolicy";
	public static final String XacmlPolicyName = "XacmlPolicyName";
	public static final String XacmlPolicyContent = "XacmlPolicyContent";
	
	// XacmlRequests Table
	public static final String XacmlRequests = "XacmlRequests";
	public static final String PK_XacmlRequest = "PK_XacmlRequest";
	public static final String XacmlRequestName = "XacmlRequestName";
	public static final String XacmlRequestContent = "XacmlRequestContent";
	public static final String FK_XacmlPolicy = "FK_XacmlPolicy";
	
	// XacmlDecisions Table 
	public static final String XacmlDecisions = "XacmlDecisions";
	public static final String PK_XacmlDecision = "PK_XacmlDecision";
	public static final String XacmlDecisionValue = "XacmlDecisionValue";
	
	// XacmlExecutions Table
	public static final String XacmlExecutions = "XacmlExecutions";
	public static final String PK_XacmlExecution = "PK_XacmlExecution";
	public static final String FK_XacmlDecision = "FK_XacmlDecision";
	public static final String FK_XacmlRequest = "FK_XacmlRequest";
//	public static final String FK_XacmlPolicy = "FK_XacmlPolicy";	
	
}
