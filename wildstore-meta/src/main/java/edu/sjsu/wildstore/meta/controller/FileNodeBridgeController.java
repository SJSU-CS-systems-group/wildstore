package edu.sjsu.wildstore.meta.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import edu.sjsu.wildstore.wildstore_relationalDb.records.FileNodeRecord;

@RestController
public class FileNodeBridgeController {
    @PreAuthorize("hasRole('GUEST')")
    @GetMapping("/file_contents")
    public List<FileNodeRecord> files(@RequestParam(value = "file_id", defaultValue = "0") String fileId) {
        try {
            Class<?> controllerClass = Class.forName("edu.sjsu.wildstore.wildstore_relationalDb.controller.FileNodeController");
            Object fileNodeController = controllerClass.getDeclaredConstructor().newInstance();
            Object response = controllerClass.getMethod("files", String.class).invoke(fileNodeController, fileId);
            return (List<FileNodeRecord>) response;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to load FileNodeController data", e);
        }
    }
}
