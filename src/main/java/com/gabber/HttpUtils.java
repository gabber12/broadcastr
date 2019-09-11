package com.gabber;

import com.flipkart.ranger.model.ServiceNode;
import io.dropwizard.discovery.common.ShardInfo;
import lombok.val;
import org.apache.commons.io.IOUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

public class HttpUtils {
    public static String hostPortString(ServiceNode<ShardInfo> node) {
        return String.format("http://%s:%s", node.getHost(), node.getPort());
    }

    public static Object readResponseObject(Response res) throws IOException {
        return res.hasEntity()
                ? (
                res.getMediaType() != null && res.getMediaType().equals(MediaType.APPLICATION_JSON_TYPE)
                        ? res.readEntity(Object.class)
                        : IOUtils.toString((InputStream) res.getEntity(), StandardCharsets.UTF_8))
                : null;
    }

    public static String serializeQueryMap(Map<String, String> queries) {
        return URLEncodedUtils.format(queries.entrySet().stream().map(e -> new NameValuePair() {
            @Override
            public String getName() {
                return e.getKey();
            }

            @Override
            public String getValue() {
                return e.getValue();
            }
        }).collect(Collectors.toList()), StandardCharsets.UTF_8);
    }

    public static Response getResponse(Method thisMethod, RequestModel model, Client client, String api) throws IOException {
        val hasEntity = model.getNoAnnotation().size() != 0;
        val headers = new MultivaluedHashMap<String, Object>();
        headers.putSingle("Content-Type", "application/json");
        return sendRequest(client, api, ReflectionUtils.getMethod(thisMethod), headers, hasEntity, MediaType.APPLICATION_JSON_TYPE, model.getNoAnnotation().get(0));
    }


    public static Response sendRequest(Client client, String target, String method, MultivaluedHashMap<String, Object> headers, boolean hasEntity, MediaType mediaType, Object entity) throws IOException {
        Invocation.Builder builder = client.target(target)
                .request()
                .headers(headers);
        return hasEntity
                ? builder.method(method, Entity.entity(entity, mediaType))
                : builder.method(method);
    }

}
