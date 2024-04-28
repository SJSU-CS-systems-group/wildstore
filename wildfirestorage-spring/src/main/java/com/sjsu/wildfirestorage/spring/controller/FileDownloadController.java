package com.sjsu.wildfirestorage.spring.controller;

import com.sjsu.wildfirestorage.Metadata;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@RestController
@RequestMapping("/api")
public class FileDownloadController {

    @Value("${custom.fileServer}")
    private String fileServerUrl;

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/file/{digestString}")
    public void downloadFile(@PathVariable String digestString, HttpServletResponse response) throws IOException {
        // Send redirect
        response.sendRedirect(fileServerUrl + "/api/file/" + digestString);
    }
}
