package it.unipi.di.lai.groot.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import it.unipi.di.lai.utils.DomUtils;

public class Prova {
	private static Document doc;
	private static DocumentBuilderFactory dbFactory;
	private static Element xacmlRequestDOM;
	public Prova() {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) throws TransformerException {
		// TODO Auto-generated method stub
		
		generateDocument();
		Element risultato = generateElement("Risultato");
		
		doc.appendChild(risultato);
		
		DomUtils  domUtils = new DomUtils();
		StreamSource in = new StreamSource();
		
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		domUtils.serialize(doc, baos);
		System.out.println(baos.toString());
		
//		validator.invoke(baos.toString());
//
//		Document doc = parser.parse(new ByteArrayInputStream(request.getBytes())); 
//		
//		
//		ByteArrayInputStream out =	new ByteArrayInputStream();
//		
//		OutputStream outputStream;
//		domUtils.serialize(doc, System.out);
//		
//		
//		
//		StreamSource streamSource = new StreamSource(new ByteArrayInputStream(reponseMsg.getBytes()));
//		StreamUtils.copy(in, out);

		
	}
	
	private static void generateDocument(){
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
	
	private static Element generateElement(String elementName){
		Element element = doc.createElement(elementName);
		
		
//		xacmlRequestDOM = doc.createElementNS("urn:oasis:names:tc:xacml:3.0:core:schema:wd-17", XacmlConstants.Request);
//		xacmlRequestDOM.setAttribute(XacmlConstants.CombinedDecision, "false");
//		xacmlRequestDOM.setAttribute(XacmlConstants.ReturnPolicyIdList, "false");
//		doc.appendChild(xacmlRequestDOM);
		return element;
	}

}
