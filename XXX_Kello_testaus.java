package Testaus;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Timer;
import java.util.TimerTask;

public class Kello_testaus {

	private static Timer timer = new Timer();

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		timer.schedule(new MyTask(), 0, 1000 * 60 * 1);
	}
}

class MyTask extends TimerTask {
	public void run() {
		ZonedDateTime zdtNow = ZonedDateTime.now();
		
		String tunnit = DateTimeFormatter.ISO_INSTANT.format(zdtNow).toString().substring(11,13);
		String minuutit = DateTimeFormatter.ISO_INSTANT.format(zdtNow).toString().substring(14,16);
		
		//System.out.println("Tunnit: " + tunnit + " Minuutit: " + minuutit);
		
		if(Integer.parseInt(minuutit) == 0 || Integer.parseInt(minuutit)%5 == 0) {
			System.out.println("Kello: " + DateTimeFormatter.ISO_INSTANT.format(zdtNow).toString().substring(11, 19)+" Tarkistetaan ryhmien hälytysajat.");
		}
				
//		else {
//			System.out.println("Ei vielä...");
//		}
		
	}
}
