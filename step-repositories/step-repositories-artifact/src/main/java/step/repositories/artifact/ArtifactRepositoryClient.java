package step.repositories.artifact;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.cache.CacheConfig;
import org.apache.http.impl.client.cache.CachingHttpClients;
import step.resources.Resource;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.function.Function;

public class ArtifactRepositoryClient {

    private final CloseableHttpClient httpclient;

    public ArtifactRepositoryClient() {
        super();

        CacheConfig cacheConfig = CacheConfig.custom().setMaxCacheEntries(100).setMaxObjectSize(1024 * 1024).build();
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(30000).setSocketTimeout(30000).build();
        httpclient = CachingHttpClients.custom().setCacheConfig(cacheConfig).setDefaultRequestConfig(requestConfig)
                .build();
    }

    public static class HTTPRepositoryProfile {

        public HTTPRepositoryProfile(String username, String password) {
            super();
            this.username = username;
            this.password = password;
        }

        String username;
        String password;
    }

    public Resource downloadResource(HTTPRepositoryProfile profile, String url, Function<BufferedInputStream, Resource> streamConsumer) {
        assert url != null;

        BufferedInputStream bis = null;

        HttpGet httpget = new HttpGet(url);

        try (CloseableHttpResponse response = httpclient.execute(httpget, buildContext(profile))) {

            HttpEntity entity = response.getEntity();
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new RuntimeException("Invalid HTTP response code: " + response.getStatusLine().getReasonPhrase());
            } else if (entity == null) {
                throw new RuntimeException("Invalid HTTP response: nothing to download");
            }

            bis = new BufferedInputStream(entity.getContent());

            return streamConsumer.apply(bis);
        } catch (IOException e) {
            throw new RuntimeException("Exception when trying to download the resource", e);
        } finally {
            try {
                if (bis != null)
                    bis.close();
            } catch (IOException e) {
            }
        }
    }

    public String getETag(HTTPRepositoryProfile profile, String url) {
        HttpHead httphead = new HttpHead(url);
        try {
            try (CloseableHttpResponse response = httpclient.execute(httphead, buildContext(profile))) {
                Header result = response.getLastHeader(HttpHeaders.ETAG);
                return (result != null ? result.getValue() : "");
            }
        } catch (Exception e) {
            throw new RuntimeException("Exception when trying to get the etag", e);
        }
    }

    private HttpClientContext buildContext(HTTPRepositoryProfile profile) {
        HttpClientContext context = HttpClientContext.create();
        if (profile.username != null) {
            CredentialsProvider credProvider = new BasicCredentialsProvider();
            credProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(profile.username, profile.password));
            context.setCredentialsProvider(credProvider);
        }
        return context;
    }

    public static String getFileName(String url) {
        int index;
        String fileName;
        try {
            URL urlObj = new URL(url);

            fileName = urlObj.getPath();

            if ((index = fileName.lastIndexOf("/")) != -1) {
                fileName = fileName.substring(index + 1);
            }
            if ((index = fileName.lastIndexOf(".")) != -1) {
                fileName = fileName.substring(0,index);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException(url + " is not a valid URL");
        }

        return fileName;
    }

}
