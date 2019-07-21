package com.example.scl_server;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;

public class SCLService extends Service { //Diese Klasse als Service deklarieren bzw. um die Service-Funktionen erweitern

    //Deklarierung der Objekte die für den Service benötigt werden
    private LocationListener listener; // Fragt Standortänderungen ab
    private LocationManager locationManager;// Für Zugang zu den Ortungsfunktionen des Smartphones


    StreetLightRepository repository;
    What3WordsV3 w3wApi = null;


    @Nullable //Die Methode darf "null" zurückgeben
    @Override
    public IBinder onBind(Intent intent) { //Diese Methode muss immer im Service implementiert werden, wird aber hier nicht benötigt somit soll Diese "null" zurückgeben
        return null;
    }

    //"final" weil sich der Wert der Variable nach einmaliger Zuordnung nie verändern soll


    @Override
    public void onCreate() //onCreate-Methode (Methode die immer zu Anfang ausgeführt wird)
    {
        this.repository = new StreetLightRepository();
        w3wApi = new What3WordsV3("GD7LLRNL");

        List<String> light1Adresses = new ArrayList<String>();
        light1Adresses.add("covered.unoccupied.exasperated");
        light1Adresses.add("newbie.imprecise.obliging");
        light1Adresses.add("dared.rambles.windscreen");

        repository.save(new StreetLight("192.168.1.100", "80"), light1Adresses); //TODO Ip-Adresse




        Log.d("SCL", "Service created");
        listener = new LocationListener() { //Initialisieren des LocationListener (Diese Methode wird aufgerufen wenn sich etwas im Location_Manager ändert)
            @Override
            public void onLocationChanged(Location location) { // Wird aufgerufen wenn sich der Standort geändert hat
                Log.d("SCL", "Location changed");
                // Übertragen der Daten aus dieser Methode in die MainActivity
                new LocationNotifyTask().execute(location);

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) { }
            @Override
            public void onProviderEnabled(String provider) { }

            @Override
            //Bisher wird immer nur genau einmal abgefragt wenn der Start Button gedrückt wird (Wenn man aber den Standort in dem Fenster nicht einschaltet und wieder zurückklickt und erneut auf Start drückt, wird das Einstellungsfenster nicht mehr geöffnet)
            public void onProviderDisabled(String provider) { //Falls GPS deaktiviert ist
                Intent i = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS); //Neuer Intent zu Standorteinstellungen des Handies
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); //Das Setzen des Flags lässt diesen Intent eine neue Task starten d.h. wenn man von den Standorteinstellungen wieder zurück geht in die App, wird einem die App im selben Zustand gezeigt wie sie vorher war
                startActivity(i); //Starte die Aktivität aus dem Intent i (Standorteinstellungen des Smartphones)
            }
        };

        locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE); //Initialisieren des Location Managers mit der getSystemService-Methode (Welcher Service wird benötigt ? Location Service.)
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 0, listener); //Abfrage des GPS_Provider alle 3000ms, GPS_PROVIDER = Name des GPS Standortproviders, MinDistance: Min. Distanz zu vorherigem Standort damit Standort aktualisiert wird(falls 0 dann wird nur die minTime berücksichtigt) ,listener = Name des Listeners
        // Rot unterstrichen weil nach den Berechtigungen gefragt wird, jedoch wurde das bereits in der MainActivity geprüft (somit kann die Warnung ignoriert werden)

    }

    public void onDestroy() //Methode die den Service "zerstört"/beendet wenn die Taste "Stoppe" gedrückt wurde und der Service somit beendet wird
    {
        super.onDestroy();
        if (locationManager != null) //Solange noch etwas im locationManager
        {
            locationManager.removeUpdates(listener); //Löscht die Informationen im LocationListener und zudem werden nach dem Befehl auch keine neuen Updates(=holen aktueller Informationen) mehr ausgeführt
        }
        ;
    }


    class LocationNotifyTask extends AsyncTask<Location, Void, String> {

        private final String host;
        private Intent i;

        public LocationNotifyTask() {
            host = "192.168.1.104";
            this.i = new Intent("location_update"); //Erstellen eines neuen Intents i mit dem Aktionsnamen "location_update"(Ziel des Intents ist die Standortaktualisierung) (Die Bezeichnung dient als Intent-Filter damit der BroadcastReceiver in der Main zuerst nach Intents mit diesem Namen abfrafgt);
        }


        @Override
        protected String doInBackground(Location... locations) { //Was soll im Hintergrund der AsyncTask ablaufen
            Location location = locations[0];

            JSONObject coords = new JSONObject();

            try {
                coords.put("lat", location.getLatitude());
                coords.put("lng", location.getLongitude());
            } catch (JSONException e) {
                e.printStackTrace();
            }

            try {
                URL url = new URL("http://" + host +":8080/api/location"); //Zusammenstellen der URL die als API Request gesendet werden soll: https://api.what3words.com/v2/reverse?coords=bg%2Clg&key=O70UBGBH
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection(); //Verbindung zu der URL herstellen und eine neue Instanz der Klasse HttpURLConnection erstellen die weitere Funktionalitäten ermöglicht

                byte[] coordsData = coords.toString().getBytes();
                int    postDataLength = coordsData.length;
                Log.d("SCL", coords.toString());


                urlConnection.setDoOutput( true );
                urlConnection.setRequestMethod( "POST" );
                urlConnection.setRequestProperty( "Content-Type", "application/json");
                urlConnection.setRequestProperty( "charset", "utf-8");
                urlConnection.setRequestProperty( "Content-Length", Integer.toString( postDataLength ));

                urlConnection.connect();



                try( DataOutputStream wr = new DataOutputStream( urlConnection.getOutputStream())) {
                    wr.write( coordsData );
                }

                Log.d("SCL", urlConnection.getResponseCode() + "");


                try {
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream())); //Die neue Instanz der Klasse BufferedReader dient als Zwischenspeicher welcher den eingehenden Datenfluss der URL Verbindung zwischenspeichert
                    StringBuilder stringBuilder = new StringBuilder(); //Neuen Instanz der Klasse StringBuilder initialisieren (Tut das was der Name sagt, man kann mithilfe des append-Befehls schrittweise seinen String aufbauen)
                    String line;
                    while ((line = bufferedReader.readLine()) != null) { // Die zusätzliche Zwischenvariable line speichert den Zeilennhalt des bufferedReader zwischen und hängt ihn an der Instanz des StringBuilders an sofern noch was im bufferedReader drin steht
                        stringBuilder.append(line).append("\n"); //String Builder fügt die API-Antwort Zeile für Zeile zusammen
                    }
                    bufferedReader.close(); //Schließen des BufferedReaders

                    return stringBuilder.toString(); //Zurückgeben des Inhalts des StringBuilders als String(Die gesamte Antwort der API), Umwandlung als String notwendig weil das Rückgabeformat JSON, GeoJSON oder XML ist
                } finally { //finally ist ein Programmabschnitt der unabhängig von der try-catch Auswertung immer ausgeführt wird
                    urlConnection.disconnect(); //Nachdem die Antwort gelesen wurde wird die bestehende Verbindung zur URL geschlossen
                }

            } catch (Exception e) { //Fehlerbehandlung, falls die Anweisungen in try fehlgeschlagen ist wird der catch-Block ausgeführt
                Log.e("SCL", e.getMessage(), e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if(!result.trim().equals("[]")) {
                i.putExtra("Lampen", result); // Anhängen von "Koordinaten"(=Bezeichnung des Anhangs) an den Intent (Koordinaten gleich Längengrad + Breitengrad)
                sendBroadcast(i); //"Normale" Broadcast bzw. Übertragung (Intent i wird an alle Empfänger verschickt)
            }
        }
    }


}