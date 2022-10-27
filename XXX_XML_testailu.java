package Testaus;

import org.w3c.dom.*;
import org.xml.sax.*;

import javax.xml.parsers.*;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;


public class XML_testailu {
	
	// https://howtodoinjava.com/java/xml/read-xml-dom-parser-example/
	// https://stackoverflow.com/questions/38904352/parsing-xml-from-url-in-java
	
	public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {
		
		final String CapURL = "https://opendata.fmi.fi/wfs?request=GetCapabilities";
		final String FeaURL = "https://opendata.fmi.fi/wfs?request=GetFeature";
		final String ValURL = "https://opendata.fmi.fi/wfs?request=GetPropertyValue";
		final String DataURL = "http://opendata.fmi.fi/wfs?service=WFS&version=2.0.0&request=getFeature&storedquery_id=fmi::forecast::hirlam::surface::point::multipointcoverage&place=turku&";

		//Yllä olevaan DataURL:iin pitää muuttaa alla olevat kohdat:
		//storedquery_id=fmi::observations::weather::hourly::simple
		
		//final String DataURL2 = "http://opendata.fmi.fi/wfs?service=WFS&version=2.0.0&request=getFeature&storedquery_id=fmi::observations::weather::hourly::simple&place=turku&";
		//final String DataURL2 = "http://opendata.fmi.fi/wfs?service=WFS&version=2.0.0&request=getFeature&storedquery_id=fmi::observations::weather::hourly::simple&starttime=2020-12-07T12:01:00Z&stoptime=2020-12-07T12:59:00Z&place=turku&parameters=TA_PT1H_AVG&";
		//YLLÄ: TA_PT1H_AVG -> ​​<weather​​parameter>_<time​​span>_<method>.​​ TA_PT1H_AVG​​ for ​​hourly ​​mean ​​temperature.
		//file:///tmp/mozilla_khl0/METreport-bora-naming-convention_unsigned-1.pdf
		
		/*
		 variable == "TA_PT1H_AVG" ~ "Ilman lämpötila / Air temperature (C)",
      	 variable == "PRI_PT1H_MAX" ~ "Sateen intensiteetti / Maximum precipitation intensity (mm/h)",
      	 variable == "WS_PT1H_AVG" ~ "Tuulen nopeus / Wind speed (m/s)",
      	 variable == "RH_PT1H_AVG" ~ "Ilmankosteus / Relative humidity (%)"),
      	
      	 PRA_PT1H_ACC
      	
      	 TA_PT1H_AVG  Lämpötila keskiarvo
      	 TA_PT1H_MAX  Lämpötila maksimi
      	 TA_PT1H_MIN  Lämpötila minimi
      	 RH_PT1H_AVG  Suhteellisen kosteuden keskiarvo
      	 WS_PT1H_AVG  Tuulen nopeuden keskiarvo
      	 WS_PT1H_MAX  Tuulen nopeuden 10 minuutin keskiarvojen maksimi
      	 WS_PT1H_MIN  Tuulen nopeuden 10 minuutin keskiarvojen minimi
      	 WD_PT1H_AVG  Tuulen suunnan keskiarvo
      	 PRA_PT1H_ACC Sademäärä
      	 PRI_PT1H_MAX  Sateen intensiteetin maksimi 
      	 PA_PT1H_AVG   Ilmanpaineen keskiarvo
      	 WAWA_PT1H_RANK  Merkittävin sääkoodi
		 * 
		 */
		
		// S��havaintoaineisto: tuntiarvot
		final String DataURL01 = "http://opendata.fmi.fi/wfs?service=WFS&version=2.0.0&request=getFeature&storedquery_id=fmi::observations::weather::hourly::simple&starttime=2020-12-09T08:01:00Z&stoptime=2020-12-0T08:59:00Z&place=turku&parameters=TA_PT1H_AVG,PRA_PT1H_ACC&";
		
		double lämpötilaKa = -200.0; //Lämpötila keskiarvo tunnin aikana
		double sadeKertymä = -1.0;   //Sadekertymä tunnin aikana
				
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	    DocumentBuilder d = factory.newDocumentBuilder();
	    
		URLConnection urlConData01 = new URL(DataURL01).openConnection();
	    urlConData01.addRequestProperty("Accept", "application/xml");
	    Document docData01 = d.parse(urlConData01.getInputStream());
	    docData01.getDocumentElement().normalize();

	    //Read root element
	    //                                     document       locateRoot       getItName
	    System.out.println("Root element: " +  docData01.getDocumentElement().getNodeName());

	    //Read array of elements -> nodeList
	    //NodeList nList = docData01.getElementsByTagName("wfs:FeatureCollection");
	    NodeList nList = docData01.getElementsByTagName("wfs:member");
	    
	    System.out.println("Node amount: " + nList.getLength() +"\n");
	    
	    if(nList.getLength()>2) {
	    	System.out.println("Ohjelma lopetettu: Liian monta nodea -> tarkista starttime ja stoptime (päivämäärä/kellonajat).");
	    	System.exit(0);
	    }
	    
	    for (int k = 0; k < nList.getLength(); k++){
	    	Node nNode = nList.item(k);
	        if (nNode.getNodeType() == Node.ELEMENT_NODE){
	        	Element eElement = (Element) nNode;
	        	if(k==0) {
	        		//System.out.println("Lämpötila: " + eElement.getChildNodes().item(1).getChildNodes().item(5).getTextContent());
	        		//lämpötilaKa = Double.valueOf(eElement.getChildNodes().item(1).getChildNodes().item(7).getTextContent());
	        		System.out.println("Keskilämpötila (" + eElement.getElementsByTagName("BsWfs:ParameterName").item(0).getTextContent() + "): " +
							eElement.getElementsByTagName("BsWfs:ParameterValue").item(0).getTextContent());
	        		lämpötilaKa = Double.valueOf(eElement.getElementsByTagName("BsWfs:ParameterValue").item(0).getTextContent());
	        	}
	        	if(k==1) {
	        		//System.out.println("Sademäärä: " + eElement.getChildNodes().item(1).getChildNodes().item(5).getTextContent());
	        		//sadeKertymä = Double.valueOf(eElement.getChildNodes().item(1).getChildNodes().item(7).getTextContent());
	        		
	        		System.out.println("Sadekertymä   (" + eElement.getElementsByTagName("BsWfs:ParameterName").item(0).getTextContent() + "): " +
	        							eElement.getElementsByTagName("BsWfs:ParameterValue").item(0).getTextContent());
	        		sadeKertymä = Double.valueOf(eElement.getElementsByTagName("BsWfs:ParameterValue").item(0).getTextContent());
	        		}
	        }
	    }
	    System.out.println("Tunnin keskilämpötila: " + lämpötilaKa);
	    System.out.println("Tunnin sadekertymä: " + sadeKertymä);
	}
}
