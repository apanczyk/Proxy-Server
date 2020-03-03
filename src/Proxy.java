import java.io.*;
import java.net.*;
import java.util.StringTokenizer;


class Proxy extends Thread {
    private Socket sClient;

    public static void main(String[] args) {
        try {
            System.out.println("Proxy server working...");
            ServerSocket server = new ServerSocket(8080);
            while (true) {
                new Proxy(server.accept());
            }
        } catch (Exception e) {
            System.err.println(e);
        }
    }

    Proxy(Socket sClient) {
        this.sClient = sClient;
        this.start();
    }

    @Override
    public void run() {
        try {
            final InputStream inFromClient = sClient.getInputStream();
            final DataOutputStream outToClient = new DataOutputStream(sClient.getOutputStream());
            final BufferedWriter outToClientBuffered = new BufferedWriter(new OutputStreamWriter(sClient.getOutputStream()));


            byte[] content = new byte[4096];
            int lengthIn = inFromClient.read(content);
            String tmp = new String(content, 0, lengthIn);
            System.out.println(tmp);

            int port = getServerPort(tmp);
            String hostName = getSiteName(tmp, port);


            Socket server = new Socket(hostName, port);
            log(hostName + ":" + port);

            final InputStream inFromServer = server.getInputStream();
            final DataOutputStream outToServer = new DataOutputStream(server.getOutputStream());

            if(port==443) {
                outToClientBuffered.write("HTTP/1.0 200 Connection established\r\n\r\n");
                outToClientBuffered.flush();
            }
            else {
                outToServer.write(content, 0, lengthIn);
                outToServer.flush();
            }
            new Thread(() -> {
                try {
                    int lengthTmp;

                    while ((lengthTmp = inFromClient.read(content)) != -1) {
                        outToServer.write(content, 0, lengthTmp);
                        outToServer.flush();
                    }
                    outToServer.close();
                } catch (IOException e) { }
            }).start();

            int lengthOut;
            while ((lengthOut = inFromServer.read(content)) != -1) {
                outToClient.write(content, 0, lengthOut);
                outToClient.flush();
            }

            server.close();
            outToClient.close();
            sClient.close();
        }catch (Exception e) {}
    }

    public static String getSiteName(String site, int port) {
        StringTokenizer st = new StringTokenizer(site);
        while(!st.nextToken().equals("Host:"));

        System.out.println(site);
        String siteName;
        if(port == 443) {
            String[] siteNameTmp = st.nextToken().split(":");
            siteName = siteNameTmp[0];
        }
        else {
            siteName = st.nextToken();
        }
        return siteName;
    }

    public static int getServerPort(String site) {
        String protocol = site.substring(site.indexOf(" ")+1, site.indexOf(":"));
        if(!protocol.equals("http"))
            return 443;
        return 80;
    }

    public static void log(String s) {
        System.out.println("================== " + s + " ==================");
    }
}