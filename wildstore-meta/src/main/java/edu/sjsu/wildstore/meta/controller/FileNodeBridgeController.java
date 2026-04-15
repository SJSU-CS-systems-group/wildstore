package edu.sjsu.wildstore.meta.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import edu.sjsu.wildstore.wildstore_relationalDb.controller.FileNodeController;
import edu.sjsu.wildstore.wildstore_relationalDb.resources.FileNode;


@RestController
public class FileNodeBridgeController {
    private final FileNodeController fileNodeController;
    public FileNodeBridgeController(FileNodeController fileNodeController) {
        this.fileNodeController = fileNodeController;
    }
    @PreAuthorize("hasRole('GUEST')")
    @GetMapping("/file_contents")
    public List<FileNode> files(@RequestParam(value = "file_id", defaultValue = "0") String fileId) {
        try {
            return fileNodeController.files(fileId);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to load FileNodeController data", e);
        }
    }
}
