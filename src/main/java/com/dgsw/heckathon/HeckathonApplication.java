package com.dgsw.heckathon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@SpringBootApplication
public class HeckathonApplication {

    public static void main(String[] args) {
        SpringApplication.run(HeckathonApplication.class, args);
    }

}
