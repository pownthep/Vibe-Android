package com.example.vibe_android.http;

import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.google.android.gms.common.util.IOUtils;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;

import static com.example.vibe_android.MainActivity.APP_DATA;
import static com.example.vibe_android.MainActivity.accessToken;

import rawhttp.core.*;
import rawhttp.core.body.StringBody;
import rawhttp.core.server.*;

public class HttpServer extends Thread {
    private final String CRLF = "\n\r";
    private ServerSocket serverSocket;
    private int chunkSize = 20000000;

    @Override
    public void run() {
        try {
            startServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class HttpThread extends Thread {
        private Socket socket;
        private int id;

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
                Long size = Long.parseLong(request.getUri().getQuery().split("&")[1].replace("size=", ""));
                InputStream req = socket.getInputStream();
                OutputStream res = socket.getOutputStream();
                try {
                    //Log.d("VIBE","Sending headers");
                    SimpleDateFormat gmtFrmt = new SimpleDateFormat("E, d MMM yyyy HH:mm:ss 'GMT'", Locale.US);
                    gmtFrmt.setTimeZone(TimeZone.getTimeZone("GMT"));
                    PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(res)), false);
                    String start = "0";
                    String end = "";
                    if (request.getHeaders().get("Range").size() > 0) {
                        String range = request.getHeaders().get("Range").get(0);
                        String[] parts = range.replace("bytes=", "").split("-");

                        pw.append("HTTP/1.1 ").append("206").append(" \r\n");
                        start = parts[0];
                        end = parts.length > 1 ? parts[1] : (size - 1) + "";
                        String ContentRange = "bytes " + start + "-" + end + "/" + size;
                        printHeader(pw, "Content-Range", ContentRange);
                        //Log.d("VIBE","Content-Range: " + ContentRange);

                        printHeader(pw, "Accept-Ranges", "bytes");

                        String chunkSize = (Integer.parseInt(end) - Integer.parseInt(start) + 1) + "";
                        //Log.d("VIBE","Content-Length: " + chunkSize);
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
                        Log.d("VIBE","Sending partial data:" + request.getHeaders().get("Range").get(0));
                        httpStreamFile(fileId, Long.parseLong(start), Long.parseLong(end), res);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    Log.d("VIBE","Closing connection #"+id);
                    req.close();
                    res.close();
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void startServer() throws IOException {
        serverSocket = new ServerSocket(8080);
        int count = 0;
        while (serverSocket.isBound() && !serverSocket.isClosed()) {
            Socket socket = serverSocket.accept();
            count++;
            Log.d("VIBE", "------------------------------------------------------------------------------------------------------------------------------");
            Log.d("VIBE","Incoming request: " + count);
            HttpThread thread = new HttpThread(socket,count);
            thread.start();
        }
    }

    public void stopServer() throws IOException {
        serverSocket.close();
    }

    private void downloadFile(String fileId, long start, long end, OutputStream res) throws IOException {
        long startChunk = (long) Math.floor(start / chunkSize);
        String chunkName = APP_DATA + File.separator + fileId + "@" + startChunk;
        Log.d("VIBE","req: " + start + " / " + end + "   offline");
        long relativeStart =
                start > startChunk * chunkSize ? start - startChunk * chunkSize : 0;
        long relativeEnd =
                end > (startChunk + 1) * chunkSize
                        ? chunkSize
                        : end - startChunk * chunkSize;
        File file = new File(chunkName);
        if (!file.exists()) {

        } else {
            FileInputStream stream = new FileInputStream(file);
            //init array with file length
            byte[] bytesArray = new byte[(int) file.length()];
            FileInputStream fis = new FileInputStream(file);
            fis.read(bytesArray); //read file into bytes[]
            fis.close();
            res.write(bytesArray);
            if (end >= (startChunk + 1) * chunkSize) {
                downloadFile(fileId, (startChunk + 1) * chunkSize, end, res);
            } else {
                res.flush();
            }
        }
    }

    private void httpStreamFile(String fileId, long start, long end, OutputStream res) {
        String fileURL = "https://www.googleapis.com/drive/v3/files/" + fileId + "?alt=media";
        URL url = null;
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
                int bytesRead = -1;
                byte[] buffer = new byte[4096];
                long byteTransferred = 0;
                boolean error = false;
                while ((bytesRead = inputStream.read(buffer)) != -1 && !error) {
                    try {
                        res.write(buffer, 0, bytesRead);
                        byteTransferred += bytesRead;
                        //Log.d("VIBE","Transfer: " + byteTransferred / 1000000);
                    } catch (SocketException e) {
                        Log.d("VIBE ERROR", "Socket closed!");
                        error = true;
                    }
                }
                inputStream.close();
            } else {
                Log.d("VIBE","No file to download. Server replied HTTP code: " + responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            urlConnection.disconnect();
        }
    }

    private byte[] getByte(String fileId, long start, long end) throws IOException {
        long startChunk = (long) Math.floor(start / chunkSize);
        String chunkName = APP_DATA + File.separator + fileId + "@" + startChunk;
        Log.d("VIBE","req: " + start + " / " + end + "   offline");
        long relativeStart =
                start > startChunk * chunkSize ? start - startChunk * chunkSize : 0;
        long relativeEnd =
                end > (startChunk + 1) * chunkSize
                        ? chunkSize
                        : end - startChunk * chunkSize;
        File file = new File(chunkName);
        if (!file.exists()) {
            String fileURL = "https://www.googleapis.com/drive/v3/files/" + fileId + "?alt=media";
            URL url = null;
            HttpURLConnection urlConnection = null;
            try {
                url = new URL(fileURL);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestProperty("Authorization", "Bearer " + accessToken);
                urlConnection.setRequestProperty("Range", "bytes=" + start + "-" + end);
                Log.d("token", accessToken);
                int responseCode = urlConnection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_PARTIAL) {
                    String fileName = "";
                    String disposition = urlConnection.getHeaderField("Content-Disposition");
                    String contentType = urlConnection.getContentType();
                    int contentLength = urlConnection.getContentLength();

                    if (disposition != null) {
                        // extracts file name from header field
                        int index = disposition.indexOf("filename=");
                        if (index > 0) {
                            fileName = disposition.substring(index + 10,
                                    disposition.length() - 1);
                        }
                    } else {
                        // extracts file name from URL
                        fileName = fileURL.substring(fileURL.lastIndexOf("/") + 1,
                                fileURL.length());
                    }

                    Log.d("VIBE","Content-Type = " + contentType);
                    Log.d("VIBE","Content-Disposition = " + disposition);
                    Log.d("VIBE","Content-Length = " + contentLength);
                    Log.d("VIBE","fileName = " + fileName);

                    // opens input stream from the HTTP connection
                    InputStream inputStream = urlConnection.getInputStream();

                    // opens an output stream to save into file
                    FileOutputStream outputStream = new FileOutputStream(file);

                    int bytesRead = -1;
                    byte[] buffer = new byte[4096];
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    outputStream.close();
                    inputStream.close();
                    Log.d("VIBE","File downloaded");

                } else {
                    Log.d("VIBE","No file to download. Server replied HTTP code: " + responseCode);
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                urlConnection.disconnect();
            }

        }
        FileInputStream stream = new FileInputStream(file);
        //init array with file length
        byte[] bytesArray = new byte[(int) file.length()];
        FileInputStream fis = new FileInputStream(file);
        fis.read(bytesArray); //read file into bytes[]
        fis.close();
        return bytesArray;
    }

    protected void printHeader(PrintWriter pw, String key, String value) {
        pw.append(key).append(": ").append(value).append("\r\n");
    }
}
