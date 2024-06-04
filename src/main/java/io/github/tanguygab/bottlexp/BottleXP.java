package io.github.tanguygab.bottlexp;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class BottleXP extends JavaPlugin implements Listener {

    private final NamespacedKey EXP_KEY = new NamespacedKey(this,"experience");

    private String NO_PERMISSION;
    private String NOT_A_NUMBER;
    private String SUCCESS;
    private String NOT_ENOUGH_LEVELS;

    private ItemStack bottle;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();

        NO_PERMISSION = getString("no-permission","&cYou don't have permission to use this command.");
        NOT_A_NUMBER = getString("not-a-number","&cPlease enter a valid number.");
        SUCCESS = getString("success","&aYou have received an XP bottle with &b%xp% &aXP.");
        NOT_ENOUGH_LEVELS = getString("not-enough-levels","&cYou don't have enough levels!");

        bottle = new ItemStack(Material.POTION);
        if (bottle.getItemMeta() instanceof PotionMeta meta) {
            meta.setDisplayName(color(getConfig().getString("item.name", "&6XP Bottle")));
            meta.setLore(getConfig().getStringList("item.lore").stream().map(this::color).toList());
            meta.setCustomModelData(getConfig().getInt("item.model-data",0));
            java.awt.Color color = java.awt.Color.decode("#"+getConfig().getString("item.color","00FF00"));
            meta.setColor(Color.fromRGB(color.getRed(),color.getGreen(),color.getBlue()));
            try {
                meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            } catch (Error ignored) {
                meta.addItemFlags(ItemFlag.valueOf("HIDE_POTION_EFFECTS"));
            }
            bottle.setItemMeta(meta);
        }

        getServer().getPluginManager().registerEvents(this,this);
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll((Plugin) this);
    }

    private String getString(String path, String def) {
        return color(getConfig().getString("messages."+path, def));
    }

    private String color(String string) {
        return ChatColor.translateAlternateColorCodes('&', string);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length > 0 && args[0].equalsIgnoreCase("reload") && sender.hasPermission("bottlexp.reload")) {
            onDisable();
            onEnable();
            sender.sendMessage(ChatColor.GREEN+"Plugin reloaded!");
            return true;
        }


        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (!sender.hasPermission("bottlexp.use")) {
            sender.sendMessage(NO_PERMISSION);
            return true;
        }

        int levels;
        if (args.length > 0) {
            try {
                levels = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                sender.sendMessage(NOT_A_NUMBER);
                return true;
            }
        } else levels = player.getLevel();

        if (player.getLevel() < levels || levels == 0) {
            sender.sendMessage(NOT_ENOUGH_LEVELS);
            return true;
        }
        int xp = takeLevels(player,levels);

        ItemStack bottle = this.bottle.clone();
        ItemMeta meta = bottle.getItemMeta();
        meta.setLore(meta.getLore().stream().map(line->parseString(line,xp)).toList());

        meta.getPersistentDataContainer().set(EXP_KEY, PersistentDataType.INTEGER, xp);

        bottle.setItemMeta(meta);

        player.getInventory().addItem(bottle);
        sender.sendMessage(parseString(SUCCESS,xp));

        return true;
    }

    private String parseString(String string, int xp) {
        return string.replace("xp",String.valueOf(xp))
                .replace("%levels%",String.valueOf(getLevelsFromExperience(xp)));
    }

    // https://github.com/PlaceholderAPI/Player-Expansion/blob/master/src/main/java/com/extendedclip/papi/expansion/player/PlayerUtil.java#L232
    // Thanks to whoever did this
    private int getExperienceAtLevel(int level) {
        if (level <= 15) return (level << 1) + 7;
        if (level <= 30) return (level * 5) - 38;
        return (level * 9) - 158;
    }

    // Thanks https://minecraftxpcalculator.com/
    private int getLevelsFromExperience(int xp) {
        int levels = 0;
        while (xp >= 0) {
            if(levels < 16) xp -= (2 * levels) + 7;
            else if (levels < 31) xp -= (5 * levels) - 38;
            else xp -= (9 * levels) - 158;
            levels++;
        }
        return levels-1;
    }

    private int takeLevels(Player player, int levels) {
        int xp = 0;

        while (levels > 0) {
            int levelXp = getExperienceAtLevel(player.getLevel());
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