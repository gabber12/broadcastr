package com.gabber;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.ws.rs.core.MediaType;
import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestModel {
    List<String> paths = new ArrayList<>();
    Map<String, String> queries = new HashMap<>();
    List<Object> noAnnotation = new ArrayList<>();
    private String path;
    private Set<String> consumes;

    public boolean isJson() {
        return consumes != null && consumes.contains(MediaType.APPLICATION_JSON);
    }
}
