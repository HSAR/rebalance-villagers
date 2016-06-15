package com.hotmail.wolfiemario.rebalancevillagers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.minecraft.server.EntityVillager;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * The Rebalance Villagers plugin's main class.
 * @author Gerrard Lukacs
 */
public class RebalanceVillagers extends JavaPlugin implements Listener
{
	private ConfigLoader configLoader;
	
	FileConfiguration offerConfig;
	File offerConfigFile;
	static final String OFFER_CONFIG_FILENAME = "offers.yml";
	static final String OFFER_DEFAULT_CONFIG_FILENAME = "offers-default.yml";
	static final String OFFER_VANILLA_CONFIG_FILENAME = "offers-vanilla.yml";
	
	public boolean allowDamage;
	public static final Integer[] DEFAULT_ALLOWED_PROFESSIONS = {0,1,2,3,4};
	public Integer[] allowedProfessions;
	
	private Plugin shopkeepersPlugin; //A handle of Shopkeepers, for compatibility.
	@SuppressWarnings("rawtypes")
	private Map activeShopkeepers; //A Shopkeepers field into which active shopkeepers are placed upon spawning.
	int shopkeeperCheckAttempts; //Number of times to check if a CUSTOM-spawned villager has been registered as a shopkeeper.
	int shopkeeperCheckDelay; //The time in milliseconds before checking whether a Shopkeeper is registered.
	
	private static final String PLUGIN_NAME = "Rebalance Villagers";
	private static final String SHOPKEEPERS_NAME = "Shopkeepers";
	
	/**
	 * Initializes the plugin.
	 */
	public RebalanceVillagers()
	{
		offerConfig = null;
		offerConfigFile = null;
		configLoader = new ConfigLoader(this);
	}
	
	public void onEnable()
	{
		//Load config
		File file = new File(getDataFolder(), "config.yml");
		if(!file.exists())
			saveDefaultConfig();
		reloadConfig();
		//Load offer config
		File offerFile = new File(getDataFolder(), OFFER_CONFIG_FILENAME);
		if(!offerFile.exists())
			saveDefaultOfferConfig();
		reloadOfferConfig();
		//Save sample offer configs
		saveSampleOfferConfigs();
		
		getLogger().info(PLUGIN_NAME + " has been enabled, and configuration has loaded.");
		
		try
		{
			//Replaces default villagers with new villagers
			//Thanks to Icyene for the help with this! Also http://forums.bukkit.org/threads/tutorial-how-to-customize-the-behaviour-of-a-mob-or-entity.54547/
			Method entityTypesA = net.minecraft.server.EntityTypes.class.getDeclaredMethod("a", Class.class, String.class, int.class);
			entityTypesA.setAccessible(true);
			entityTypesA.invoke(entityTypesA, BalancedVillager.class, "Villager", 120);
			
			//Checks if Shopkeepers is running
			connectWithShopkeepers();
			
			//Configure Plugin
			configLoader.applyConfig();
			configLoader.applyOfferConfig();
			
			//Convert existing villagers to balanced villagers
			convertExistingVillagers();
			getLogger().info("Existing villagers have been reloaded as BalancedVillagers.");
			
			//Registers this as a listener
			getServer().getPluginManager().registerEvents(this, this);
			
			getLogger().info(PLUGIN_NAME + " has finished loading.");
		}
		catch(Exception e)
		{
			e.printStackTrace();
			getLogger().info("Failed to modify villagers! Plugin is unloading.");
			this.setEnabled(false);
		}
	}

	public void onDisable()
	{
		getLogger().info(PLUGIN_NAME + " has been disabled.");
	}
	
	/**
	 * Reloads the plugin.
	 */
	public void reload() //TODO
	{
		onDisable();
		onEnable();
	}
	
