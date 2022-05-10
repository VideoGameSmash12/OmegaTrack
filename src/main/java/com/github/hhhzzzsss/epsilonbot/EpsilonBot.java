package com.github.hhhzzzsss.epsilonbot;

import com.github.hhhzzzsss.epsilonbot.block.World;
import com.github.hhhzzzsss.epsilonbot.command.CommandList;
import com.github.hhhzzzsss.epsilonbot.command.commands.*;
import com.github.hhhzzzsss.epsilonbot.listeners.*;
import com.github.hhhzzzsss.epsilonbot.modules.*;
import com.github.steveice10.mc.auth.exception.request.RequestException;
import com.github.steveice10.mc.auth.service.AuthenticationService;
import com.github.steveice10.mc.auth.service.MsaAuthenticationService;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatPacket;
import com.github.steveice10.mc.protocol.packet.login.clientbound.ClientboundGameProfilePacket;
import com.github.steveice10.packetlib.ProxyInfo;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.ConnectedEvent;
import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import com.github.steveice10.packetlib.packet.Packet;
import com.github.steveice10.packetlib.tcp.TcpClientSession;
import io.netty.util.concurrent.FastThreadLocal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class EpsilonBot {
	@Getter @Setter private String username;
	@Getter @Setter private UUID uuid;
	@Getter private final String host;
	@Getter private final int port;
	@Getter @Setter private ProxyInfo PROXY_INFO = null;
	@Getter private final String OAUTH_CLIENT_ID = "389b1b32-b5d5-43b2-bddc-84ce938d6737"; // token from https://github.com/microsoft/Office365APIEditor
	@Getter private Session session = null;
	@Getter private boolean running = true;
	@Getter private boolean loggedIn = false;
	@Getter @Setter private boolean autoRelog = true;
	
	@Getter private ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
	private ArrayList<PacketListener> packetListeners = new ArrayList<>();
	private ArrayList<TickListener> tickListeners = new ArrayList<>();
	private ArrayList<DisconnectListener> disconnectListeners = new ArrayList<>();
	private Queue<Packet> packetQueue = new ConcurrentLinkedQueue<>();

	@Getter protected ChatLogger chatLogger = new ChatLogger(this, true);
	@Getter protected ChatQueue chatQueue = new ChatQueue(this);
	@Getter protected StateManager stateManager = new StateManager(this);
	@Getter protected PositionManager posManager = new PositionManager(this);
	@Getter protected World world = new World(this);
	@Getter protected CommandList commandList = new CommandList();
	@Getter protected ChatCommandHandler chatCommandHandler = new ChatCommandHandler(this, commandList, "`");
	@Getter protected PlotBuilder plotBuilder = new PlotBuilder(this);
	
	public EpsilonBot() {
		this.host = Config.getConfig().getHost();
		this.port = Config.getConfig().getPort();
		loadCommands();
	}
	
	public void start() {
		getListeners();
		scheduleTicking();
		connect();
	}
	
	private void connect() {
		MinecraftProtocol protocol;
		try {
			AuthenticationService authService = new MsaAuthenticationService(OAUTH_CLIENT_ID);
			authService.setUsername(Config.getConfig().getUsername());
			authService.setPassword(Config.getConfig().getPassword());
			authService.login();
			protocol = new MinecraftProtocol(authService.getSelectedProfile(), authService.getAccessToken());
			System.out.println("Successfully authenticated user.");
		} catch (Throwable e) {
			chatLogger.log("Error: failed to authenticate user. Restarting...");
			Main.restartBot();
			return;
		}
		session = new TcpClientSession(host, port, protocol, PROXY_INFO);
		session.addListener(new SessionAdapter() {
			@Override
            public synchronized void packetReceived(Session session, Packet packet) {
				packetQueue.add(packet);
			}

			@Override
		    public void disconnected(DisconnectedEvent event) {
				FastThreadLocal.removeAll();
				executor.submit(() -> {
					FastThreadLocal.removeAll();
					processDisconnect(event);
				});
		    }
		});
		session.connect();
	}

	// loop through the fields of the class and all of its superclasses up until the Bot class
	private void getListeners() {
		Class<?> c = this.getClass();
		do {
			for (Field field : c.getDeclaredFields()) {
				field.setAccessible(true);
				Object value;
				try {
					value = field.get(this);
				} catch (IllegalArgumentException | IllegalAccessException e) {
					System.err.println("Error when accesing field: " + field.getName());
					e.printStackTrace();
					continue;
				}
				field.setAccessible(false);
				
				if (value instanceof PacketListener) {
					packetListeners.add((PacketListener) value);
				}
				if (value instanceof TickListener) {
					tickListeners.add((TickListener) value);
				}
				if (value instanceof DisconnectListener) {
					disconnectListeners.add((DisconnectListener) value);
				}
			}
			c = c.getSuperclass();
		} while (EpsilonBot.class.isAssignableFrom(c));
	}
	
	private void scheduleTicking() {
		executor.scheduleAtFixedRate(() -> {
			try {
				processTick();
			}
			catch (Throwable e) {
				e.printStackTrace();
			}
		}, 0, 50, TimeUnit.MILLISECONDS);
	}

	// Packets are processed at the beginning of ticks
	private void processTick() {
		int queueLength = packetQueue.size();
		for (int i=0; i<queueLength; i++) {
			processPacket(packetQueue.poll());
		}
		
		if (loggedIn) {
			for (TickListener listener : tickListeners) {
				try {
					listener.onTick();
				}
				catch (Throwable e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public interface PacketFuture {
		boolean onPacket(Packet p);
	}
	
	@AllArgsConstructor
	private class ExpirablePacketFuture implements PacketFuture {
		private PacketFuture future;
		private long expiration;
		@Override
		public boolean onPacket(Packet packet) {
			if (System.currentTimeMillis() < expiration) {
				return future.onPacket(packet);
			}
			else {
				return false;
			}
		}
	}
	
	public List<PacketFuture> packetFutures = new LinkedList<>();
	private void processPacket(Packet packet) {
		if (packet instanceof ClientboundGameProfilePacket) {
			ClientboundGameProfilePacket t_packet = (ClientboundGameProfilePacket) packet;
			uuid = t_packet.getProfile().getId();
		} else if (packet instanceof ClientboundLoginPacket) {
			loggedIn = true;
		}

		for (PacketListener listener : packetListeners) {
			try {
				listener.onPacket(packet);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		synchronized (packetFutures) {
			Iterator<PacketFuture> itr = packetFutures.iterator();
			while (itr.hasNext()) {
				PacketFuture future = itr.next();
				if (future.onPacket(packet)) {
					itr.remove();
				}
			}
		}
	}
	
	private void processDisconnect(DisconnectedEvent event) {
		loggedIn = false;

		for (DisconnectListener listener : disconnectListeners) {
			try {
				listener.onDisconnected(event);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		if (autoRelog) {
			if (event.getReason().contains("Wait 5 seconds before connecting, thanks! :)") || event.getReason().contains("Connection throttled! Please wait before reconnecting.")) {
				executor.schedule(() -> {
					connect();
				}, 5, TimeUnit.SECONDS);
			}
			else {
				executor.schedule(() -> {
					connect();
				}, 1, TimeUnit.SECONDS);
			}
		}
		else {
			stop();
		}
	}
	
	/**
	 * A thread-safe way to schedule an task that waits for a certain packet event before running.
	 * 
	 * @param future The packet to schedule
	 */
	public void schedulePacketFuture(PacketFuture future) {
		synchronized(packetFutures) {
			packetFutures.add(future);
		}
	}
	
	/**
	 * A thread-safe way to schedule an task that waits for a certain packet event before running.
	 * 
	 * @param future The packet to schedule
	 */
	public void schedulePacketFuture(PacketFuture future, long timeout) {
		synchronized(packetFutures) {
			packetFutures.add(new ExpirablePacketFuture(future, timeout));
		}
	}
	
	public void sendPacket(Packet packet) {
		session.send(packet);
	}
	
	public void sendChat(String chat) {
		chatQueue.sendChat(chat);
	}
	
	public void sendChatAsync(String chat) {
		executor.submit(() -> {
			sendChat(chat);
		});
	}
	
	public void sendCommand(String command) {
		chatQueue.sendCommand(command);
	}
	
	public void sendChatInstantly(String chat) {
		sendPacket(new ServerboundChatPacket(chat));
	}
	
	public void stop() {
		running = false;
		autoRelog = false;
		try {
			session.disconnect("bye");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		executor.shutdownNow();
	}
	
	public void relog() {
		try {
			session.disconnect("bye");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
		return executor.awaitTermination(timeout, unit);
	}
	
	public String toString() {
		return String.format("%s (%s|%s:%d)", this.getClass().getName(), username, host, port);
	}

	private void loadCommands() {
		commandList.add(new HelpCommand(this));
		commandList.add(new CreatorCommand(this));

		commandList.add(new TestCommand(this));

		commandList.add(new BuildStatusCommand(this));
		commandList.add(new ReloadIndexCommand(this));
		commandList.add(new RepairCommand(this));

		commandList.add(new RestartCommand(this));
		commandList.add(new StopCommand(this));
	}
}
