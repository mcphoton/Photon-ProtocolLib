package org.spacehq.mc.protocol;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mcphoton.mc.protocol.MinecraftConstants.GAME_VERSION;
import static org.mcphoton.mc.protocol.MinecraftConstants.PROTOCOL_VERSION;
import static org.mcphoton.mc.protocol.MinecraftConstants.SERVER_COMPRESSION_THRESHOLD;
import static org.mcphoton.mc.protocol.MinecraftConstants.SERVER_INFO_BUILDER_KEY;
import static org.mcphoton.mc.protocol.MinecraftConstants.SERVER_INFO_HANDLER_KEY;
import static org.mcphoton.mc.protocol.MinecraftConstants.SERVER_LOGIN_HANDLER_KEY;
import static org.mcphoton.mc.protocol.MinecraftConstants.VERIFY_USERS_KEY;
import static org.mcphoton.mc.protocol.data.SubProtocol.STATUS;
import static org.mcphoton.mc.protocol.data.game.entity.player.GameMode.SURVIVAL;
import static org.mcphoton.mc.protocol.data.game.setting.Difficulty.PEACEFUL;
import static org.mcphoton.mc.protocol.data.game.world.WorldType.DEFAULT;
import static org.spacehq.mc.protocol.ByteBufHelper.assertBlock;
import static org.spacehq.mc.protocol.ByteBufHelper.assertPosition;
import static org.spacehq.mc.protocol.ByteBufHelper.writeAndRead;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mcphoton.mc.protocol.MinecraftProtocol;
import org.mcphoton.mc.protocol.ServerLoginHandler;
import org.mcphoton.mc.protocol.data.game.entity.metadata.Position;
import org.mcphoton.mc.protocol.data.game.world.block.BlockChangeRecord;
import org.mcphoton.mc.protocol.data.game.world.block.BlockState;
import org.mcphoton.mc.protocol.data.message.TextMessage;
import org.mcphoton.mc.protocol.data.status.PlayerInfo;
import org.mcphoton.mc.protocol.data.status.ServerStatusInfo;
import org.mcphoton.mc.protocol.data.status.VersionInfo;
import org.mcphoton.mc.protocol.data.status.handler.ServerInfoBuilder;
import org.mcphoton.mc.protocol.data.status.handler.ServerInfoHandler;
import org.mcphoton.mc.protocol.packet.ingame.server.ServerJoinGamePacket;
import org.mcphoton.mc.protocol.packet.ingame.server.world.ServerBlockChangePacket;
import org.mcphoton.packetlib.Client;
import org.mcphoton.packetlib.NetworkServer;
import org.mcphoton.packetlib.Session;
import org.mcphoton.packetlib.event.session.DisconnectedEvent;
import org.mcphoton.packetlib.event.session.PacketReceivedEvent;
import org.mcphoton.packetlib.event.session.SessionListener;
import org.mcphoton.packetlib.packet.Packet;
import org.mcphoton.packetlib.tcp.TcpSessionFactory;
import org.spacehq.mc.auth.data.GameProfile;

public class MinecraftProtocolTest {
	private static final String HOST = "localhost";
	private static final int PORT = 25560;

	private static NetworkServer server;
	private Client client;

	@BeforeClass
	public static void setUpServer() {
		server = new NetworkServer(HOST, PORT, MinecraftProtocol.class, new TcpSessionFactory());
		server.setGlobalFlag(VERIFY_USERS_KEY, false);
		server.setGlobalFlag(SERVER_COMPRESSION_THRESHOLD, 100);
		server.setGlobalFlag(SERVER_INFO_BUILDER_KEY, new ServerInfoBuilder() {
			@Override
			public ServerStatusInfo buildInfo(Session session) {
				return new ServerStatusInfo(new VersionInfo(GAME_VERSION, PROTOCOL_VERSION),
						new PlayerInfo(100, 0, new GameProfile[0]), new TextMessage("Hello world!"), null);
			}
		});

		server.setGlobalFlag(SERVER_LOGIN_HANDLER_KEY, new ServerLoginHandler() {
			@Override
			public void loggedIn(Session session) {
				session.send(new ServerJoinGamePacket(0, false, SURVIVAL, 0, PEACEFUL, 100, DEFAULT, false));
			}
		});

		assertTrue("Could not bind server.", server.bind().isListening());
	}

