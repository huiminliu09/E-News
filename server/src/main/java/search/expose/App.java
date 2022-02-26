package search.expose;

import org.archive.io.ArchiveRecord;
import org.archive.io.warc.WARCReader;
import org.archive.io.warc.WARCReaderFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class App {
    private static List<Article> list = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        // Download a WARC
        System.out.println("Start Download a WARC");
        StringBuilder saveTo = new StringBuilder("data");
        File f = new File(saveTo + ".warc");
        if (!(f.exists() && f.length() > 0)) {
            while (f.exists() && f.length() == 0) {
                System.out.println("File " + f.getName() + " is used");
                saveTo.append("(1)");
                System.out.println("try " + saveTo);
                f = new File(saveTo + ".warc");
            }
            System.out.println("warc file name: " + f.getName());

            try (S3Client s3 = S3Client.builder()
                    .region(Region.US_EAST_1)
                    .overrideConfiguration(ClientOverrideConfiguration.builder()
                            .apiCallTimeout(Duration.ofMinutes(30)).build())
                    .build()) {
                String fileName = System.getenv("COMMON_CRAWL_FILENAME");
                String bucket = "commoncrawl";

                // default situation
                if (fileName == null || "".equals(fileName)) {
                    fileName = "";
                    ListObjectsV2Request request = ListObjectsV2Request.builder()
                            .bucket(bucket)
                            .prefix("crawl-data/CC-NEWS/2021/")
                            .maxKeys(1000)
                            .build();
                    ListObjectsV2Response response = s3.listObjectsV2(request);
                    for (S3Object item : response.contents()) {
                        if (item.key().compareTo(fileName) > 0) {
                            fileName = item.key();
                        }
                    }
                }

                GetObjectRequest request = GetObjectRequest.builder()
                        .bucket(bucket)
                        .key(fileName)
                        .build();

                System.out.println("Start s3.getObject");
                s3.getObject(request, ResponseTransformer.toFile(f));
                System.out.println("End s3.getObject");
            }
        }

        // Parse the WARC file
        System.out.println("Start Parse the WARC file: " + f.getName());
        WARCReader warcReader = WARCReaderFactory.get(f.getName());
        for (ArchiveRecord archiveRecord: warcReader) {
            try {
                if (archiveRecord.getHeader().getUrl() != null) {
                    if(!"response".equals(archiveRecord.getHeader().getHeaderValue("WARC-Type"))){
                        continue;
                    }

                    byte[] b = new byte[2];
                    StringBuilder sb = new StringBuilder();
                    while (archiveRecord.read(b) != -1) {
                        sb.append(new String(b));
                    }

                    // Parse the HTML
                    String src = sb.toString();
                    String[] html = src.split("\r\n\r\n");
                    Document document = Jsoup.parse(html[1]);
                    if (document.select("html").attr("lang").toLowerCase(Locale.ROOT).equals("en")) {
                        Article article = new Article(
                                document.title(),
                                archiveRecord.getHeader().getUrl(),
                                document.text());
                        list.add(article);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Post the <url, title, txt> to Elasticsearch
        System.out.println("Start Post the <url, title, txt> to Elasticsearch");
        ElasticSearch elasticSearch = new ElasticSearch("es");
        elasticSearch.createIndex();
        AtomicInteger count = new AtomicInteger();
        list.forEach(index -> {
            try {
                elasticSearch.postDocument(index);
                count.getAndIncrement();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        elasticSearch.close();
        System.out.println("total posted " + count);
    }
}
