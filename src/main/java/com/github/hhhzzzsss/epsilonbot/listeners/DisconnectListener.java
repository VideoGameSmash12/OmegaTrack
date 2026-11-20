package com.github.hhhzzzsss.epsilonbot.listeners;

import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;

public interface DisconnectListener {
	public void onDisconnected(DisconnectedEvent packet);
}
