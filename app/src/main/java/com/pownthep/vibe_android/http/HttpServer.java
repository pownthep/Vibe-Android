package com.pownthep.vibe_android.http;

import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

import rawhttp.core.RawHttp;
import rawhttp.core.RawHttpRequest;

import static com.pownthep.vibe_android.MainActivity.APP_DATA;
import static com.pownthep.vibe_android.MainActivity.accessToken;
import static com.pownthep.vibe_android.MainActivity.isCacheEnabled;

public class HttpServer extends Thread {
    private ServerSocket serverSocket;

    @Override
    public void run() {
        try {
            startServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class HttpThread extends Thread {
        private final Socket socket;
        private final int id;

        public HttpThread(Socket socket, int id) {
            this.socket = socket;
            this.id = id;
        }

        @Override
        public void run() {
            try {
                RawHttp http = new RawHttp();
                RawHttpRequest request = http.parseRequest(socket.getInputStream());
                String fileId = request.getUri().getQuery().split("&")[0].replace("id=", "");
                long size = Long.parseLong(request.getUri().getQuery().split("&")[1].replace("size=", ""));
                try (InputStream req = socket.getInputStream(); OutputStream res = socket.getOutputStream()) {
                    SimpleDateFormat gmtFrmt = new SimpleDateFormat("E, d MMM yyyy HH:mm:ss 'GMT'", Locale.US);
                    gmtFrmt.setTimeZone(TimeZone.getTimeZone("GMT"));
                    PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(res)), false);
                    String start = "0";
                    String end = "";
                    if (request.getHeaders().get("Range").size() > 0) {
                        pw.append("HTTP/1.1 ").append("206").append(" \r\n");
                        String range = request.getHeaders().get("Range").get(0);
                        String[] parts = range.replace("bytes=", "").split("-");
                        start = parts[0];
                        end = parts.length > 1 ? parts[1] : (size - 1) + "";
                        String ContentRange = "bytes " + start + "-" + end + "/" + size;
                        printHeader(pw, "Content-Range", ContentRange);
                        printHeader(pw, "Accept-Ranges", "bytes");
                        String chunkSize = (Long.parseLong(end) - Long.parseLong(start) + 1) + "";
                        printHeader(pw, "Content-Length", chunkSize);
                    } else {
                        pw.append("HTTP/1.1 ").append("200").append(" \r\n");
                        printHeader(pw, "Content-Length", size + "");
                    }
                    printHeader(pw, "Content-Type", "video/x-matroska");
                    printHeader(pw, "Date", gmtFrmt.format(new Date()));
                    printHeader(pw, "Connection", "keep-alive");
                    printHeader(pw, "Server", "HTTP");
                    pw.append("\r\n");
                    pw.flush();
                    if (request.getHeaders().get("Range").size() > 0) {
                        Log.d("VIBE", "Sending partial data:" + request.getHeaders().get("Range").get(0));
                        ArrayList<String> fileList = isCacheEnabled ? listFilesForFolder(fileId, Long.parseLong(start)) : null;
                        if (fileList != null && fileList.size() > 0) {
                            long reqByteStart = Long.parseLong(start);
                            long parseEnd = Long.parseLong(end);
                            for (String tmp : fileList) {
                                File tmpFile = new File(APP_DATA + File.separator + tmp);
                                long byteStart = Long.parseLong(tmp.split("@")[1]);
                                long byteEnd = byteStart + (tmpFile.length() - 1);

                                if (byteStart <= reqByteStart && byteEnd >= reqByteStart) {
                                    Log.d("VIBE", "Offline: " + byteStart + "->" + byteEnd);
                                    RandomAccessFile randFile = new RandomAccessFile(APP_DATA + File.separator + tmp, "r");
                                    randFile.seek(reqByteStart - byteStart);
                                    int bytesRead;
                                    byte[] buffer = new byte[4096];
                                    boolean error = false;
                                    while ((bytesRead = randFile.read(buffer)) != -1 && !error) {
                                        try {
                                            res.write(buffer, 0, bytesRead);
                                        } catch (SocketException e) {
                                            error = true;
                                        }
                                    }
                                    reqByteStart = byteEnd + 1;
                                } else {
                                    long newEndByte = byteStart - 1 > reqByteStart ? byteStart - 1 : parseEnd;
                                    Log.d("VIBE", "Online: " + reqByteStart + "->" + newEndByte);
                                    httpStreamFile(fileId, reqByteStart, newEndByte, res);
                                }
                            }
                        } else {
                            Log.d("VIBE", "Online: " + start + "->" + end);
                            httpStreamFile(fileId, Long.parseLong(start), Long.parseLong(end), res);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    Log.d("VIBE", "Closing connection #" + id);
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void startServer() throws IOException {
        serverSocket = new ServerSocket(8080);
        Log.d("VIBE", String.valueOf(serverSocket.getInetAddress()));
        Log.d("VIBE", String.valueOf(isCacheEnabled));
        int count = 0;
        while (serverSocket.isBound() && !serverSocket.isClosed()) {
            try {
                Socket socket = serverSocket.accept();
                count++;
                Log.d("VIBE", "------------------------------------------------------------------------------------------------------------------------------");
                Log.d("VIBE", "Incoming request: " + count);
                HttpThread thread = new HttpThread(socket, count);
                thread.start();
            } catch (SocketException e) {
                Log.d("HTTP", "SHUTTING DOWN");
            }
        }
    }

    public ArrayList<String> listFilesForFolder(String fileId, Long start) {
        ArrayList<String> files = new ArrayList<>();
        File folder = new File(APP_DATA);
        for (final File fileEntry : Objects.requireNonNull(folder.listFiles())) {
            long byteStart = Long.parseLong(fileEntry.getName().split("@")[1]);
            long byteEnd = byteStart + (fileEntry.length() - 1);
            if (fileEntry.isFile() && fileEntry.getName().contains(fileId) && byteEnd > start && byteStart <= start) {
                files.add(fileEntry.getName());
            }
        }
        Collections.sort(files);
        return files;
    }

    public void stopServer() throws IOException {
        serverSocket.close();
    }


    private void httpStreamFile(String fileId, long start, long end, OutputStream res) {
        String fileURL = "https://www.googleapis.com/drive/v3/files/" + fileId + "?alt=media";
        URL url;
        HttpURLConnection urlConnection = null;
        try {
            url = new URL(fileURL);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("Authorization", "Bearer " + accessToken);
            urlConnection.setRequestProperty("Range", "bytes=" + start + "-" + end);

            int responseCode = urlConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_PARTIAL) {
                // opens input stream from the HTTP connection
                InputStream inputStream = urlConnection.getInputStream();
                int bytesRead;
                byte[] buffer = new byte[4096];
                boolean error = false;
                if (isCacheEnabled) {
                    File newFile = new File(APP_DATA + File.separator + fileId + "@" + start);
                    FileOutputStream fileOutputStream = new FileOutputStream(newFile);
                    while ((bytesRead = inputStream.read(buffer)) != -1 && !error) {
                        try {
                            res.write(buffer, 0, bytesRead);
                            fileOutputStream.write(buffer, 0, bytesRead);
                        } catch (SocketException e) {
                            Log.d("VIBE ERROR", "Socket closed!");
                            error = true;
                        }
                    }
                    fileOutputStream.close();
                } else {
                    while ((bytesRead = inputStream.read(buffer)) != -1 && !error) {
                        try {
                            res.write(buffer, 0, bytesRead);
                        } catch (SocketException e) {
                            Log.d("VIBE ERROR", "Socket closed!");
                            error = true;
                        }
                    }
                }
                inputStream.close();
            } else {
                Log.d("VIBE", "No file to download. Server replied HTTP code: " + responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            assert urlConnection != null;
            urlConnection.disconnect();
        }
    }

    protected void printHeader(PrintWriter pw, String key, String value) {
        pw.append(key).append(": ").append(value).append("\r\n");
    }
}
