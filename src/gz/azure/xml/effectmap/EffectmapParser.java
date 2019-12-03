package gz.azure.xml.effectmap;

import gz.azure.utils.Log;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by Scott on 8/18/2015.
 */
public class EffectmapParser {
    public static Effectmap getEffectmap() {
        try {
            return getEffectmap(new URL("https://habboo-a.akamaihd.net/gordon/PRODUCTION-201508111205-320867585/effectmap.xml"));
        } catch (MalformedURLException e) {
            // This is a static URL. It should never be "malformed"
            return null;
        }
    }

    public static Effectmap getEffectmap(URL sourceURL) {
        try {
            System.out.println(sourceURL);
            JAXBContext jc = JAXBContext.newInstance(Effectmap.class);
            Unmarshaller unmarshaller = jc.createUnmarshaller();
            HttpURLConnection conn = (HttpURLConnection) sourceURL.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            StreamSource src = new StreamSource(conn.getInputStream());
            return ((Effectmap) unmarshaller.unmarshal(src));
        } catch (JAXBException jaxbEx) {
            Log.error("Failed to create new JAXB instance", jaxbEx);
            return null;
        } catch (IOException ioEx) {
            Log.error("Failed to download furnidata XML for reading", ioEx);
            return null;
        }
    }
}
