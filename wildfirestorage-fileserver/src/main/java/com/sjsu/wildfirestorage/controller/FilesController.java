package com.sjsu.wildfirestorage.controller;

import com.sjsu.wildfirestorage.Metadata;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

@RestController
@RequestMapping("/api")
public class FilesController {

    private static final Logger log = LoggerFactory.getLogger(FilesController.class);
    @Value("${custom.metadataServer}")
    private String metadataServerUrl;

    @CrossOrigin(origins = "http://localhost:3000")
    @GetMapping("/file/{digestString}")
    public void downloadFile(@PathVariable String digestString, HttpServletRequest request, HttpServletResponse response) {
        final String uri = metadataServerUrl + "/api/metadata/" + digestString;

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", request.getHeader("Authorization"));
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<Metadata> result = restTemplate.exchange(uri, HttpMethod.GET, entity, Metadata.class);
        if (result.getBody() == null) {
            return;
        }
        downloadHelper(result.getBody(), request, response);
    }

    private void downloadHelper(Metadata result, HttpServletRequest request, HttpServletResponse response) {
        File fileToOpen = null;
        for (var fileName : result.fileName) {
            var file = new File(fileName);
            if (file.canRead()) {
                log.info("Found to download: " + file.getAbsolutePath());
                fileToOpen = file;
                break;
            }
        }
        ServletOutputStream outputStream;
        try {
            outputStream = response.getOutputStream();
        } catch (IOException e) {
            log.info("Error getting output stream", e);
            return;
        }
        if (fileToOpen == null) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            response.setContentType(MediaType.TEXT_HTML_VALUE);
            try {
                outputStream.write("File is missing. Please contact the content owner.".getBytes());
            } catch (IOException ignore) {}
            return;
        }
        try (var file = new RandomAccessFile(fileToOpen, "r")) {
            long fileLength = file.length();

            String rangeHeader = request.getHeader("Range");
            log.info("Starting to transfer " + fileToOpen + " Range header: " + rangeHeader);
            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                String[] ranges = rangeHeader.substring(6).split("-");
                long start = Long.parseLong(ranges[0]);
                long end = ranges.length > 1 ? Long.parseLong(ranges[1]) : fileLength - 1;

                if (start >= fileLength) {
                    response.setStatus(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE.value());
                    response.setHeader("Content-Range", "bytes */" + fileLength);
                    return;
                }

                long contentLength = end - start + 1;
                response.setStatus(HttpStatus.PARTIAL_CONTENT.value());
                response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileLength);
                response.setHeader("Content-Length", String.valueOf(contentLength));
                response.setHeader("Accept-Ranges", "bytes");
                response.setHeader("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE);

                byte[] buffer = new byte[65536];
                int bytesRead;
                long bytesRemaining = contentLength;

                file.seek(start);
                while (bytesRemaining > 0) {
                    bytesRead = file.read(buffer, 0, (int) Math.min(buffer.length, bytesRemaining));
                    if (bytesRead >= 0) {
                        outputStream.write(buffer, 0, bytesRead);
                        bytesRemaining -= bytesRead;
                    } else {
                        break;
                    }
                }
            } else {
                response.setStatus(HttpStatus.OK.value());
                response.setHeader("Content-Length", String.valueOf(fileLength));
                response.setHeader("Content-Disposition", "attachment; filename=" + fileToOpen);
                response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);

                byte[] buffer = new byte[1024*1024];
                int bytesRead;
                long totalRead = 0;

                while ((bytesRead = file.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    outputStream.flush();
                    totalRead += bytesRead;
                }
                log.info("Finished writing file " + fileToOpen + " last " + bytesRead + " total bytes read: " + totalRead);
            }
        } catch (Exception e) {
            log.info("download error", e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setContentType(MediaType.TEXT_HTML_VALUE);
            try {
                outputStream.println("Error: " + e.getMessage());
            } catch (IOException ignore) {}
        }
    }

    @GetMapping("/share/{shareId}")
    public void downloadSharedFile(@PathVariable String shareId, HttpServletRequest request, HttpServletResponse response) {
        try {
            final String verifyUri = metadataServerUrl + "/api/share-link/verify";

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.add("Authorization", request.getHeader("Authorization"));
            HttpEntity<String> entity = new HttpEntity<>(shareId, headers);
            Metadata result = restTemplate.postForObject(verifyUri, entity, Metadata.class);
            if (result == null) {
                return;
            }
            downloadHelper(result, request, response);

            restTemplate.postForObject(metadataServerUrl + "/api/share-link/downloadhistory", entity, Integer.class);
        } catch (HttpStatusCodeException ex) {
            var httpStatus = ex.getStatusCode();
            if (httpStatus != HttpStatus.UNAUTHORIZED) {
                response.setStatus(httpStatus.value());
            } else {
                try {
                    var token = request.getHeader("Authorization");
                    // we need to change to FORBIDDEN because UNAUTHORIZED will cause the
                    // content to be ignored in wget. stupid wget!
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    var os = response.getOutputStream();
                    os.println("You do not have access to this resource.");
                    if (token == null) {
                        os.println("Please use a token for this resource. Examples:");
                        os.println("<pre>");
                        os.println("    curl -H \"Authorization: Bearer TOKEN_FROM_WEB\" URL");
                        os.println("    wget --header=\"Authorization: Bearer TOKEN_FROM_WEB\" URL");
                        os.println("</pre>");
                    } else {
                        os.println("I see you used a token, but it is not valid for this resource.");
                    }
                } catch (IOException e) {
                    log.error("Error writing to output stream: " + e.getMessage());
                }
            }
        }
    }
}
