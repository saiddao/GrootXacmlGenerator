package it.unipi.di.lai.lai16XacmlRequestGeneratorWS;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
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
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import it.unipi.di.lai.database.ConnectionPool;
import it.unipi.di.lai.utils.DomUtils;
import it.unipi.di.lai.utils.StreamUtils;
import it.unipi.di.lai.utils.XacmlWsConstants;



/*
 * Nome: XACML Request Generator
	Operazioni:
		•	createRequests()
			o	Parametri:
					Input:		XacmlPolicy
					Output:	IdXacmlPolicy
		•	getRequests()
			o	Parametri
					Input:		IdXacmlPolicy
					Output:	XacmlRequestsList
		•	getXacmlPolicies()
			o	Parametri
					Input:		
					Output:	XacmlPoliciesList

 */

@WebServiceProvider(
		portName="XacmlRequestsGeneratorWSDLFileSOAP",
		serviceName="XacmlRequestsGeneratorWSDLFile",
		targetNamespace="http://di.unipi.it/lai/XacmlRequestsGeneratorWSDLFile")
//,
//		wsdlLocation="WEB-INF/wsdl/XacmlRequestsGeneratorWSDLFile.wsdl")
@ServiceMode(value = Service.Mode.PAYLOAD)

public class XacmlRequestsGenerator_Initial implements Provider<Source> {
	
	private static final Logger LOG = Logger.getLogger(XacmlRequestsGenerator_Initial.class.getName());
   
   	private Document doc;
	private DocumentBuilderFactory dbFactory;
	private Element xacmlRequestDOM;
	
	
	public XacmlRequestsGenerator_Initial() {
		// TODO Auto-generated constructor stub
	}

	// Injection del contesto del messaggio.
	// Serve per recuperare i dati del servizio e del messaggio
	@Resource
	WebServiceContext wsCtx;
	private String tempDirPath;

