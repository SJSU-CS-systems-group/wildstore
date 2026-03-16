package edu.sjsu.wildstore.wildstore_relationalDb;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FileNodeController {

  private static final String template = "Hello, %s!";
  private final AtomicLong counter = new AtomicLong();

  @GetMapping("/greeting")
  public FileNode greeting(@RequestParam(defaultValue = "World") String name) {
    return new FileNode(counter.incrementAndGet(), template.formatted(name));
  }
}
