package fi.utu.tech.ringersClockServer;



public class ServerApp {

	private static ServerApp app;
	private ServerSocketListener listener;
	private WakeUpService wup;
	private String serverIP = "127.0.0.1";
	private int serverPort = 3000;

	public ServerApp() {

		wup = new WakeUpService();
		listener = new ServerSocketListener(serverIP, serverPort, wup);
		
		wup.setListener(listener);   // yhdistetään kuuntelija wupiin, jotta wup voi hakea kuuntelijasta tietoa olemassaolevista yhteyksistä

		wup.start();
		listener.start();
	}

	public static void main(String[] args) {

		System.out.println("Starting server...");
		app = new ServerApp();
	}
	
	public ServerSocketListener getServerSocketListener() {
		return this.listener;		
	}
	
	public WakeUpService getWakeUpService() {
		return this.wup;
	}
	
	public String getServerIP() {
		return this.serverIP;
	}
	
	public int getServerPort() {
		return this.serverPort;
	}

}
