package com.triptalker.triptalk.domain.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminController {

    @Operation(hidden = true)
    @GetMapping("/admin")
    public String admin() {
        return "admin controller";
    }
}
