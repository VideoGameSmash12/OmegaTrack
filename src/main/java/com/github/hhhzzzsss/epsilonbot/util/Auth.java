package com.github.hhhzzzsss.epsilonbot.util;

import org.geysermc.mcprotocollib.protocol.MinecraftProtocol;

public class Auth {
	public static MinecraftProtocol login(String username) throws Exception {
		return loginOffline(username);
	}

	public static MinecraftProtocol loginOffline(String username) {
		return new MinecraftProtocol(username);
	}
}