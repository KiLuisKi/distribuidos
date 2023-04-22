package prueba;

import java.io.IOException;
import java.io.BufferedWriter;
import java.io.FileWriter;

public class Logger {

    private String fileName;

    public Logger(String fileName) {
        this.fileName = fileName;
    }

    public synchronized void write(String entry) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true))) {
            writer.write(entry);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
