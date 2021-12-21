package jukebox;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetSocket;
import io.vertx.core.parsetools.RecordParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetControl extends AbstractVerticle {
  private Logger LOGGER = LoggerFactory.getLogger(NetControl.class);

  @Override
  public void start() throws Exception {
    vertx.createNetServer()
      .connectHandler(this::handleClient)
      .listen(3000);
  }

  private void handleClient(NetSocket socket) {
    RecordParser.newDelimited("\n", socket)
      .handler(buffer -> handleBuffer(socket, buffer))
      .endHandler(v -> LOGGER.info("Connection ended"));
  }

  private void handleBuffer(NetSocket socket, Buffer buffer) {
    String command = buffer.toString();

    switch (command) {
      case "/list":
        vertx.eventBus().request("jukebox.list", "", reply -> {
          if (reply.succeeded()) {
            JsonObject data = (JsonObject) reply.result().body();
            data.getJsonArray("files")
                    .stream().forEach(name -> socket.write(name + "\n"));
          } else {
            LOGGER.error("/list error", reply.cause());
          }
        });
        break;

      case "/play":
        LOGGER.info("Playing");
        vertx.eventBus().send("jukebox.play", "");
        break;
      case "/pause":
        LOGGER.info("Pause");
        vertx.eventBus().send("jukebox.pause", "");
        break;
      default:
        if (command.startsWith("/schedule ")) {
          String track = command.substring(10);
          JsonObject json = new JsonObject().put("file", track);
          vertx.eventBus().send("jukebox.schedule", json);
        } else {
          LOGGER.error("Unknown command");
          socket.write("Unknown command\n");
        }
    }
  }
}
