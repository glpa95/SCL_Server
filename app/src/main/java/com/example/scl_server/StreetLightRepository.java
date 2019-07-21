package com.example.scl_server;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

public class StreetLightRepository {
	
	private HashMap<String, List<StreetLight>> streetLights;
	
	public StreetLightRepository() {
		streetLights = new HashMap<String, List<StreetLight>>();
	}
	
	public void save(StreetLight newLight, List<String> w3wAddresses) {
		for(String address : w3wAddresses) {
			List<StreetLight> lights = null;
			if(streetLights.containsKey(address)) {
				lights = streetLights.get(address);
			} else {
				lights = new ArrayList<StreetLight>();
				streetLights.put(address, lights);
			}
			lights.add(newLight);
		}
	}
	
	
	public List<StreetLight> getLightsInRange(String address) {
		if(streetLights.containsKey(address)) {
			return streetLights.get(address);
		} else {
			return new ArrayList<StreetLight>();
		}
	}
	
	

}
