package it.unipi.di.lai.groot.lai16XacmlRequestGeneratorWS;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.Provider;
import javax.xml.ws.Service;
import javax.xml.ws.ServiceMode;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.WebServiceProvider;
import javax.xml.ws.handler.MessageContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import it.cnr.isti.sedc.xcreate.xacml3.lai16.starter.XCREATE4Lai16XacmlReqGenWS;
import it.unipi.di.lai.groot.database.ConnectionPool;
import it.unipi.di.lai.groot.utils.Lai16XacmlConstants;
import it.unipi.di.lai.groot.utils.XacmlWsConstants;
import it.unipi.di.lai.utils.DomUtils;

@WebServiceProvider(
		portName="XacmlRequestsGeneratorWSDLFileSOAP",
		serviceName="XacmlRequestsGeneratorWSDLFile",
		targetNamespace="http://di.unipi.it/lai/XacmlRequestsGeneratorWSDLFile")

//,\WEB-INF\wsdl\XacmlRequestsGeneratorWSDLFile.wsdl
//		wsdlLocation="WEB-INF/wsdl/XacmlRequestsGeneratorWSDLFile.wsdl")
@ServiceMode(value = Service.Mode.PAYLOAD)

public class XacmlRequestsGenerator implements Provider<Source> {
	
	private static final Logger LOG = Logger.getLogger(XacmlRequestsGenerator.class.getName());
   
   	private Document doc;
	private DocumentBuilderFactory dbFactory;
	private Element xacmlRequestDOM;
	
	
	public XacmlRequestsGenerator() {
		// TODO Auto-generated constructor stub
	}

	// Injection del contesto del messaggio.
	// Serve per recuperare i dati del servizio e del messaggio
	@Resource
	WebServiceContext wsCtx;
	private String tempDirPath;

	public Source invoke(Source source) {
		try {
			LOG.info(XacmlRequestsGenerator.class.getName()+" Received a request.");
			// generate Document for response
			generateDocument();
			
			// Costruisco il DOM del contenuto del BODY (PAYLOAD)
			DOMResult dom = new DOMResult();
			Transformer trans = TransformerFactory.newInstance().newTransformer();
			trans.transform(source, dom);

			// Recupero l'operazione richiesta dall'header HTTP SOAPAction
			Map<?, ?> httpHeadersMap = (Map<?, ?>) wsCtx.getMessageContext().get(MessageContext.HTTP_REQUEST_HEADERS);
			String operation = (String) ((List<?>) httpHeadersMap.get("SOAPAction")).get(0);
			
			LOG.info(XacmlRequestsGenerator.class.getName()+" Operation Name: "+operation);
			
			String reponseMsg;
			switch (operation) {
			case Lai16XacmlConstants.GetXacmlPoliciesOp:
				reponseMsg = getXacmlPolicies();
				break;
			case Lai16XacmlConstants.AddXacmlPolicyOp:
				reponseMsg = adXacmlPolicyAndcreateXacmlRequests(dom.getNode());			
				break;
				
			case Lai16XacmlConstants.GetXacmlRequestsOP:
				reponseMsg = getXacmlRequests(dom);
				break;
				
			default:
				reponseMsg = getErrorMessage();				
				break;
			}
			
			return new StreamSource(new ByteArrayInputStream(reponseMsg.getBytes()));

		} catch (TransformerException e) {
			throw new RuntimeException("Errore nella trasformazione dell'XML in DOM", e);
		} 
	}
	
