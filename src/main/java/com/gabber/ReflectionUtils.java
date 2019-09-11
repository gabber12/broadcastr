package com.gabber;

import com.google.common.collect.Sets;
import lombok.val;
import org.glassfish.jersey.uri.UriTemplate;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.lang.reflect.Method;
import java.util.*;

public class ReflectionUtils {
    public static RequestModel generateModel(Method thisMethod, Object[] args) {
        val path = thisMethod.getAnnotation(Path.class);
        UriTemplate tmpl = new UriTemplate(path.value());
        RequestModel model = new RequestModel();
        List<String> paths = new ArrayList<>();
        Map<String, String> queries = new HashMap<>();
        List<Object> noAnnotation = new ArrayList<>();
        for (int i = 0; i < thisMethod.getParameterAnnotations().length; i++) {
            if (Arrays.asList(thisMethod.getParameterAnnotations()[i]).stream()
                    .filter(ann -> ann.annotationType().equals(PathParam.class)).findAny().isPresent()) {
                paths.add((String) args[i]);
            }

            if (Arrays.asList(thisMethod.getParameterAnnotations()[i]).stream()
                    .filter(ann -> ann.annotationType().equals(QueryParam.class)).findAny().isPresent()) {
                val key = ((QueryParam) Arrays.asList(thisMethod.getParameterAnnotations()[i]).stream()
                        .filter(ann -> ann.annotationType().equals(QueryParam.class)).findAny().get()).value();
                queries.put(key, (String) args[i]);
            }
            if (thisMethod.getParameterAnnotations().length == 0) {
                noAnnotation.add(args[i]);
            }
        }

        model.setPaths(paths);
        model.setQueries(queries);
        model.setNoAnnotation(noAnnotation);
        model.setPath(tmpl.createURI(paths.toArray(new String[]{})));
        val consumes = thisMethod.getAnnotation(Consumes.class);
        if (consumes != null) {
            model.setConsumes(Sets.newHashSet(consumes.value()));
        };
        return model;
    }

    public static String getMethod(Method thisMethod) {
        if (thisMethod.getAnnotation(POST.class) != null) {
            return "POST";
        } else if (thisMethod.getAnnotation(PUT.class) != null) {
            return "PUT";
        } else if (thisMethod.getAnnotation(DELETE.class) != null) {
            return "DELETE";
        } else if (thisMethod.getAnnotation(OPTIONS.class) != null) {
            return "OPTIONS";
        } else if (thisMethod.getAnnotation(HEAD.class) != null) {
            return "HEAD";
        }
        return "GET";
    }
}
