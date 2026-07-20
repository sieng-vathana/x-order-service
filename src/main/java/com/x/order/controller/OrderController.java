package com.x.order.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    @GetMapping("/echo/{text}")
    public String echoText(@PathVariable String text) {
        return "The Order Service received your variable: " + text;
    }
}
