import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequestBuilder;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Requests;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.jsoup.nodes.Document;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;


public class ElasticBD {
    PreBuiltTransportClient client = null;
    public void connect() throws UnknownHostException {
        Settings settings = Settings.builder()
                .put("cluster.name","docker-cluster")
                .build();
        PreBuiltTransportClient cli = new PreBuiltTransportClient(settings);
        cli.addTransportAddress(new TransportAddress(InetAddress.getByName("127.0.0.1"), 9300));
        this.client = cli;
    }

    void SendJson() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setUsername("rabbitmq");
        factory.setPassword("rabbitmq");
        factory.setPort(5672);
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare("JSON_DATA", false, false, true, null);
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String json = new String(delivery.getBody(), "UTF-8");
            try {

                IndexRequest request = new IndexRequest("posts");
                String sha256hex = org.apache.commons.codec.digest.DigestUtils.sha256Hex(json);
                request.id(sha256hex);
                String jsonString = json;
                request.source(jsonString, XContentType.JSON);
                client.index(request);
                XContentBuilder mapping = jsonBuilder()
                        .startObject()
                        .startObject("properties")
                        .startObject("author")
                        .field("type", "keyword")
                        .endObject()
                        .endObject()
                        .endObject();
                AcknowledgedResponse resp = client.admin().indices()
                        .preparePutMapping("posts")
                        .setSource(mapping)
                        .setType("_doc")
                        .execute().actionGet();
                System.out.println("\nAppended \n");
            }
            catch(Exception e)
            {

            }finally {
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            }
        };
        channel.basicConsume("JSON_DATA", false, deliverCallback, consumerTag -> { });
    }



    static void Aggregation() throws IOException, ExecutionException, InterruptedException {
        Client client = new PreBuiltTransportClient(
                Settings.builder().put("cluster.name","docker-cluster").build())
                .addTransportAddress(new TransportAddress(InetAddress.getByName("localhost"), 9300));
        SearchSourceBuilder searchSourceBuilder2 = new SearchSourceBuilder().aggregation(AggregationBuilders.terms("AUTHOR_count").field("author"));
        SearchRequest searchRequest2 = new SearchRequest().indices("posts").source(searchSourceBuilder2);
        SearchResponse searchResponse = client.search(searchRequest2).get();
        Terms terms = searchResponse.getAggregations().get("AUTHOR_count");

        File file = new File("infoAggr.txt");
        FileWriter writer = new FileWriter(file);
        for (Terms.Bucket bucket : terms.getBuckets())
            writer.write("Count: " + bucket.getDocCount() + "\tAuthor: " + bucket.getKey()  + "\n"); // write in file


        client.close();
        writer.close();
    }


}
