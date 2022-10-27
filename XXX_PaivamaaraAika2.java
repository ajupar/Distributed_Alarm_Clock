package Testaus;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class XXX_PäivämääräAika2 {

	public static void main(String[] args) {
		
		System.out.println("URL-linkin rakentamiseksi halutaan alku- ja loppuaika halutaan seuraavan muotoisiksi:");
        System.out.println("HALUTTU MUOTO: 2020-12-08T09:00:00Z \n");
        
		//https://www.javatpoint.com/java-localdatetime
		//LocalDateTime ldtNow = LocalDateTime.now();
        
        //https://www.javabrahman.com/java-8/working-with-time-zones-in-java-8-zoneddatetime-zoneid-tutorial-with-examples/
	    //Starting with an java.time.Instant value
	    Instant timeStamp= Instant.now();
	    //System.out.println("Machine Time Now:" + timeStamp);
	 
	    //timeStamp in zone - "Europe/Helsinki"
	    ZonedDateTime zdtNow2 = timeStamp.atZone(ZoneId.of("Europe/Helsinki"));
	    //System.out.println("In Helsinki Time Zone:"+ helsinkiZone);
        
		System.out.println("Muokkaamaton : " + zdtNow2.toString());
				
        int herätysTunti = zdtNow2.getHour(); // 00:xx tai 01:xx tai 02:00.... 23:59
        System.out.println("HerätysTunti : " + herätysTunti + " (Noudetaan tästä 2 tuntia aiempi data, joka kerätty edellisen tunnin aikana. Esimerkki: Pyyntö: 17:xx -> Data: 15:00 -> Haku: 14:01-14:59)");
		
		ZonedDateTime zdtNowTemp3 = timeStamp.atZone(ZoneId.of("Europe/Helsinki"));
		ZonedDateTime zdtNowTemp4 = timeStamp.atZone(ZoneId.of("Europe/Helsinki"));
		
		zdtNowTemp3 = zdtNow2.minusHours(3).minusMinutes(zdtNow2.getMinute()).minusSeconds(zdtNow2.getSecond()).plusMinutes(1);  //Alkuaika
		zdtNowTemp4 = zdtNow2.minusHours(3).minusMinutes(zdtNow2.getMinute()).minusSeconds(zdtNow2.getSecond()).plusMinutes(59); //Loppuaika
		
		//alkuAika = DateTimeFormatter.ISO_INSTANT.format(zdtNowTemp3).toString();
		//loppuAika= DateTimeFormatter.ISO_INSTANT.format(zdtNowTemp4).toString();
		
		String alkuAika2 = zdtNowTemp3.toString().substring(0,19) +'Z';
		String loppuAika2= zdtNowTemp4.toString().substring(0,19) +'Z';
		
		System.out.println("Alkuaika     : " + alkuAika2);
		System.out.println("Loppuaika    : " + loppuAika2);
				
		//final String DataURL2 = "http://opendata.fmi.fi/wfs?service=WFS&version=2.0.0&request=getFeature&storedquery_id=fmi::observations::weather::hourly::simple&starttime=2020-12-08T07:01:00Z&stoptime=2020-12-08T07:59:00Z&place=turku&parameters=TA_PT1H_AVG,PRA_PT1H_ACC&";
		
		//Haku URL:n muodostaminen/kokoaminen
		String osa1 = "http://opendata.fmi.fi/wfs?service=WFS&version=2.0.0&request=getFeature&storedquery_id=fmi::observations::weather::hourly::simple&";
		String osa2 = "starttime=" + alkuAika2 + "&stoptime=" + loppuAika2 + "&place=turku&parameters=";
		String osa3a = "TA_PT1H_AVG"; //Lämpötila
		String osa3b = ",";
		String osa3c = "PRA_PT1H_ACC"; //Sademäärä
		String osa4 = "&";
		
		//URL haku lämpötila ja sade:
		String tempeRainURL = osa1 + osa2 + osa3a + osa3b + osa3c + osa4; 
		//URL haku lämpötila:
		String tempeURL = osa1 + osa2 + osa3a + osa4;
		//URL haku sade:
		String rainURL = osa1 + osa2 + osa3c + osa4;
		
		System.out.println("\nURL linkin malli:");
		System.out.println("http://opendata.fmi.fi/wfs?service=WFS&version=2.0.0&request=getFeature&storedquery_id=fmi::observations::weather::hourly::simple&starttime=2020-12-08T07:01:00Z&stoptime=2020-12-08T07:59:00Z&place=turku&parameters=TA_PT1H_AVG,PRA_PT1H_ACC&");
		System.out.println("Lämpötila ja sade:");
		System.out.println(tempeRainURL);
		System.out.println("Lämpötila:");
		System.out.println(tempeURL);
		System.out.println("Sade:");
		System.out.println(rainURL);
		
		System.out.println("XXXX(URL:t testattu https://www.xmlviewer.org/ sivustolla.)XXXX");
		
		
		
	}

}
