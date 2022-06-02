package org.slabserver.plugin.camera;

import java.util.Map;

import org.bukkit.GameMode;
import org.bukkit.Location;

public class CameraData {
	Location location;
	GameMode gamemode;
	
	public CameraData(Location location, GameMode gamemode) {
		this.location = location;
		this.gamemode = gamemode;
	}
	
	public Map<String, Object> serialize() {
		Map<String, Object> map = location.serialize();
		map.put("gamemode", gamemode.name());
		return map;
	}
	
	public static CameraData deserialize(Map<String, Object> map) {
		Location location = Location.deserialize(map);
		GameMode gamemode = GameMode.valueOf((String) map.get("gamemode"));
		return new CameraData(location, gamemode);
	}
}
