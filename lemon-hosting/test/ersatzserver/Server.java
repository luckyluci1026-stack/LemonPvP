import java.io.*;

/**
 * Ein Minecraft-Server, der keiner ist.
 *
 * Die Testreihen muessen einen Server wirklich starten, stoppen und mit
 * ihm reden - sonst prueft man nur, ob Knoepfe da sind. Eine echte
 * Paper-Jar geht dafuer auch, hat aber drei Nachteile: Sie ist 55 MB
 * gross, braucht eine halbe Minute zum Hochfahren und gehoert nicht in
 * ein Git-Verzeichnis.
 *
 * Dieses Programm hier tut so, als waere es einer. Es gibt dieselben
 * Zeilen aus, auf die das Panel achtet - vor allem "Done (...)", woran
 * es erkennt, dass der Server oben ist - und hoert auf `stop` genauso
 * auf, wie ein echter es tut. Es startet in einer Sekunde und ist eine
 * Textdatei.
 *
 * Gebaut wird es von test/alles.sh; von Hand geht es so:
 *
 *   javac -d bau Server.java
 *   jar --create --file server.jar --main-class Server -C bau .
 */
public class Server {
  public static void main(String[] argumente) throws Exception {
    String port = "25565";
    for (int i = 0; i < argumente.length - 1; i++) {
      if (argumente[i].equals("--port")) port = argumente[i + 1];
    }

    System.out.println("[00:00:01 INFO]: Starting minecraft server version 1.21.11");
    System.out.println("[00:00:01 INFO]: Starting Minecraft server on *:" + port);
    System.out.println("[00:00:02 WARN]: Ambiguity between arguments detected");
    Thread.sleep(900);
    System.out.println("[00:00:03 INFO]: Done (2.481s)! For help, type \"help\"");

    BufferedReader lesen = new BufferedReader(new InputStreamReader(System.in));
    String zeile;
    while ((zeile = lesen.readLine()) != null) {
      String z = zeile.trim();

      if (z.equals("stop")) {
        System.out.println("[00:00:09 INFO]: Stopping the server");
        System.out.println("[00:00:09 INFO]: Saving worlds");
        Thread.sleep(300);
        System.out.println("[00:00:10 INFO]: All dimensions are saved");
        return;
      }

      // Zwei Beitritte in EINER Ausgabe. Genau daran ist die
      // Spielerliste einmal gescheitert: Sie benutzte match() statt
      // matchAll() und verlor deshalb den zweiten.
      if (z.equals("zweikommen")) {
        System.out.println("[00:00:04 INFO]: Lea joined the game");
        System.out.println("[00:00:04 INFO]: Tom joined the game");
      } else if (z.startsWith("weg ")) {
        System.out.println("[00:00:06 INFO]: " + z.substring(4) + " left the game");
      } else {
        System.out.println("[00:00:05 INFO]: [Server] " + z);
      }
    }
  }
}
