package tool;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

public class ListenThread implements Runnable {
    private Socket socket;
    private InputStream inputStream;

    public ListenThread(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() throws RuntimeException{
        try {
            this.inputStream = socket.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }

        while (true) {
            try {
                int first = this.inputStream.read();
                if (first == -1) {
                    // 输入流已经被关闭，无需继续读取
                    throw new RuntimeException("disconnected.");
                }
                int second = this.inputStream.read();
                int msgLength = (first<<8) + second;
                byte[] readBuffer = new byte[msgLength];
                this.inputStream.read(readBuffer);

                System.out.println("message from [" + socket.getInetAddress() + "]: " + new String(readBuffer,"GBK"));
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e.getMessage());
            }
        }
    }
}

