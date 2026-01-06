package com.forge;

import org.bukkit.plugin.java.JavaPlugin;
import com.forge.command.DtCommand;
import com.forge.command.QdCommand;
import com.forge.storage.Database;
import com.forge.storage.PlayerDao;
import com.forge.store.YamlStore;
import com.forge.service.*;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;

public class ForgePlugin extends JavaPlugin {
    private static ForgePlugin inst;
    private Database database;
    private PlayerDao playerDao;
    private YamlStore yamlStore;
    private MessageService messages;
    private AttributeService attributes;
    private MappingService mappings;
    private TemplateService templates;
    private MaterialService materials;
    private RecipeService recipes;
    private SessionService sessions;
    private ForgingService forging;
    private AttributeAdapterService adapters;
    private GemService gems;
    private GuiConfigService guiConfig;
    public static ForgePlugin get() { return inst; }
    public Database database() { return database; }
    public PlayerDao playerDao() { return playerDao; }
    public YamlStore yaml() { return yamlStore; }
    public MessageService msg() { return messages; }
    public AttributeService attributes() { return attributes; }
    public MappingService mappings() { return mappings; }
    public TemplateService templates() { return templates; }
    public MaterialService materials() { return materials; }
    public RecipeService recipes() { return recipes; }
    public SessionService sessions() { return sessions; }
    public ForgingService forging() { return forging; }
    public AttributeAdapterService adapters() { return adapters; }
    public GemService gems() { return gems; }
    public GuiConfigService guiConfig() { return guiConfig; }
    @Override
    public void onEnable() {
        inst = this;
        saveDefaultConfig();
        yamlStore = new YamlStore(this);
        database = new Database(this);
        playerDao = new PlayerDao(database);
        messages = new MessageService(this);
        attributes = new AttributeService(yamlStore);
        mappings = new MappingService(yamlStore);
        guiConfig = new GuiConfigService(this);
        templates = new TemplateService(yamlStore);
        sessions = new SessionService();
        materials = new MaterialService(yamlStore, getConfig(), sessions, attributes);
        adapters = new AttributeAdapterService(yamlStore);
        recipes = new RecipeService(yamlStore);
        forging = new ForgingService(this);
        gems = new GemService(this);
        
        getCommand("dt").setExecutor(new DtCommand(this));
        getCommand("dt").setTabCompleter(new DtCommand(this));
        getCommand("qd").setExecutor(new QdCommand(this));
        getCommand("gems").setExecutor(new com.forge.command.GemsCommand(this));
        
        Bukkit.getPluginManager().registerEvents(new com.forge.listener.GuiListener(this), this);
        Bukkit.getPluginManager().registerEvents(new com.forge.listener.ChatCaptureListener(this), this);
        Bukkit.getPluginManager().registerEvents(new com.forge.listener.GemListener(this), this);
        getLogger().info("加qq群1078284723，获取技术支持与最新信息");
    }
    @Override
    public void onDisable() {
        if (sessions != null) sessions.restoreAll();
        HandlerList.unregisterAll(this);
        database.close();
    }
}
