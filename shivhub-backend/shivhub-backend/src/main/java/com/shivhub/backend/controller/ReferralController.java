package com.shivhub.backend.controller;
import java.security.Principal;
import org.springframework.web.bind.annotation.*;
import com.shivhub.backend.dto.ReferralResponse;
import com.shivhub.backend.service.ReferralService;
@RestController @RequestMapping("/api/referrals") public class ReferralController { private final ReferralService service; public ReferralController(ReferralService service){this.service=service;} @GetMapping("/me") public ReferralResponse mine(Principal principal){return service.mine(principal.getName());} @PostMapping("/invite") public void invite(Principal p,@RequestBody java.util.Map<String,String> body){service.invite(p.getName(),body.get("email"));} }
