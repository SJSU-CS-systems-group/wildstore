package com.sjsu.wildfirestorage.spring.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class OauthController {
    @GetMapping("/")
    public String index () { return "index.html"; }
}

