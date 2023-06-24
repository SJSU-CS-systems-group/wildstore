package com.sjsu.wildfirestorage.spring.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class OauthController {
    @GetMapping("/")
    public ResponseEntity<String> index() {
        return new ResponseEntity<>("hello", HttpStatus.OK);
    }

}
