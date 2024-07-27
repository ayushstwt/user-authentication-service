package com.narainox.spring_security.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class UserController {

    @GetMapping("/anyone")
    public ResponseEntity<String> anyone() {
        return ResponseEntity.ok("Anyone can access this resource");
    }

    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_MANAGER')")
    @GetMapping("/admin")
    public ResponseEntity<String> admin() {
        return ResponseEntity.ok("Admin can access this resource");
    }

    @PreAuthorize("hasRole('ROLE_MANAGER')")
    @GetMapping("/manager")
    public ResponseEntity<String> manager() {
        return ResponseEntity.ok("Manager can access this resource");
    }
}
