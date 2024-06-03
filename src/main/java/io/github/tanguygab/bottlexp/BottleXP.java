package io.github.tanguygab.bottlexp;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class BottleXP extends JavaPlugin implements Listener {

    private final NamespacedKey EXP_KEY = new NamespacedKey(this,"experience");

    private String NO_PERMISSION;
    private String NOT_A_NUMBER;
    private String INVALID_USAGE;
    private String SUCCESS;
    private String NOT_ENOUGH_LEVELS;

    private ItemStack bottle;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        NO_PERMISSION = getString("no-permission","&cYou don't have permission to use this command.");
        NOT_A_NUMBER = getString("not-a-number","&cPlease enter a valid number.");
        INVALID_USAGE = getString("invalid-usage","&cUsage: /bottle <levels>");
        SUCCESS = getString("success","&aYou have received an XP bottle with &b%xp% &aXP.");
        NOT_ENOUGH_LEVELS = getString("not-enough-levels","&cYou don't have enough levels!");

        bottle = new ItemStack(Material.POTION);
        if (bottle.getItemMeta() instanceof PotionMeta meta) {
            meta.setDisplayName(color(getConfig().getString("item.name", "&6XP Bottle")));
            meta.setLore(getConfig().getStringList("item.lore").stream().map(this::color).toList());
            meta.setCustomModelData(getConfig().getInt("item.model-data",0));
            java.awt.Color color = java.awt.Color.decode("#"+getConfig().getString("item.color","00FF00"));
            meta.setColor(Color.fromRGB(color.getRed(),color.getGreen(),color.getBlue()));
            bottle.setItemMeta(meta);
        }

        getServer().getPluginManager().registerEvents(this,this);
    }

    private String getString(String path, String def) {
        return color(getConfig().getString("messages."+path, def));
    }

    private String color(String string) {
        return ChatColor.translateAlternateColorCodes('&', string);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (!sender.hasPermission("bottlexp.use")) {
            sender.sendMessage(NO_PERMISSION);
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(INVALID_USAGE);
            return true;
        }

        if (player.getLevel() < 1) {
            sender.sendMessage(NOT_ENOUGH_LEVELS);
            return true;
        }

        int levels;
        try {
            levels = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            sender.sendMessage(NOT_A_NUMBER);
            return true;
        }

        if (player.getLevel() < levels) {
            sender.sendMessage(NOT_ENOUGH_LEVELS);
            return true;
        }
        int xp = takeLevels(player,levels);

        ItemStack bottle = this.bottle.clone();
        ItemMeta meta = bottle.getItemMeta();
        meta.setLore(meta.getLore().stream().map(line->line.replace("%xp%", String.valueOf(xp))).toList());

        meta.getPersistentDataContainer().set(EXP_KEY, PersistentDataType.INTEGER, xp);

        bottle.setItemMeta(meta);

        player.getInventory().addItem(bottle);
        sender.sendMessage(SUCCESS.replace("%xp%", String.valueOf(xp)));

        return true;
    }

    // https://github.com/PlaceholderAPI/Player-Expansion/blob/master/src/main/java/com/extendedclip/papi/expansion/player/PlayerUtil.java#L232
    // Thanks to whoever did this
    private int getExperienceAtLevel(int level) {
        if (level <= 15) return (level << 1) + 7;
        if (level <= 30) return (level * 5) - 38;
        return (level * 9) - 158;
    }

    private int takeLevels(Player player, int levels) {
        int xp = 0;

        while (levels > 0) {
            int levelXp = getExperienceAtLevel(player.getLevel()-1);
            xp += levelXp;
            player.giveExp(-levelXp);
            --levels;
        }
        return xp;
    }

    @EventHandler
    public void onBottle(PlayerItemConsumeEvent e) {
        ItemStack item = e.getItem();
        if (item.getType() != Material.POTION) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        if (!meta.getPersistentDataContainer().has(EXP_KEY)) return;
        int xp = meta.getPersistentDataContainer().getOrDefault(EXP_KEY,PersistentDataType.INTEGER,0);
        Player player = e.getPlayer();
        player.giveExp(xp);
        e.setItem(null);
    }

}