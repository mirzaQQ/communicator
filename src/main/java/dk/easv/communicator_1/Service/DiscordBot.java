package dk.easv.communicator_1.Service;

import dk.easv.communicator_1.gui.HelloController;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.io.FileReader;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class DiscordBot extends ListenerAdapter {

    protected static JDA jda;
    protected static TextChannel channel;
    private static final List<HelloController> controllers = new CopyOnWriteArrayList<>();

    public static void start(HelloController application) throws Exception {

        Properties properties = new Properties();

        try (FileReader reader = new FileReader("secrets")) {
            properties.load(reader);
        }

        String token = properties.getProperty("TOKEN");
        controllers.add(application);

        if (jda == null) {
            jda = JDABuilder.createDefault(token)
                    .addEventListeners(new MessageService())
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MESSAGES)
                    .build();

            jda.awaitReady();
            channel = jda.getTextChannelById("1527656008531443755");
        }
        System.out.println("[DB] resolved path: " + new java.io.File("../../DataGripProjects/default/identifier.sqlite").getCanonicalPath());
    }


    public static void unregister(HelloController application) {
        controllers.remove(application);
    }

    public static void dispatch(String conversationUuid, String ciphertext) {
        for (HelloController controller : controllers) {
            controller.onDiscordMessage(conversationUuid, ciphertext);
        }
    }

    protected static boolean isRelayChannel(MessageChannel eventChannel) {
        return channel != null && eventChannel != null && eventChannel.getIdLong() == channel.getIdLong();
    }

    protected static TextChannel getChannel() {
        return channel;
    }
}
