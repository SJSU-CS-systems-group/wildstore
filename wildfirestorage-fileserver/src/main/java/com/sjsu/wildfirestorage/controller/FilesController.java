package com.sjsu.wildfirestorage.controller;

import com.sjsu.wildfirestorage.Metadata;
import com.sjsu.wildfirestorage.MetadataRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.*;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class FilesController {

    @Value("${custom.metadataServer}")
    private String metadataServerUrl;

    @GetMapping("/file/{digestString}")
    public void downloadFile(@PathVariable String digestString, HttpServletRequest request, HttpServletResponse response) {
        final String uri = metadataServerUrl + "/api/metadata/" + digestString;

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", request.getHeader("Authorization"));
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<Metadata> result = restTemplate.exchange(uri, HttpMethod.GET, entity, Metadata.class);
        if(result.getBody() == null) {
            return;
        }
        downloadHelper(result.getBody(), request, response);
    }

    private void downloadHelper(Metadata result, HttpServletRequest request, HttpServletResponse response) {
        try {
            RandomAccessFile file = new RandomAccessFile((String)result.filePath.toArray()[0], "r");
            long fileLength = file.length();

            String rangeHeader = request.getHeader("Range");
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

                byte[] buffer = new byte[4096];
                int bytesRead;
                long bytesRemaining = contentLength;

                file.seek(start);
                while (bytesRemaining > 0) {
                    bytesRead = file.read(buffer, 0, (int) Math.min(buffer.length, bytesRemaining));
                    if (bytesRead >= 0) {
                        response.getOutputStream().write(buffer, 0, bytesRead);
                        bytesRemaining -= bytesRead;
                    } else {
                        break;
                    }
                }
            } else {
                response.setStatus(HttpStatus.OK.value());
                response.setHeader("Content-Length", String.valueOf(fileLength));
                response.setHeader("Content-Disposition", "attachment; filename=" + (String)result.fileName.toArray()[0]);
                response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);

                byte[] buffer = new byte[4096];
                int bytesRead;

                while ((bytesRead = file.read(buffer)) != -1) {
                    response.getOutputStream().write(buffer, 0, bytesRead);
                }
            }

            file.close();
        } catch (IOException e) {
            e.printStackTrace();
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    @GetMapping("/share/{shareId}")
    public void downloadSharedFile(@PathVariable String shareId, HttpServletRequest request, HttpServletResponse response) {
        final String verifyUri = metadataServerUrl + "/api/share-link/verify";

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", request.getHeader("Authorization"));
        HttpEntity<String> entity = new HttpEntity<>(shareId, headers);
        Metadata result = restTemplate.postForObject(verifyUri, entity, Metadata.class);
        if(result == null) {
            return;
        }
        downloadHelper(result, request, response);

        restTemplate.postForObject(metadataServerUrl + "/api/share-link/downloadhistory", entity, Integer.class);
    }
}