	/*
	 * TODO :: COSTRUIRE I MESSAGGIO DI ERRORE DA SPEDIRE AL CLIENT ::: 
	 */
	private void sendSOAPFaultMessage(String message, HttpServletResponse response) {

		response.setContentType("text/xml");
		MessageFactory mf;
		SOAPMessage soapRes = null;

		// spedisco il messaggio di errore
		try {
			mf = MessageFactory.newInstance();
			soapRes = mf.createMessage();
			SOAPFault soapFault = soapRes.getSOAPBody().addFault();

			soapFault.setFaultString(message);
			soapRes.writeTo(response.getOutputStream());

		} catch (Exception e) {
			// chiamata ricorsiva, il client ha il timeout
			sendSOAPFaultMessage(e.getMessage(), response);
		}
	}
	
	
	private String getErrorMessage() {
		// TODO Auto-generated method stub
		return "<XacmlError dirName= \"" +tempDirPath+"\">Unknown Operation</XacmlError>";
	}
	/**
	 * Recupera i dati relativi alle Xacml Policies presenti nel database:
	 * 	Restituisce l'elenco delle politiche presenti:
	 * 		PK_XacmlPolicy
	 * 		XacmlPolicyName
	 * @return
	 */
	private String getXacmlPolicies(){
		
		
		LOG.info("\nExecuting operation :: "+Lai16XacmlConstants.GetXacmlPoliciesOp);
        
		// create root Response
		Element getXacmlPoliciesResp = generateElement(Lai16XacmlConstants.GetXacmlPoliciesResponse);
		// create XacmlPoliciesList
		Element xacmlPoliciesList = generateElement(XacmlWsConstants.XACML_POLICIES_LIST);
		// append XacmlPoliciesList 
		getXacmlPoliciesResp.appendChild(xacmlPoliciesList);
		
        final String SQLstmt =
                "SELECT " +XacmlWsConstants.PK_XacmlPolicy+", "+XacmlWsConstants.XacmlPolicyName+
                " FROM "+ XacmlWsConstants.XacmlPolicies+
                " ORDER BY "+XacmlWsConstants.PK_XacmlPolicy+";";
        
        Connection dbConn = null;
        
        ResultSet rs = null;
        PreparedStatement pstmt = null;
        
        try {
        	// prelevo una connessione dalla coda di connessioni del datasource
        	dbConn = ConnectionPool.getConnection();
        	
        	pstmt = (PreparedStatement) dbConn.prepareStatement(SQLstmt);
            rs = pstmt.executeQuery();
                        
            while (rs.next()) {
            	int pkPolicy = rs.getInt(XacmlWsConstants.PK_XacmlPolicy);
            	String policyName = rs.getString(XacmlWsConstants.XacmlPolicyName);
            	// create a XacmlPolicyNode
            	Element xacmlPolicy = generateElement(XacmlWsConstants.XACML_POLICY);
            	Element xacmlPolicyID = generateElement(XacmlWsConstants.XACML_POLICY_ID);
            	xacmlPolicyID.setTextContent(String.valueOf(pkPolicy));
            	
            	Element xacmlPolicyName = generateElement(XacmlWsConstants.XACML_POLICY_NAME);
            	xacmlPolicyName.setTextContent(policyName);
            	
            	xacmlPolicy.appendChild(xacmlPolicyName);
            	xacmlPolicy.appendChild(xacmlPolicyID);
            	// add created XacmlPolicyNode to the XacmlPolicyList
            	xacmlPoliciesList.appendChild(xacmlPolicy);
            		
            }
            
        } catch (Exception ex) {
        	// per ora codifico l'errore come caso 
        	// particolare nella lista degli stock
        	return "<XacmlError>Error occurs during "+Lai16XacmlConstants.GetXacmlPoliciesOp+" Operation "
        			+ ":: "+ex.toString()
        			+ " :: </XacmlError>";
        }
        finally{
        	// libero le risorse e la connessione
        	try {
				if(dbConn!=null)
					try{ dbConn.close(); } catch(Exception ignore) { }
				if(rs!=null) 
					rs.close();
	        	if(pstmt!=null) 
	        		pstmt.close();
	        	
			} catch (SQLException e) { }
        }
		
//        doc.appendChild(xacmlPoliciesList);
        doc.appendChild(getXacmlPoliciesResp);
        
        return operationResult();
	}
	
	
	/**
	 * insert xacmlPolity into database
	 * create xacmlRequests associated with the current xacmlPolicy
	 * @param payload
	 * @return
	 */
	private String adXacmlPolicyAndcreateXacmlRequests(Node payload){
		
//		String xacmlPOlicyNameNew = getNodeContentByName(payload, XacmlWsConstants.XacmlPolicyName);
//		LOG.info("xacmlPOlicyNameNew :: \n\n"+xacmlPOlicyNameNew);
		
		LOG.info("NodeName :: "+payload.getNodeName());
		LOG.info("Content PAYLOAD :: \n\n"+getNodeAsString(payload));
		
		String xacmlPolicyName = getNodeContentByName(payload, XacmlWsConstants.XacmlPolicyName);
		String xacmlPolicyContent = null;
		
		int currentPkPolicy = -1;
		
		
		Node xacmlPolicyNode = payload.getChildNodes().item(0).getChildNodes().item(0);
		
		NodeList children = xacmlPolicyNode.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if(child.getNodeType() == Node.ELEMENT_NODE){
				LOG.info("\nNode Name :: "+child.getNodeName());
				switch (child.getNodeName()) {
				case XacmlWsConstants.XacmlPolicyName:
					xacmlPolicyName = child.getTextContent();
					break;
				case XacmlWsConstants.POLICY:
				case XacmlWsConstants.POLICY_SET:
					xacmlPolicyContent = getNodeAsString(child);
					break;
					
				default:
					break;
				}
			}
		}
		
