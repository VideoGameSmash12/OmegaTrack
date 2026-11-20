package com.github.hhhzzzsss.epsilonbot.listeners;

import org.geysermc.mcprotocollib.network.packet.Packet;

public interface PacketListener {
	public void onPacket(Packet packet);
}