	@Test
	public void testStatus() throws InterruptedException {
		client = new Client(HOST, PORT, new MinecraftProtocol(STATUS), new TcpSessionFactory());
		Session session = client.getSession();

		ServerInfoHandlerTest handler = new ServerInfoHandlerTest();
		session.setFlag(SERVER_INFO_HANDLER_KEY, handler);
		session.addListener(new DisconnectListener());
		session.connect();

		assertTrue("Could not connect status session.", session.isConnected());
		handler.status.await(2, SECONDS);

		ServerStatusInfo info = handler.info;
		assertNotNull("Failed to get server info.", info);

		assertEquals("Received incorrect description.", "Hello world!", info.getDescription().getFullText());
		assertEquals("Received incorrect game version.", GAME_VERSION,
				info.getVersionInfo().getVersionName());
		assertEquals("Received incorrect protocol version.", PROTOCOL_VERSION,
				info.getVersionInfo().getProtocolVersion());
		assertEquals("Received incorrect online players.", 0, info.getPlayerInfo().getOnlinePlayers());
		assertEquals("Received incorrect max players.", 100, info.getPlayerInfo().getMaxPlayers());
	}

	@Test
	public void testLogin() throws InterruptedException {
		Client client = new Client(HOST, PORT, new MinecraftProtocol("test"), new TcpSessionFactory());
		Session session = client.getSession();

		LoginListenerTest listener = new LoginListenerTest();
		session.addListener(listener);
		session.addListener(new DisconnectListener());
		session.connect();

		assertTrue("Could not connect login session.", session.isConnected());
		listener.login.await(4, SECONDS);

		ServerJoinGamePacket packet = listener.packet;
		assertNotNull("Failed to log in.", packet);

		assertEquals("Received incorrect entity ID.", 0, packet.getEntityId());
		assertFalse("Received incorrect hardcore flag.", packet.getHardcore());
		assertEquals("Received incorrect gamemode.", SURVIVAL, packet.getGameMode());
		assertEquals("Received incorrect dimension.", 0, packet.getDimension());
		assertEquals("Received incorrect difficulty.", PEACEFUL, packet.getDifficulty());
		assertEquals("Received incorrect max players.", 100, packet.getMaxPlayers());
		assertEquals("Received incorrect world type.", DEFAULT, packet.getWorldType());
		assertFalse("Received incorrect reduced debug info flag.", packet.getReducedDebugInfo());
	}

	@Test
	public void testBlockBreak() throws IOException {
		BlockChangeRecord record = new BlockChangeRecord(new Position(1, 61, -1), new BlockState(3, 2));
		ServerBlockChangePacket packet = writeAndRead(new ServerBlockChangePacket(record));

		assertPosition(packet.getRecord().getPosition(), 1, 61, -1);
		assertBlock(packet.getRecord(), 3, 2);
	}

	@After
	public void tearDownClient() {
		if (this.client != null) {
			this.client.getSession().disconnect("Bye!");
		}
	}

	@AfterClass
	public static void tearDownServer() {
		server.close(true);
	}

	private static class ServerInfoHandlerTest implements ServerInfoHandler {
		public CountDownLatch status = new CountDownLatch(1);
		public ServerStatusInfo info;

		@Override
		public void handle(Session session, ServerStatusInfo info) {
			this.info = info;
			this.status.countDown();
		}
	}

	private static class LoginListenerTest implements SessionListener {
		public CountDownLatch login = new CountDownLatch(1);
		public ServerJoinGamePacket packet;

		@Override
		public void packetReceived(PacketReceivedEvent event) {
			Packet packet = event.getPacket();

			if (packet instanceof ServerJoinGamePacket) {
				this.packet = (ServerJoinGamePacket) packet;
				this.login.countDown();
			}
		}
	}

	private static class DisconnectListener implements SessionListener {
		@Override
		public void disconnected(DisconnectedEvent event) {
			System.err.println("Disconnected: " + event.getReason());
			if (event.getCause() != null) {
				event.getCause().printStackTrace();
			}
		}
	}
}
