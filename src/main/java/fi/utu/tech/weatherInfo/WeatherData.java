package fi.utu.tech.weatherInfo;

/*
 * Class presenting current weather
 * Is returned by  weather service class
 */

public class WeatherData {

	/*
	 * What kind of data is needed? What are the variable types. Define class
	 * variables to hold the data
	 */
	
	private double sademaara; //millimetreinä
	private double lampotila; //C-asteina
	
	private boolean sataa = false;  // Jos arvo = 1 ---> sataa
	private boolean alle0 = false;   // Jos arvo = 1 ---> on alle 0C lämmintä
	
	/*
	 * Since this class is only a container for weather data we only need to set the
	 * data in the constructor.
	 */
	
	public WeatherData(double sademäärä, double lämpötila) {
		sademaara = sademäärä;
		lampotila = lämpötila;
		
		if(sademaara > 0) {
			sataa = true;
		}
		
		if(lampotila <= 0) {
			alle0 = true;
		}		
	}
	
	public boolean getItIsRaining() {
		return this.sataa;
	}
	
	public boolean getItIsTooCold() {
		return alle0;
	}
	
	
}