	/**
	 * Listens to creatures which are spawned, and kills new EntityVillagers, replacing them with identical BalancedVillagers.
	 * <br>Avoids killing Shopkeepers.
	 */
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onCreatureSpawn(CreatureSpawnEvent event)
	{
		if (event.isCancelled())
			return;
		
		Entity entity = event.getEntity();
		EntityType entityType = event.getEntityType();
		
		net.minecraft.server.World mcWorld = ((CraftWorld) entity.getWorld()).getHandle();
		net.minecraft.server.Entity mcEntity = (((CraftEntity) entity).getHandle());
		
		if (entityType == EntityType.VILLAGER)
		{
			EntityVillager entityVil = (EntityVillager)mcEntity;
			
			//This should only occur if convertVillager triggered this event. This is unnecessary repetition; skip it.
			if(mcEntity instanceof BalancedVillager && event.getSpawnReason().equals(SpawnReason.CUSTOM))
				return;
			
			if(!Arrays.equals(allowedProfessions, DEFAULT_ALLOWED_PROFESSIONS)) //If we're not on the default set of professions, override
				entityVil.setProfession(allowedProfessions[new java.util.Random().nextInt(allowedProfessions.length)]); //mouthful
			
			//Not a BalancedVillager yet!
			if((mcEntity instanceof BalancedVillager) == false)
			{				
				//Check if this is a Shopkeeper
				if(activeShopkeepers != null && event.getSpawnReason().equals(SpawnReason.CUSTOM))
				{		
					Thread shopkeeperWaiter = new Thread(new ShopkeeperWaiter(entityVil, mcWorld));
					shopkeeperWaiter.start();
					return;
				}
					
				convertVillager(entityVil, mcWorld);
			}
			
		}
	}
	
	/**
	 * Listens to entity damage, canceling the event if the target is a BalancedVillager and the plugin is
	 * configured to make them invulnerable.
	 */
	@EventHandler
	public void onEntityDamage(EntityDamageEvent event)
	{
		if(!allowDamage && (((CraftEntity)event.getEntity()).getHandle()) instanceof BalancedVillager)
			event.setCancelled(true);
	}
	
	/**
	 * Attempts to prepare for interactions with the Shopkeepers plugin, if it is loaded.
	 */
	@SuppressWarnings("rawtypes")
	private void connectWithShopkeepers()
	{
		shopkeepersPlugin = getServer().getPluginManager().getPlugin(SHOPKEEPERS_NAME);
		if (shopkeepersPlugin != null)
		{
			getLogger().info(SHOPKEEPERS_NAME + " has been detected.");
			
			try
			{
				Field activeShopkeepersField = shopkeepersPlugin.getClass().getDeclaredField("activeShopkeepers");
				activeShopkeepersField.setAccessible(true);
				activeShopkeepers = (Map)activeShopkeepersField.get(shopkeepersPlugin);
				getLogger().info("Successfully connected to Shopkeepers; custom shopkeepers will not be altered by this plugin.");
			}
			catch(Exception e)
			{
				getLogger().info("Could not properly connect with Shopkeepers - Incorrect version?");
			}
		}
	}
	
	/**
	 * Converts all existing (non-Shopkeeper) villagers in all worlds to BalancedVillagers.
	 */
	private void convertExistingVillagers()
	{
		List<World> worldList = getServer().getWorlds();
		
		for(World world: worldList)
		{
			Collection<Villager> villagerList = world.getEntitiesByClass(Villager.class);
			 
			net.minecraft.server.World mcWorld = ((CraftWorld) world).getHandle();
			
			for(Villager vil: villagerList)
			{
				//Detect Shopkeepers even on startup!
				if(activeShopkeepers == null || !activeShopkeepers.containsKey(vil.getEntityId()))
				{
					EntityVillager entityVil = (EntityVillager)((CraftEntity)vil).getHandle();
				
					convertVillager(entityVil, mcWorld);
				}
			}
		}
	}
	
	/**
	 * Converts the given villager into a BalancedVillager, leaving an identical villager.
	 * Because the previous villager is removed, the new villager will have a different unique ID.
	 */
	private void convertVillager(EntityVillager vil, net.minecraft.server.World mcWorld)
	{
		Location location = vil.getBukkitEntity().getLocation();
		
		BalancedVillager balancedVil = new BalancedVillager(vil);
		balancedVil.setPosition(location.getX(), location.getY(), location.getZ());
		
		mcWorld.removeEntity(vil);
		mcWorld.addEntity(balancedVil, SpawnReason.CUSTOM);
	}
	
	/**
	 * An attempt to wait for a newly spawned Villager to be added to activeShopkeepers, so we can tell whether it was indeed a Shopkeeper.
	 * Having learned about the Bukkit scheduler from nisovin, I intend to change this mechanic, as multithreading leads to a potential
	 * ConcurrentModificationException.
	 * @author Gerrard Lukacs
	 */
	private class ShopkeeperWaiter implements Runnable
	{
		private EntityVillager villager;
		private net.minecraft.server.World mcWorld;
		
		private ShopkeeperWaiter(EntityVillager vil, net.minecraft.server.World world)
		{
			villager = vil;
			mcWorld = world;
		}
		
