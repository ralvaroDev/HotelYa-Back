package pe.ralvaro.hotelyaback.controller;

import java.io.IOException;
import java.util.*;
import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pe.ralvaro.hotelyaback.dto.response.MediaInfo;
import pe.ralvaro.hotelyaback.model.MediaFile;
import pe.ralvaro.hotelyaback.service.MediaService;

@RestController
public class MediaController {
    private final MediaService service;
    public MediaController(MediaService service) { this.service=service; }
    @GetMapping("/api/owner/media") public List<MediaInfo> list(@AuthenticationPrincipal Jwt jwt) { return service.list(Long.valueOf(jwt.getSubject())); }
    @PostMapping("/api/owner/media") @ResponseStatus(HttpStatus.CREATED) public MediaInfo upload(@AuthenticationPrincipal Jwt jwt,@RequestParam("file") MultipartFile file,@RequestParam(required=false) String roomScope) throws IOException { return service.upload(Long.valueOf(jwt.getSubject()),file,roomScope); }
    @DeleteMapping("/api/owner/media/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id) { service.delete(Long.valueOf(jwt.getSubject()),id); }
    @GetMapping("/api/owner/media/{id}") public ResponseEntity<Resource> preview(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id) { return response(service.privateFile(Long.valueOf(jwt.getSubject()),id)); }
    @GetMapping("/api/media/{id}") public ResponseEntity<Resource> publicFile(@PathVariable UUID id) { return response(service.publicFile(id)); }
    private ResponseEntity<Resource> response(MediaFile file) { return ResponseEntity.ok().contentType(MediaType.parseMediaType(file.contentType))
        .header("X-Content-Type-Options","nosniff").header("Content-Disposition","inline").header("Cache-Control","no-store")
        .contentLength(file.byteSize).body(new FileSystemResource(service.path(file.id))); }
}
