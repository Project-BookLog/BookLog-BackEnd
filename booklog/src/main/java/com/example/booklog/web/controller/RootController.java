package com.example.booklog.web.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootController {

    @GetMapping("/")
    public String root() {
        return "BookLog API Server is running";
    }

    @GetMapping("/health")
    public String healthCheck() {
        return "I'm healthy!";
    }
}