		// add code
		LOG.info("XacmlPolicyName :: "+xacmlPolicyName);
		LOG.info("XacmlPolicyContent :: \n"+xacmlPolicyContent);
		
		// test cases generation	
		HashMap<String, String> xacmlRequests = generateXacmlRequests(xacmlPolicyContent, xacmlPolicyName);

		if(xacmlRequests == null){
			return getErrorMessage("Problemi durante la generazione delle richieste per la XacmlPoicy: "+xacmlPolicyName);
		}
		
		// TODO check available Values, Or XacmlPolicyName and PolicyContent
		
		Element addXacmlPolicyResponse = generateElement(Lai16XacmlConstants.AddXacmlPolicyResponse);
		Element xacmlPolicy = generateElement(XacmlWsConstants.XACML_POLICY_ID);
		addXacmlPolicyResponse.appendChild(xacmlPolicy);
		
		String SQLstmtInsertXacmlPolicy = 
				"INSERT INTO "+XacmlWsConstants.XacmlPolicies+" ("
						+ XacmlWsConstants.XacmlPolicyName+", "+XacmlWsConstants.XacmlPolicyContent+") "
						+ "VALUES ((?),(?));";
		

        
        Connection dbConn = null;
        
        ResultSet rs = null;
        PreparedStatement pstmt = null;
        PreparedStatement pstmt4MaxPk = null;
        PreparedStatement preStmt = null;
        try {
        	
        	// prelevo una connessione dalla coda di connessioni del datasource
        	dbConn = ConnectionPool.getConnection();
        	// livello di isolamento SERIALIZABLE
        	try {
        		dbConn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
        	} catch(SQLException e){
        		// dovuto ad un problema di gestione dei pool di connessione in MySQL:
        		// in questo caso devo riprovare l'operazione
        		this.adXacmlPolicyAndcreateXacmlRequests(payload);
        	}
        	
        	dbConn.setAutoCommit(false);
        	
        	
        	// preparo lo statement
        	pstmt = (PreparedStatement) dbConn.prepareStatement(SQLstmtInsertXacmlPolicy);
        	// values setting
        	pstmt.setString(1, xacmlPolicyName);
        	pstmt.setString(2, xacmlPolicyContent);
        	// execute query :: insert xacmlPolicy into database
			if(pstmt.executeUpdate() != 1)
				throw new java.lang.Exception("Problemi durante l'inserimento della Xacml Policy: "+xacmlPolicyName);
        	
			
			// check database content
			// recupero della chiave primaria della policy appena inserita
			
	        final String SQLstmt =
	                "SELECT MAX(" +XacmlWsConstants.PK_XacmlPolicy+") as PkCurrentPolicy "+
	                " FROM "+ XacmlWsConstants.XacmlPolicies+";";
	        pstmt4MaxPk = (PreparedStatement) dbConn.prepareStatement(SQLstmt);
	        	
	            rs = pstmt4MaxPk.executeQuery();
	                        
	            while (rs.next()) {
	            	currentPkPolicy = rs.getInt("PkCurrentPolicy");
	            	xacmlPolicy.setTextContent(String.valueOf(currentPkPolicy));
	            }
	            
	            // test cases insertion
	            String SQLstmtInsertXacmlRequest = 
	    				"INSERT INTO "+XacmlWsConstants.XacmlRequests+" ("
	    						+ XacmlWsConstants.XacmlRequestName+", "+XacmlWsConstants.XacmlRequestContent+", "+
	    						XacmlWsConstants.FK_XacmlPolicy+") "
	    						+ "VALUES ((?),(?),(?));";
	            
	            
	            preStmt = (PreparedStatement) dbConn.prepareStatement(SQLstmtInsertXacmlRequest);
	        	
	            Set<String> xacmlRequestIDs = xacmlRequests.keySet();
	            
	            for (String xacmlReqID : xacmlRequestIDs) {
	            	// values setting
	            	preStmt.setString(1, xacmlReqID);
	            	preStmt.setString(2, xacmlRequests.get(xacmlReqID));
	            	preStmt.setInt(3, currentPkPolicy);
	            	preStmt.addBatch();
				}
	            preStmt.executeBatch();
					            
			// commit
			dbConn.commit();
			
        } catch (Exception ex) {
        	
        	try {
            	// eseguo il rollback
				dbConn.rollback();
			} catch (SQLException e) { }
        	// per ora codifico l'errore come caso  particolare
        	return "<XacmlError>Error occurs during "+XacmlWsConstants.OPERATION_CREATE_XACML_REQUESTS+" Operation "
        			+ ":: "+ex.toString()
        			+ " :: </XacmlError>";
        }
        
        
        finally{
        	// libero le risorse e la connessione
        	try {
				if(dbConn!=null){
					try{ dbConn.close(); } catch(Exception ignore) { }
				}
				if(rs!=null){
					rs.close();
				}
	        	if(pstmt!=null){ 
	        		pstmt.close();
	        	}
	        	if(pstmt4MaxPk != null){
	        		pstmt4MaxPk.close();
	        	}
	        	if(preStmt!= null){
	        		preStmt.close();
	        	
	        	}
	        	
			} catch (SQLException e) { }
        }
        
//        doc.appendChild(xacmlPolicy);
        doc.appendChild(addXacmlPolicyResponse);
        return operationResult();
	}	
	
	
	private String getNodeContentByName(Node payload, String nodeName) {
		// TODO Auto-generated method stub
		
		String xacmlPolicyIdAsString = null;
		try {
			XPath xPath = XPathFactory.newInstance().newXPath();
			xacmlPolicyIdAsString = xPath.compile("//*[contains(local-name(), '"+nodeName+"')]").evaluate(payload);
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return xacmlPolicyIdAsString;
	}


	private String getErrorMessage(String errorMsg) {
		// TODO Auto-generated method stub
		return "<XacmlError>"
    			+ "::\n "+errorMsg
    			+ " \n:: </XacmlError>";
	}

	private HashMap<String, String> generateXacmlRequests(String xacmlPolicyContent, String xacmlPolicyName) {
		// TODO Auto-generated method stub
		try {
			
			System.out.println((new File(".").getAbsolutePath()));
			LOG.info("WorkingDir :: "+(new File(".").getAbsolutePath()));
			tempDirPath = System.getProperty("java.io.tmpdir");
		
			System.out.println(tempDirPath);
		
			LOG.info("TempDir :: "+tempDirPath);
			File currentTmpDir = new File(tempDirPath);
			File xacmlPolicyFile = File.createTempFile("XacmlPolicy_", "_"+xacmlPolicyName, currentTmpDir);
		
			BufferedWriter bw = new BufferedWriter(new FileWriter(xacmlPolicyFile));
			bw.write(xacmlPolicyContent);
			bw.close();

			System.out.println("Done");
			
			XCREATE4Lai16XacmlReqGenWS requestsGeneratorWS = new XCREATE4Lai16XacmlReqGenWS(xacmlPolicyFile.getAbsolutePath(), currentTmpDir.getAbsolutePath());
			return requestsGeneratorWS.creatRequests();
		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;	
				
				
	}

	/*
	 * <complexType name="XacmlRequestType">
		<sequence>
			<element name="XacmlRequestName" type="string"/>
			<element ref="xacmlGen:XacmlRequestID"/>
			<element ref="xacml:Request" minOccurs="0"/>
		</sequence>
		<attribute name="xacmlPolicyID" type="integer"/>
	</complexType>
	 */
	private String getXacmlRequests(DOMResult dom){
		LOG.info("Executing operation GetXacmlRequests");
        
		
		Element getXacmlRequestsResponse = generateElement(Lai16XacmlConstants.GetXacmlRequestsResponse);
		
		Element xacmlRequestsList = generateElement(XacmlWsConstants.XACML_REQUESTS_LIST);
		getXacmlRequestsResponse.appendChild(xacmlRequestsList);
        Connection dbConn = null;
        
        ResultSet rs = null;
        PreparedStatement pstmt = null;
        
        try {
        	// Recupero il valore di XacmlPolicyID dal dom dell'xml utilizzando il linguaggio XPath
    		XPath xPath = XPathFactory.newInstance().newXPath();
    		String xacmlPolicyIdAsString = xPath.compile("//*[contains(local-name(), '"+Lai16XacmlConstants.XacmlPolicyID+"')]").evaluate(dom.getNode());
    		
    		System.out.println("Richiesta lista Xacml Requests asociate alla Xacml Policy: " + xacmlPolicyIdAsString);

        	String SQLstmt =
            		"SELECT " +XacmlWsConstants.PK_XacmlRequest+", "+XacmlWsConstants.XacmlRequestName+", "+XacmlWsConstants.FK_XacmlPolicy+ 
            		" FROM "+XacmlWsConstants.XacmlRequests+
            		" WHERE "+XacmlWsConstants.FK_XacmlPolicy+" = ?;";
            
            // prelevo una connessione dalla coda di connessioni del datasource
        	dbConn = ConnectionPool.getConnection();
        	
        	pstmt = (PreparedStatement) dbConn.prepareStatement(SQLstmt);
        	// setto il valore del PK_XacmlPolicy
        	pstmt.setInt(1, Integer.valueOf(xacmlPolicyIdAsString));;
            
        	rs = pstmt.executeQuery();
                        
            while (rs.next()) {
            	int pkRequest = rs.getInt(XacmlWsConstants.PK_XacmlRequest);
            	String requestName = rs.getString(XacmlWsConstants.XacmlRequestName);
            	int fkPolicy = rs.getInt(XacmlWsConstants.FK_XacmlPolicy);
            	/*
            	 * <complexType name="XacmlRequestType">
            		<sequence>
            			<element name="XacmlRequestName" type="string"/>
            			<element ref="xacmlGen:XacmlRequestID"/>
            			<element ref="xacml:Request" minOccurs="0"/>
            		</sequence>
            		<attribute name="xacmlPolicyID" type="integer"/>
            	</complexType>
            	 */
            	// create a XacmlRequestNode
            	Element xacmlRequest = generateElement(XacmlWsConstants.XACML_REQUEST);
            	// set Attribute XacmlPolicyID
            	xacmlRequest.setAttribute(XacmlWsConstants.XACML_POLICY_ID, String.valueOf(fkPolicy));
            	// set Request Name
            	Element xacmlRequestName = generateElement(XacmlWsConstants.XacmlRequestName);
            	xacmlRequestName.setTextContent(requestName);
            	// set Request ID
            	Element xacmlRequestId = generateElement(XacmlWsConstants.XACML_REQUEST_ID);
            	xacmlRequestId.setTextContent(String.valueOf(pkRequest));
            	
            	xacmlRequest.appendChild(xacmlRequestName);
            	xacmlRequest.appendChild(xacmlRequestId);
            	// add created XacmlPolicyNode to the XacmlPolicyList
            	xacmlRequestsList.appendChild(xacmlRequest);
            }
        } catch (Exception ex) {
        	
        	// per ora codifico l'errore come caso 
        	// particolare 
        	return "<XacmlError>Error occurs during "+XacmlWsConstants.OPERATION_GET_XACML_REQUESTS+" Operation "
        			+ "::\n "+getExceptionAsString(ex)
        			+ " \n:: </XacmlError>";
        }
        finally{
        	// libero le risorse e la connessione
        	try {
				if(dbConn!=null)
					try{ dbConn.close(); } catch(Exception ignore) { }
				if(rs!=null) 
					rs.close();
	        	if(pstmt!=null) 
	        		pstmt.close();
	        	
			} catch (SQLException e) { }
        }
		
//        doc.appendChild(xacmlRequestsList);
        doc.appendChild(getXacmlRequestsResponse);
        return operationResult();
	}
	
	
	
	
	private String getExceptionAsString(Exception ex) {
		// TODO Auto-generated method stub
		StringWriter sw = new StringWriter();
    	PrintWriter pw = new PrintWriter(sw);
    	ex.printStackTrace(pw);
    	
		return sw.toString();
	}


	public void getTempDir() {
		try {
			// create a temp file
			File temp = File.createTempFile("temp-file-name", ".tmp");
			
			System.out.println("Temp file : " + temp.getAbsolutePath());
			
			// Get tempropary file path
			String absolutePath = temp.getAbsolutePath();
			String tempFilePath = absolutePath.substring(0, absolutePath.lastIndexOf(File.separator));
			
			System.out.println("Temp file path : " + tempFilePath);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	private void generateDocument(){
		// TODO Auto-generated method stub
		try {
			dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder documentBuilder;
			
			documentBuilder = dbFactory.newDocumentBuilder();
			
			doc = documentBuilder.newDocument();
			
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private Element generateElement(String elementName){
		Element element = doc.createElement(elementName);
		return element;
	}
	
	private String operationResult() {
		// TODO Auto-generated method stub
		try {
			DomUtils domUtils;
			domUtils = new DomUtils();
		
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			domUtils.serialize(doc, baos);
			System.out.println(baos.toString());
			return baos.toString();
		} catch (TransformerConfigurationException e) {
			// TODO Auto-generated catch block
			return getErrorMessage();
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			return getErrorMessage();
		}
	}
	
	private String getNodeAsString(Node currentNode) {
		// TODO Auto-generated method stub
		StringWriter sw = new StringWriter();
		try {
			Transformer t = TransformerFactory.newInstance().newTransformer();
			t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			t.transform(new DOMSource(currentNode), new StreamResult(sw));
		} catch (TransformerException te) {
			System.out.println("nodeToString Transformer Exception");
		}
		return sw.toString();

	}
}
