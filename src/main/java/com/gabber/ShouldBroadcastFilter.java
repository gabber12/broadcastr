package com.gabber;

import io.dropwizard.discovery.bundle.ServiceDiscoveryBundle;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.io.IOUtils;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Slf4j
@BroadcastIt
public class ShouldBroadcastFilter implements ContainerResponseFilter {

    private ServiceDiscoveryBundle bundle;

    public ShouldBroadcastFilter(ServiceDiscoveryBundle bundle) {

        this.bundle = bundle;
    }

    @Override
    public void filter(ContainerRequestContext containerRequestContext, ContainerResponseContext containerResponseContext) throws IOException {

        if (containerRequestContext.getHeaderString("X-BROADCASTED") != null) {
            return;
        }

        Client client = ClientBuilder.newClient();
        val in = containerRequestContext.getEntityStream();
        val headers = new MultivaluedHashMap<String, Object>();
        containerRequestContext.getHeaders().entrySet()
                .forEach(entry -> headers.put(entry.getKey(), entry.getValue().stream().map(x -> (Object) x).collect(Collectors.toList())));
        val url = containerRequestContext.getUriInfo();
        val path = url.getRequestUri().toString().replaceAll(url.getBaseUri().toString(), "/");
        val method = containerRequestContext.getMethod();
        val selfHost = System.getenv("HOST");
        val ans = bundle.getServiceDiscoveryClient().getAllNodes()
                .stream()
                .map(node -> {
                    String hostPort = HttpUtils.hostPortString(node);
                    if (node.getHost().equals(selfHost)) {
                        return Data.builder()
                                .status(containerResponseContext.getStatus())
                                .host(hostPort)
                                .data(containerResponseContext.getEntity())
                                .build();
                    }
                    String urlPath = String.format("%s%s", hostPort, path);
                    log.info("URLS being called {}", urlPath);
                    try {
                        headers.putSingle("Host", node.getHost());
                        headers.putSingle("Accept-Encoding", "UTF-8");
                        headers.putSingle("X-BROADCASTED", "true");
                        Response res = HttpUtils.sendRequest(client, urlPath, method, headers, containerRequestContext.hasEntity(), containerRequestContext.getMediaType(), IOUtils.toString(in, StandardCharsets.UTF_8));

                        Object retData = HttpUtils.readResponseObject(res);
                        return Data.builder()
                                .data(retData)
                                .host(hostPort)
                                .status(res.getStatus())
                                .build();
                    } catch (Exception e) {
                        log.error("Error Calling Url {}", urlPath, e);
                        return Data.builder()
                                .status(400)
                                .host(hostPort)
                                .data(e.getMessage())
                                .build();
                    }

                }).collect(Collectors.toList());

        log.info("Responses {}", ans);
        containerResponseContext.setEntity(ans, new Annotation[]{}, MediaType.APPLICATION_JSON_TYPE);
    }




    @lombok.Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class Data {
        private int status;
        private String host;
        private Object data;
    }
}
