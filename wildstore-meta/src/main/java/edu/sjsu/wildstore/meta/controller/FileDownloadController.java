package edu.sjsu.wildstore.meta.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
