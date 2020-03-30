package it.multicoredev.spigot;

import it.multicoredev.mclib.db.MySQL;
import it.multicoredev.mclib.yaml.Configuration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Copyright © 2019 by Lorenzo Magni
 * This file is part of RTP.
 * RTP is under "The 3-Clause BSD License", you can find a copy <a href="https://opensource.org/licenses/BSD-3-Clause">here</a>.
 * <p>
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
public class RTP extends JavaPlugin {
    public static Configuration config;
    public static MySQL db;

    public void onEnable() {
        try {
            initConfig();
        } catch (IOException e) {
            e.printStackTrace();
            onDisable();
            return;
        }

        try {
            initDb();
        } catch (SQLException e) {
            e.printStackTrace();
            onDisable();
            return;
        }

        getCommand("rtp").setExecutor((sender, command, label, args) -> {
            if (!(sender instanceof Player)) {
                Chat.send(config.getString("messages.not-player"), sender, true);
                return true;
            }

            Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                try {
                    Player player = (Player) sender;

                    if (db.lineExists("uuid", player.getUniqueId())) {
                        Chat.send(config.getString("messages.one-command"), player, true);
                        return;
                    }

                    Chat.send(config.getString("messages.teleport"), player, true);
                    Location spawn = Bukkit.getWorld("world").getSpawnLocation();
                    Bukkit.getScheduler().callSyncMethod(RTP.this, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "spreadplayers " +
                            spawn.getX() + " " +
                            spawn.getZ() +
                            " 5000 20000 false " + player.getName()));
                    db.addLine("uuid", player.getUniqueId());
                } catch (SQLException ignored) {
                    Chat.send(config.getString("messages.command-error"), sender, true);
                }
            });
            return true;
        });
    }

    private void initConfig() throws IOException {
        config = new Configuration(new File(getDataFolder(), "config.yml"), getResource("config.yml"));

        if (!getDataFolder().exists() || !getDataFolder().isDirectory()) {
            if (!getDataFolder().mkdir()) throw new IOException("Cannot create config folder");
        }

        config.autoload();
    }

    private void initDb() throws SQLException {
        String host = config.getString("mysql.host");
        int port = config.getInt("mysql.port");
        String database = config.getString("mysql.database");
        String username = config.getString("mysql.username");
        String password = config.getString("mysql.password");
        String prefix = config.getString("mysql.prefix");

        db = new MySQL(host, port, database, username, password, prefix + "parties");
        db.createTable(new String[]{
                "`uuid` VARCHAR(100)"
        }, "utf8mb4");
    }
}