	public Source invoke(Source source) {
		try {
			LOG.info(XacmlRequestsGenerator_Initial.class.getName()+" Received a request.");
			// generate Document for response
			generateDocument();
			
			
			
			tempDirPath = System.getProperty("java.io.tmpdir");
			
			System.out.println(tempDirPath);
			
			LOG.info("TempDir :: "+tempDirPath);
			
			// Costruisco il DOM del contenuto del BODY (PAYLOAD)
			DOMResult dom = new DOMResult();
			Transformer trans = TransformerFactory.newInstance().newTransformer();
			trans.transform(source, dom);

			// Recupero l'operazione richiesta dall'header HTTP SOAPAction
			Map<?, ?> httpHeadersMap = (Map<?, ?>) wsCtx.getMessageContext().get(MessageContext.HTTP_REQUEST_HEADERS);
			String operation = (String) ((List<?>) httpHeadersMap.get("SOAPAction")).get(0);
			
			LOG.info(XacmlRequestsGenerator_Initial.class.getName()+" Operation Name: "+operation);
			
			String reponseMsg;
			switch (operation) {
			case XacmlWsConstants.OPERATION_GET_XACML_POLICIES:
				reponseMsg = getXacmlPolicies();
				break;
			case XacmlWsConstants.OPERATION_CREATE_XACML_REQUESTS:
				reponseMsg = createXacmlRequests(dom.getNode());			
				break;
				
			case XacmlWsConstants.OPERATION_GET_XACML_REQUESTS:
				reponseMsg = getXacmlRequests(dom);
				break;
	

			default:
				reponseMsg = getErrorMessage();				
				break;
			}
			
			return new StreamSource(new ByteArrayInputStream(reponseMsg.getBytes()));
			
			
//			if (operation.equals("mostraOrdini")) {
//				// Operazione mostraOrdini.
//				// Recupero il valore del codice fiscale dal dom dell'xml
//				// E' possibile farlo usando le tecniche di navigazione del DOM
//				// gi? viste
//				// Qui lo facciamo utilizzando il linguaggio XPath
//				XPath xPath = XPathFactory.newInstance().newXPath();
//				String codFisc = xPath.compile("//*[contains(local-name(), 'codfisc')]").evaluate(dom.getNode());
//				
//				System.out.println("Richiesta lista ordini per il codice fiscale " + codFisc);
//				
//				// Costruisco il messaggio di risposta.
//				return new StreamSource(new ByteArrayInputStream(
//						"<listaOrdini xmlns=\"http://www.negozio.org/\"><idOrdine>1000</idOrdine></listaOrdini>"
//								.getBytes()));
//
//			} else {
//				throw new RuntimeException("Operazione sconosciuta");
//			}
		} catch (TransformerException e) {
			throw new RuntimeException("Errore nella trasformazione dell'XML in DOM", e);
		} 
//		catch (XPathExpressionException e) {
//			throw new RuntimeException("Errore nella ricerca XPath", e);
//		}
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
		
		
		LOG.info("Executing operation GetXacmlPolicies");
        
		Element xacmlPoliciesList = generateElement(XacmlWsConstants.XACML_POLICIES_LIST);
		
		
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
        	return "<XacmlError>Error occurs during "+XacmlWsConstants.OPERATION_GET_XACML_POLICIES+" Operation "
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
		
        doc.appendChild(xacmlPoliciesList);
        
        return operationResult();
		
		
       
	
		
//		return "<XacmlPoliciesList xmlns=\"http://www.negozio.org/\"><XacmlPolicyID>1000</XacmlPolicyID></XacmlPoliciesList>";
	}
	
	
	/**
	 * insert xacmlPolity into database
	 * create xacmlRequests associated with the current xacmlPolicy
	 * @param payload
	 * @return
	 */
	private String createXacmlRequests(Node payload){
		
//		int result = insertXacmlPolicy();
//		if(result == -1){
//			insertXacmlPolicy();
//		}
		
		
		
		LOG.info("NodeName :: "+payload.getNodeName());
		
		String xacmlPolicyName = null;
		String xacmlPolicyContent = null;
		
		int currentPkPolicy = -1;
		
		
		getNode(XacmlWsConstants.XacmlPolicyName, payload);
		
		Node xacmlPolicyNode = payload.getChildNodes().item(0);
		
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
		
		// TODO check available Values, Or XacmlPolicyName and PolicyContent
		
		Element xacmlPolicy = generateElement(XacmlWsConstants.XACML_POLICY_ID);
		
		String SQLstmtInsertXacmlPolicy = 
				"INSERT INTO "+XacmlWsConstants.XacmlPolicies+" ("
						+ XacmlWsConstants.XacmlPolicyName+", "+XacmlWsConstants.XacmlPolicyContent+") "
						+ "VALUES ((?),(?));";
		

        
        Connection dbConn = null;
        
        ResultSet rs = null;
        PreparedStatement pstmt = null;
        PreparedStatement pstmt4MaxPk = null;
        
        try {
        	
        	// prelevo una connessione dalla coda di connessioni del datasource
        	dbConn = ConnectionPool.getConnection();
        	// livello di isolamento SERIALIZABLE
        	try {
        		dbConn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
        	} catch(SQLException e){
        		// dovuto ad un problema di gestione dei pool di connessione in MySQL:
        		// in questo caso devo riprovare l'operazione
        		this.createXacmlRequests(payload);
        	}
        	
        	dbConn.setAutoCommit(false);
        	
        	
        	// preparo lo statement
        	pstmt = (PreparedStatement) dbConn.prepareStatement(SQLstmtInsertXacmlPolicy);
        	// values setting
        	pstmt.setString(1, xacmlPolicyName);
        	pstmt.setString(2, xacmlPolicyContent);
        	// execute query
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
	        	
			} catch (SQLException e) { }
        }
		
//        doc.appendChild(xacmlPoliciesList);
        doc.appendChild(xacmlPolicy);
        return operationResult();
//		return "<XacmlPolicyID>1000</XacmlPolicyID>";
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
        
