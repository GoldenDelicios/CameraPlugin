package org.slabserver.plugin.camera;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.yaml.snakeyaml.Yaml;

public class Camera extends JavaPlugin implements Listener {
	
	public static class CameraData {
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
	
	static final PotionEffect RESISTANCE = new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 100, 5, false, false);
	final File playerData = new File(this.getDataFolder() + "/playerData.yml");

	Map<String, CameraData> cameraData = new HashMap<>();
	Map<String, GameMode> specData = new HashMap<>();
	
	public Camera() {
		
	}

	public Camera(JavaPluginLoader loader, PluginDescriptionFile description, File dataFolder, File file) {
		super(loader, description, dataFolder, file);
	}

	@Override
	public void onEnable() {
		getServer().getPluginManager().registerEvents(this, this);
		load();
	}

	@Override
	public void onDisable() {
		
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void load() {
		try {
			this.getDataFolder().mkdir();
			
			if (playerData.exists()) {
				FileInputStream stream = new FileInputStream(playerData);
				Map<?,?> loaded = new Yaml().load(stream);
				
				Map<String, Object> cameraData = (Map<String, Object>) loaded.get("cameraData");
				for (Entry<String, Object> entry : cameraData.entrySet()) {
					Map<String, Object> value = (Map<String, Object>) entry.getValue();
					CameraData newValue = CameraData.deserialize(value);
					entry.setValue(newValue);
				}
				
				Map<String, Object> specData = (Map<String, Object>) loaded.get("specData");
				for (Entry<String, Object> entry : specData.entrySet()) {
					String value = (String) entry.getValue();
					GameMode newValue = GameMode.valueOf(value);
					entry.setValue(newValue);
				}
				
				this.cameraData = (Map) cameraData;
				this.specData = (Map) specData;
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			this.getLogger().log(Level.WARNING, "Could not load file! " + e);
		}
	}
	
	public void save() {
		try {
			Map<String, Object> cameraData = new HashMap<>();
			this.cameraData.forEach((uuid, data) -> {
				cameraData.put(uuid, data.serialize());
			});
			
			Map<String, Object> specData = new HashMap<>();
			this.specData.forEach((uuid, gamemode) -> {
				specData.put(uuid, gamemode.name());
			});
			
			Map<String, Object> map = new HashMap<>();
			map.put("cameraData", cameraData);
			map.put("specData", specData);
			
			String yaml = new Yaml().dumpAsMap(map);
			this.getDataFolder().mkdir();
			FileWriter writer = new FileWriter(playerData);
			writer.append(yaml);
			writer.close();
		}
		catch (Exception e) {
			e.printStackTrace();
			this.getLogger().log(Level.WARNING, "Could not save file! " + e);
		}
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equals("c")) {
			return toggleCamera(sender);
		}
		else if (cmd.getName().equals("s")) {
			return toggleSpectator(sender);
		}
		else if (cmd.getName().equals("fly")) {
			return toggleFly(sender);
		}
		
		return false;
	}

	private boolean toggleFly(CommandSender sender) {
		if (sender instanceof Player) {
			Player player = (Player) sender;
			player.addPotionEffect(RESISTANCE);
			player.setAllowFlight(!player.getAllowFlight());
		}
		else {
			sender.sendMessage("This command may only be run by a player");
		}
		return true;
	}

	private boolean toggleSpectator(CommandSender sender) {
		if (sender instanceof Player) {
			Player player = (Player) sender;
			String uuid = player.getUniqueId().toString();
			
			if (player.getGameMode() == GameMode.SPECTATOR) {
				player.addPotionEffect(RESISTANCE);
				GameMode gamemode = specData.remove(uuid);
				player.setGameMode(gamemode == null ? this.getServer().getDefaultGameMode() : gamemode);
			}
			else {
				specData.put(uuid, player.getGameMode());
				player.setGameMode(GameMode.SPECTATOR);
			}
			save();
		}
		else {
			sender.sendMessage("This command may only be run by a player");
		}
		return true;
	}

	private boolean toggleCamera(CommandSender sender) {
		if (sender instanceof Player) {
			Player player = (Player) sender;
			
			String uuid = player.getUniqueId().toString();
			CameraData data = cameraData.remove(uuid);
			
			if (data == null) {
				// set player to camera mode
				data = new CameraData(player.getLocation(), player.getGameMode());
				cameraData.put(uuid, data);
				player.setGameMode(GameMode.SPECTATOR);
			}
			else {
				// take player out of camera mode
				player.addPotionEffect(RESISTANCE);
				player.teleport(data.location);
				player.setGameMode(data.gamemode);
			}
			save();
		}
		else {
			sender.sendMessage("This command may only be run by a player");
		}
		return true;
	}
	
}
