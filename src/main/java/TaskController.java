import java.io.IOException;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
public class TaskController {

    public Document GetUrl(String url) {
        int code = 0;
        Document doc = null;
        try (CloseableHttpClient httpclient = HttpClients.createDefault()){ //Creating a HttpClient object
            final HttpGet httpget = new HttpGet(url); //Creating a HttpGet object
            try (CloseableHttpResponse response = httpclient.execute(httpget)) {
                StatusLine statusline = response.getStatusLine();
                //System.out.println(statusline.getStatusCode() + " " + statusline.getReasonPhrase());
                if (statusline.getStatusCode() == 200) {
                    System.out.println("Status " + statusline.getStatusCode() + " OK " + Thread.currentThread().getName());
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        try {
                            doc = Jsoup.connect(url).get();
                            // System.out.println(doc);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    System.out.println("error get url " + url + " code " + code);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return doc;
    }


}
