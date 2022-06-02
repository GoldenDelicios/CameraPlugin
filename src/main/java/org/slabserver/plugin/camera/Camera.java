package org.slabserver.plugin.camera;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.yaml.snakeyaml.Yaml;

public class Camera extends JavaPlugin implements Listener {
	private static final PotionEffect RESISTANCE = new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 100, 5, false, false);
	private final File playerData = new File(this.getDataFolder() + "/playerData.yml");
	private Map<String, CameraData> cameraData;
	private Map<String, GameMode> specData;
	
	@Override
	public void onEnable() {
		this.getServer().getPluginManager().registerEvents(this, this);
		load();
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void load() {
		try {
			this.getDataFolder().mkdir();
			
			if (playerData.exists()) {
				FileInputStream stream = new FileInputStream(playerData);
				Map<?, Map<String, Object>> loaded = new Yaml().load(stream);
				
				Map<String, Object> cameraData = loaded.get("cameraData");
				for (Entry<String, Object> entry : cameraData.entrySet()) {
					Map<String, Object> value = (Map<String, Object>) entry.getValue();
					CameraData newValue = CameraData.deserialize(value);
					entry.setValue(newValue);
				}
				
				Map<String, Object> specData = loaded.get("specData");
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
			this.getLogger().warning("Could not load file! " + e);
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
			this.getLogger().warning("Could not save file! " + e);
		}
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (sender instanceof Player) {
			Player player = (Player) sender;
			switch (cmd.getName()) {
			case "c":
				return toggleCamera(player);
			case "s":
				return toggleSpectator(player);
			case "fly":
				return toggleFly(player);
			}
		}
		else {
			sender.sendMessage("This command may only be run by a player");
		}
		return true;
	}

	private boolean toggleCamera(Player player) {
		String uuid = player.getUniqueId().toString();
		CameraData data = cameraData.remove(uuid);
		
		if (data == null) {
			// set player to camera mode
			data = new CameraData(player.getLocation(), player.getGameMode());
			cameraData.put(uuid, data);
			player.setGameMode(GameMode.SPECTATOR);
			player.sendMessage("Entered camera mode");
		}
		else {
			// take player out of camera mode
			player.addPotionEffect(RESISTANCE);
			player.teleport(data.location);
			player.setGameMode(data.gamemode);
			if (data.gamemode == GameMode.SURVIVAL || data.gamemode == GameMode.ADVENTURE) {
				player.setAllowFlight(false);
			}
			player.sendMessage("Exited camera mode");
		}
		save();
		return true;
	}

	private boolean toggleSpectator(Player player) {
		String uuid = player.getUniqueId().toString();
		
		if (player.getGameMode() == GameMode.SPECTATOR) {
			player.addPotionEffect(RESISTANCE);
			GameMode gamemode = specData.remove(uuid);
			player.setGameMode(gamemode == null ? this.getServer().getDefaultGameMode() : gamemode);
			if (cameraData.containsKey(uuid)) {
				player.setAllowFlight(true);
				player.setFlying(true);
			}
		}
		else {
			specData.put(uuid, player.getGameMode());
			player.setGameMode(GameMode.SPECTATOR);
		}
		save();
		return true;
	}

	private boolean toggleFly(Player player) {
		player.addPotionEffect(RESISTANCE);
		player.setAllowFlight(!player.getAllowFlight());
		return true;
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		String uuid = player.getUniqueId().toString();
		if (cameraData.containsKey(uuid)) {
			player.setAllowFlight(true);
			player.setFlying(true);
			player.sendMessage("You are in camera mode. Flight is enabled.");
		}
	}

}
