package com.sjsu.wildfirestorage;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

public class Client {
    public static Object get(String path, MultiValueMap<String, String> queryParams, ParameterizedTypeReference parameterizedTypeReference) {
        WebClient webClient = WebClient.create(path);

        Object response = webClient.get()
                .uri(uriBuilder -> uriBuilder.queryParams(queryParams).build())
                .retrieve()
                .bodyToMono(parameterizedTypeReference).block();

        System.out.println("GET Response received: \n" + response);
        return response;
    }

    public static Object post(String path, Object body){
        WebClient webClient = WebClient.create(path);

        Object response = webClient.post()
                .body(Mono.just(body), Object.class)
                .retrieve()
                .bodyToMono(Object.class)
                .block();
        System.out.println("POST Response received: \n" + response);
        return response;
    }
}
