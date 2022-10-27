package fi.utu.tech.weatherInfo;

import org.w3c.dom.*;
import org.xml.sax.*;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import javax.xml.parsers.*;
import javax.xml.xpath.*;

public class FMIWeatherService {

	//private final String CapURL = "https://opendata.fmi.fi/wfs?request=GetCapabilities";
	//private final String FeaURL = "https://opendata.fmi.fi/wfs?request=GetFeature";
	//private final String ValURL = "https://opendata.fmi.fi/wfs?request=GetPropertyValue";
	//private final String DataURL = "http://opendata.fmi.fi/wfs?service=WFS&version=2.0.0&request=getFeature&storedquery_id=fmi::forecast::hirlam::surface::point::multipointcoverage&place=turku&";

	/*
	 * In this method your are required to fetch weather data from The Finnish
	 * Meteorological Institute. The data is received in XML-format.
	 */

	public static WeatherData getWeather() throws ParserConfigurationException, SAXException, IOException {
		
		// Miten luodaan yhteys sääpalveluun ja haetaan tiedot?
		// Tähän aika ja haetaan sen mukaan säätiedot.
		
	    //Starting with an java.time.Instant value
	    Instant timeStamp= Instant.now();
	    //System.out.println("Machine Time Now:" + timeStamp);
	 
	    //timeStamp in zone - "Europe/Helsinki"
	    ZonedDateTime zdtNow = timeStamp.atZone(ZoneId.of("Europe/Helsinki"));
	    //System.out.println("In Helsinki Time Zone:"+ helsinkiZone);
		
		System.out.println("Haetaan säätiedot herätykseen: " + zdtNow.toString());
		
		//int herätysTunti = zdtNow.getHour(); // 00:xx tai 01:xx tai 02:00.... 23:59
		//System.out.println("HerätysTunti : " + herätysTunti + " (Noudetaan tätä 2 tuntia aiempi data, joka kerätty edellisen tunnin aikana. Esimerkki: Pyyntö: 17:xx -> Data: 15:00 -> Haku: 14:01-14:59)");
		
		ZonedDateTime zdtNowTemp1 = timeStamp.atZone(ZoneId.of("Europe/Helsinki"));
		ZonedDateTime zdtNowTemp2 = timeStamp.atZone(ZoneId.of("Europe/Helsinki"));
		
		//Mikäli säätä haetaan tunnin ensimmäisen neljän minuutin aikana on säätiedot haettava 4 tunnin takaa
		if(zdtNow.getMinute() < 5) {
			zdtNowTemp1 = zdtNow.minusHours(4).minusMinutes(zdtNow.getMinute()).minusSeconds(zdtNow.getSecond()).plusMinutes(1);  //Alkuaika
			zdtNowTemp2 = zdtNow.minusHours(4).minusMinutes(zdtNow.getMinute()).minusSeconds(zdtNow.getSecond()).plusMinutes(59); //Loppuaika
		}
		//Mikäli säätä haetaan tunnin 05...59 minuutin aikana on säätiedot haettava 3 tunnin takaa
		else {
			zdtNowTemp1 = zdtNow.minusHours(3).minusMinutes(zdtNow.getMinute()).minusSeconds(zdtNow.getSecond()).plusMinutes(1);  //Alkuaika
			zdtNowTemp2 = zdtNow.minusHours(3).minusMinutes(zdtNow.getMinute()).minusSeconds(zdtNow.getSecond()).plusMinutes(59); //Loppuaika
		}
		
		String alkuAika2 = zdtNowTemp1.toString().substring(0,19) +'Z';
		String loppuAika2= zdtNowTemp2.toString().substring(0,19) +'Z';
		
		//System.out.println("   [debug print FMI01a] Alkuaika: " + alkuAika2);
		//System.out.println("   [debug print FMI01b] Loppuaika: " + loppuAika2);
		
		//Haku URL:n muodostaminen/kokoaminen
		String osa1 = "http://opendata.fmi.fi/wfs?service=WFS&version=2.0.0&request=getFeature&storedquery_id=fmi::observations::weather::hourly::simple&";
		String osa2 = "starttime=" + alkuAika2 + "&stoptime=" + loppuAika2 + "&place=turku&parameters=";
		String osa3a = "TA_PT1H_AVG"; //Lämpötila
		String osa3b = ",";
		String osa3c = "PRA_PT1H_ACC"; //Sademäärä
		String osa4 = "&";
		
		//URL haku lämpötila ja sade:
		String tempeRainURL = osa1 + osa2 + osa3a + osa3b + osa3c + osa4; 
		
		//#############################################################################################
		//#############################################################################################
		//Datan hakeminen:
		final String DataURL01 = tempeRainURL;
		
		// XML-muotoisen datan suodattaminen (parse)
		
		double lämpötilaKa = -200.0; //Lämpötila keskiarvo tunnin aikana
		double sadeKertymä = -100.0;   //Sadekertymä tunnin aikana
				
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	    DocumentBuilder d = factory.newDocumentBuilder();
	    
		URLConnection urlConData01 = new URL(DataURL01).openConnection();
	    urlConData01.addRequestProperty("Accept", "application/xml");
	    Document docData01 = d.parse(urlConData01.getInputStream());
	    docData01.getDocumentElement().normalize();

	    //Read root element
	    //                                     document       locateRoot       getItName
	    // System.out.println("Root element: " +  docData01.getDocumentElement().getNodeName());

	    //Read array of elements -> nodeList
	    //NodeList nList = docData01.getElementsByTagName("wfs:FeatureCollection");
	    NodeList nList = docData01.getElementsByTagName("wfs:member");
	    
	    //System.out.println("Node amount: " + nList.getLength() +"\n");
	    
	    //if(nList.getLength()>2) {
	    //	System.out.println("Ohjelma lopetettu: Liian monta nodea -> tarkista starttime ja stoptime (päivämäärä/kellonajat).");
	    //	System.exit(0);
	    //}
	    
	    for (int k = 0; k < nList.getLength(); k++){
	    	Node nNode = nList.item(k);
	        if (nNode.getNodeType() == Node.ELEMENT_NODE){
	        	Element eElement = (Element) nNode;
	        	if(k==0) {
	        		//System.out.println("Lämpötila: " + eElement.getChildNodes().item(1).getChildNodes().item(5).getTextContent());
	        		//lämpötilaKa = Double.valueOf(eElement.getChildNodes().item(1).getChildNodes().item(7).getTextContent());
	        		// System.out.println("Keskilämpötila (" + eElement.getElementsByTagName("BsWfs:ParameterName").item(0).getTextContent() + "): " +
					//		eElement.getElementsByTagName("BsWfs:ParameterValue").item(0).getTextContent());
	        		lämpötilaKa = Double.valueOf(eElement.getElementsByTagName("BsWfs:ParameterValue").item(0).getTextContent());
	        	}
	        	if(k==1) {
	        		//System.out.println("Sademäärä: " + eElement.getChildNodes().item(1).getChildNodes().item(5).getTextContent());
	        		//sadeKertymä = Double.valueOf(eElement.getChildNodes().item(1).getChildNodes().item(7).getTextContent());
	        		// System.out.println("Sadekertymä   (" + eElement.getElementsByTagName("BsWfs:ParameterName").item(0).getTextContent() + "): " +
	        		//					eElement.getElementsByTagName("BsWfs:ParameterValue").item(0).getTextContent());
	        		sadeKertymä = Double.valueOf(eElement.getElementsByTagName("BsWfs:ParameterValue").item(0).getTextContent());
	        	}
	        }
	    }
	    //System.out.println("Keskilämpötila " +  + ": " + lämpötilaKa);
	    //System.out.println("Sadekertymä: " + sadeKertymä);
		
		System.out.println("Sääpalvelusta noudetut säätiedot (" + alkuAika2.substring(0,10) + ": " + alkuAika2.substring(11,16) +"..." + loppuAika2.substring(11, 16) + "): " + "Sademäärä(cum):" + sadeKertymä + ", Lämpötila(avg):" + lämpötilaKa);
	    
		WeatherData wData = new WeatherData(sadeKertymä, lämpötilaKa);
	    
		return wData;
	}
}
