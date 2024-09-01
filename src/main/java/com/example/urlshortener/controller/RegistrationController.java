package com.example.urlshortener.controller;


import com.example.urlshortener.model.*;
import com.example.urlshortener.services.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@Controller
public class RegistrationController {

    private final UserService userService;

    public RegistrationController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Show registration form", description = "Returns the registration page.")
    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
        return "registration";
    }

    @Operation(summary = "Register new user", description = "Register a new user and redirect to login page.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "302", description = "Redirect to login page"),
            @ApiResponse(responseCode = "400", description = "Validation errors")
    })
    @PostMapping("/register")
    public String registerUser(
            @Parameter(description = "User details for registration") @ModelAttribute("user") @Valid User user,
            BindingResult result
    ) {
        userService.registerUser(user, result);

        if (result.hasErrors()) {
            return "registration";
        }

        return "redirect:/login";
    }
}
