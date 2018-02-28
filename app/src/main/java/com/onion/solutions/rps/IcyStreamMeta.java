package com.onion.solutions.rps;


import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IcyStreamMeta<Message> {

    protected URL streamUrl;
    private Map<String, String> metadata;
    private boolean isError;

    public IcyStreamMeta(URL streamUrl) {
        setStreamUrl(streamUrl);

        isError = false;
    }

    public static Map<String, String> parseMetadata(String metaString) {
        Map<String, String> metadata = new HashMap<String, String>();
        String[] metaParts = metaString.split(";");
        Pattern p = Pattern.compile("^([a-zA-Z]+)=\\'([^\\']*)\\'$");
        Matcher m;
        for (int i = 0; i < metaParts.length; i++) {
            m = p.matcher(metaParts[i]);
            if (m.find()) {
                metadata.put(m.group(1), m.group(2));
            }
        }

        return metadata;
    }

    /**
     * Obter título de transmissão
     *
     * @return String
     * @throws IOException
     */
    public String getStreamTitle() throws IOException {
        Map<String, String> data = getMetadata();

        if (!data.containsKey("StreamTitle"))
            return "";

        return data.get("StreamTitle").trim();
    }

    /**
     * Obter artista usando o título do fluxo
     *
     * @return String
     * @throws IOException
     */
    public String getArtist() throws IOException {
        Map<String, String> data = getMetadata();

        if (!data.containsKey("StreamTitle"))
            return "";

        String streamTitle = data.get("StreamTitle");
        String title = streamTitle.substring(0, streamTitle.indexOf("-"));
        return title.trim();
    }

    /**
     * Obter título usando o título do fluxo
     *
     * @return String
     * @throws IOException
     */
    public String getTitle() throws IOException {
        Map<String, String> data = getMetadata();

        if (!data.containsKey("StreamTitle"))
            return "";

        String streamTitle = data.get("StreamTitle");
        String artist = streamTitle.substring(streamTitle.indexOf("-") + 1);
        return artist.trim();
    }

    public Map<String, String> getMetadata() throws IOException {
        if (metadata == null) {
            refreshMeta();
        }

        return metadata;
    }

    public void refreshMeta() throws IOException {
        retreiveMetadata();
    }

    private void retreiveMetadata() throws IOException {
        URLConnection con = streamUrl.openConnection();
        con.setRequestProperty("Icy-MetaData", "1");
        con.setRequestProperty("Connection", "close");
        con.setRequestProperty("Accept", null);
        con.connect();

        int metaDataOffset = 0;
        Map<String, List<String>> headers = con.getHeaderFields();
        InputStream stream = con.getInputStream();

        if (headers.containsKey("icy-metaint")) {
            // Os cabeçalhos são enviados via HTTP
            metaDataOffset = Integer.parseInt(headers.get("icy-metaint").get(0));
        } else {
            // Os cabeçalhos são enviados dentro de um fluxo
            StringBuilder strHeaders = new StringBuilder();
            char c;
            while ((c = (char) stream.read()) != -1) {
                strHeaders.append(c);
                if (strHeaders.length() > 5 && (strHeaders.substring((strHeaders.length() - 4), strHeaders.length()).equals("\r\n\r\n"))) {
                    // end of headers
                    break;
                }
            }

            // Encaminhe cabeçalhos para obter deslocamentos de metadados dentro de um fluxo
            Pattern p = Pattern.compile("\\r\\n(icy-metaint):\\s*(.*)\\r\\n");
            Matcher m = p.matcher(strHeaders.toString());
            if (m.find()) {
                metaDataOffset = Integer.parseInt(m.group(2));
            }
        }

        // No caso de nenhum dado foi enviado
        if (metaDataOffset == 0) {
            isError = true;
            return;
        }

        // Leia metadados
        int b;
        int count = 0;
        int metaDataLength = 4080; // 4080 é o comprimento máximo
        boolean inData = false;
        StringBuilder metaData = new StringBuilder();
        // A posição de fluxo deve ser no início ou logo após os cabeçalhos
        while ((b = stream.read()) != -1) {
            count++;

            // Comprimento dos metadados
            if (count == metaDataOffset + 1) {
                metaDataLength = b * 16;
            }

            inData = count > metaDataOffset + 1 && count < (metaDataOffset + metaDataLength);
            if (inData) {
                if (b != 0) {
                    metaData.append((char) b);
                }
            }
            if (count > (metaDataOffset + metaDataLength)) {
                break;
            }

        }

        // Defina os dados
        metadata = IcyStreamMeta.parseMetadata(metaData.toString());

        // Fechar
        stream.close();
    }

    public boolean isError() {
        return isError;
    }

    public URL getStreamUrl() {
        return streamUrl;
    }

    public void setStreamUrl(URL streamUrl) {
        this.metadata = null;
        this.streamUrl = streamUrl;
        this.isError = false;
    }
}