		public void run()
		{
			try
			{
				for(int i = 0; i < shopkeeperCheckAttempts; i++)
				{
					Thread.sleep(shopkeeperCheckDelay);
					
					if(activeShopkeepers.containsKey(villager.getBukkitEntity().getEntityId()))
					{
						//getLogger().info("Shopkeeper found.");
						return;
					}
				}
				
				//getLogger().info("I think this isn't a Shopkeeper.");
				convertVillager(villager, mcWorld);
			}
			catch (InterruptedException e)
			{
				getLogger().info("Thread interruption: No clue how you just managed that."); //Seriously, assuming no reflection, that shouldn't be possible.
				e.printStackTrace();
			}
		}
	}
	
	//Methods for offers' custom config file
	/**
	 * Reloads the offers.yml config.
	 * @see org.bukkit.plugin.java.JavaPlugin.reloadConfig()
	 */
	public void reloadOfferConfig()
	{
		if (offerConfigFile == null)
			offerConfigFile = new File(getDataFolder(), OFFER_CONFIG_FILENAME);
		
		offerConfig = YamlConfiguration.loadConfiguration(offerConfigFile);
		
		// Look for defaults in the jar
		InputStream defConfigStream = this.getResource(OFFER_CONFIG_FILENAME);
		if (defConfigStream != null)
		{
			YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
			offerConfig.setDefaults(defConfig);
		}
	}
	
	/**
	 * Gets the offers.yml config.
	 * @see org.bukkit.plugin.java.JavaPlugin.getConfig()
	 */
	public FileConfiguration getOfferConfig()
	{
		if (offerConfig == null)
		{
			this.reloadOfferConfig();
		}
		return offerConfig;
	}
	
	/**
	 * Saves the offers.yml config.
	 * @see org.bukkit.plugin.java.JavaPlugin.saveConfig()
	 */
	public void saveOfferConfig()
	{
		if (offerConfig == null || offerConfigFile == null)
			return;
		
		try
		{
			getOfferConfig().save(offerConfigFile);
		}
		catch (IOException ex)
		{
			this.getLogger().log(Level.SEVERE, "Could not save config to " + offerConfigFile, ex);
		}
	}
	
	/**
	 * Saves the default offers.yml config.
	 * @see org.bukkit.plugin.java.JavaPlugin.saveDefaultConfig()
	 */
	public void saveDefaultOfferConfig()
	{
		saveResource(OFFER_CONFIG_FILENAME, false);
	}
	
	/**
	 * Saves two sample config files: offers-default.yml (a copy of offers.yml) and offers-vanilla.yml.
	 * @see org.bukkit.plugin.java.JavaPlugin.saveDefaultConfig()
	 */
	public void saveSampleOfferConfigs()
	{
		File defaultConfig = new File(getDataFolder(), OFFER_DEFAULT_CONFIG_FILENAME);
		if(!defaultConfig.exists())
			saveResourceCopy(OFFER_CONFIG_FILENAME, OFFER_DEFAULT_CONFIG_FILENAME);
		
		File vanillaConfig = new File(getDataFolder(), OFFER_VANILLA_CONFIG_FILENAME);
		if(!vanillaConfig.exists())
		saveResource(OFFER_VANILLA_CONFIG_FILENAME, false);
	}
	
	/**
	 * Saves a resource to a custom destination name.
	 * <br>Mostly copied from saveResource, but without exception throwing as this isn't in an API.
	 * @param resourceName - The name of the resource to save a copy of (not a path!)
	 * @param destName - The destination name to which the resource is saved (again, not a path!)
	 * @see org.bukkit.plugin.java.JavaPlugin.saveResource(String arg0, boolean arg1)
	 */
	public void saveResourceCopy(String resourceName, String destName)
	{
		InputStream in = getResource(resourceName);
		
		File outFile = new File(getDataFolder(), destName);
		File outDir = getDataFolder();
		if (!outDir.exists())
		{
			outDir.mkdirs();
		}
		
		try
		{
			if (!outFile.exists())
			{
				OutputStream out = new FileOutputStream(outFile);
				byte[] buf = new byte[1024];
				int len;
				while ((len = in.read(buf)) > 0)
				{
					out.write(buf, 0, len);
				}
				out.close();
				in.close();
			}
			else
				Logger.getLogger(JavaPlugin.class.getName()).log(Level.WARNING, "Could not save " + outFile.getName() + " to " + outFile + " because " + outFile.getName() + " already exists.");
		}
		catch(IOException ex)
		{
			Logger.getLogger(JavaPlugin.class.getName()).log(Level.SEVERE, "Could not save " + outFile.getName() + " to " + outFile, ex);
		}
		
	}
	
}