		Element xacmlRequestsList = generateElement(XacmlWsConstants.XACML_REQUESTS_LIST);
		
        Connection dbConn = null;
        
        ResultSet rs = null;
        PreparedStatement pstmt = null;
        
        try {
        	// Recupero il valore di XacmlPolicyID dal dom dell'xml utilizzando il linguaggio XPath
    		XPath xPath = XPathFactory.newInstance().newXPath();
    		String xacmlPolicyIdAsString = xPath.compile("//*[contains(local-name(), '"+XacmlWsConstants.XACML_POLICY_ID+"')]").evaluate(dom.getNode());
    		
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
        	// particolare nella lista degli stock
        	return "<XacmlError>Error occurs during "+XacmlWsConstants.OPERATION_GET_XACML_POLICIES+" Operation "
        			+ "::\n "+ex.toString()
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
		
        doc.appendChild(xacmlRequestsList);
        
        return operationResult();
        
//		// Costruisco il messaggio di risposta.
//		return new StreamSource(new ByteArrayInputStream(
//				"<listaOrdini xmlns=\"http://www.negozio.org/\"><idOrdine>1000</idOrdine></listaOrdini>"
//						.getBytes()));
//		
//		
//		return "<XacmlRequestsList xmlns=\"http://www.negozio.org/\">\n	<XacmlRequest xacmlPolicyID=\"1000\">"
//				+ "\n		<XacmlRequestName>XacmlReq_1</XacmlRequestName>"
//				+ "\n		<XacmlRequestID>20</XacmlRequestID>"
//				+ "\n	</XacmlRequest>\n</XacmlRequestsList>";
	}
	
	/*
	 * C:\LAI16\tools>curl.exe -v -H "Content-Type:text/xml" -H  "SOAPAction:GetXacmlPolicies" -X POST -v -T request.soap.v3.xml "http://localhost:8080/Lai16XacmlRequestsGeneratorWS/services/XacmlRequestsGenerator"
	 */
	
	
	
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
		
//		xacmlRequestDOM = doc.createElementNS("urn:oasis:names:tc:xacml:3.0:core:schema:wd-17", XacmlConstants.Request);
//		xacmlRequestDOM.setAttribute(XacmlConstants.CombinedDecision, "false");
//		xacmlRequestDOM.setAttribute(XacmlConstants.ReturnPolicyIdList, "false");
//		doc.appendChild(xacmlRequestDOM);
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
	
	
	private void getNode(String xacmlpolicyname, Node dom) {
		// TODO Auto-generated method stub
//		if (operation.equals("mostraOrdini")) {
//		// Operazione mostraOrdini.
//		// Recupero il valore del codice fiscale dal dom dell'xml
//		// E' possibile farlo usando le tecniche di navigazione del DOM
//		// gi? viste
//		// Qui lo facciamo utilizzando il linguaggio XPath
//		XPath xPath = XPathFactory.newInstance().newXPath();
//		String codFisc = xPath.compile("//*[contains(local-name(), '"+xacmlpolicyname+"')]").evaluate(dom);
//		
//		System.out.println("Richiesta lista ordini per il codice fiscale " + codFisc);
//		
//		// Costruisco il messaggio di risposta.
//		return new StreamSource(new ByteArrayInputStream(
//				"<listaOrdini xmlns=\"http://www.negozio.org/\"><idOrdine>1000</idOrdine></listaOrdini>"
//						.getBytes()));
//
//	} else {
//		throw new RuntimeException("Operazione sconosciuta");
//	}
	}
	
	

	/*
	 * C:\LAI16\tools>curl.exe -v -H "Content-Type:text/xml" -H  "SOAPAction:CreateXacmlRequests" -X POST -v -T CreateXacmlRequests.xml "http://localhost:8080/Lai16XacmlRequestsGeneratorWS/services/XacmlRequestsGenerator"
	 */
	
	
}
