module fi.utu.tech.ringersClock {
	requires javafx.controls;
	requires javafx.fxml;
	requires transitive javafx.graphics;
	requires java.logging;
	requires javafx.base;
	requires java.xml;
	requires java.sql;

	opens fi.utu.tech.ringersClock to javafx.fxml;
	opens fi.utu.tech.ringersClock.UI to javafx.fxml;

	exports fi.utu.tech.ringersClock;
	exports fi.utu.tech.ringersClock.UI;
	exports fi.utu.tech.ringersClock.entities;
	exports fi.utu.tech.weatherInfo;
}