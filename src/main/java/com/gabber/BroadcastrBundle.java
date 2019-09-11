package com.gabber;


import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.discovery.bundle.ServiceDiscoveryBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.ws.rs.*;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.stream.Collectors;

@Slf4j
public abstract class BroadcastrBundle<T extends Configuration> implements ConfiguredBundle<T> {
    private ServiceDiscoveryBundle bundle;

    @Override
    public void run(T configuration, Environment environment) throws Exception {
        bundle = getServiceDiscovery();
        val filter = new ShouldBroadcastFilter(bundle);
        environment.jersey().register(new BroadcastFeature(filter));
    }


    public abstract ServiceDiscoveryBundle getServiceDiscovery();

    @Override
    public void initialize(Bootstrap<?> bootstrap) {

    }

    public <T> T broadcastr(Class<T> clazz) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        ProxyFactory factory = new ProxyFactory();
        factory.setSuperclass(clazz);
        factory.setFilter(
                method -> true
        );
        return (T) factory.create(new Class<?>[0], new Object[0], getProxyMethodHandler());
    }

    public MethodHandler getProxyMethodHandler() {
        return (self, thisMethod, proceed, args) -> {
                RequestModel model = ReflectionUtils.generateModel(thisMethod, args);
                if (model.isJson()) {
                    Client client = ClientBuilder.newClient();
                    val typePath = thisMethod.getDeclaringClass().getAnnotation(Path.class).value();
                    val query = HttpUtils.serializeQueryMap(model.getQueries());
                    val pathWithQuery = String.format("%s%s?%s", typePath, model.getPath(), query);
                    val ans = getServiceDiscovery().getServiceDiscoveryClient().getAllNodes()
                            .stream()
                            .map(node -> {
                                String hostPort = HttpUtils.hostPortString(node);
                                val api = String.format("%s%s%s?%s", hostPort, pathWithQuery);
                                log.info("Api {}", api);

                                try {
                                    Response response = HttpUtils.getResponse(thisMethod, model, client, api);
                                    return ShouldBroadcastFilter.Data.builder()
                                            .status(response.getStatus())
                                            .host(hostPort)
                                            .data(HttpUtils.readResponseObject(response))
                                            .build();
                                } catch (Exception e) {
                                    return ShouldBroadcastFilter.Data.builder()
                                            .status(500)
                                            .data(e.getMessage())
                                            .build();
                                }
                            }).collect(Collectors.toList());
                    return Response.ok(ans)
                            .build();
                }

                return null;
            };
    }




}