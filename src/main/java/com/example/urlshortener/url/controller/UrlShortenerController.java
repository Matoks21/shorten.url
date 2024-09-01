package com.example.urlshortener.url.controller;

import com.example.urlshortener.model.User;
import com.example.urlshortener.services.UserService;
import com.example.urlshortener.url.model.ShortUrl;
import com.example.urlshortener.url.service.UrlShortenerService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@Controller
public class UrlShortenerController {

    private static final Logger logger = LoggerFactory.getLogger(UrlShortenerController.class);

    private final UserService userService;
    private final UrlShortenerService urlShortenerService;

    @Autowired
    public UrlShortenerController(UserService userService, UrlShortenerService urlShortenerService) {
        this.userService = userService;
        this.urlShortenerService = urlShortenerService;
    }

    @Operation(summary = "Show URL shortening page", description = "Returns the page where users can shorten URLs.")
    @GetMapping("/url/shorten")
    public String showShortenPage() {
        return "url/shorten";
    }

    @Operation(summary = "Shorten URL", description = "Shortens a URL and returns the shortened URL.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Shortened URL created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid URL request")
    })
    @PostMapping("/url/shorten")
    public String shortenUrl(
            @Parameter(description = "Request body containing the original URL and optional expiry date") @ModelAttribute UrlRequest urlRequest,
            HttpServletRequest request,
            Model model
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            User user = getUserFromAuthentication(authentication);

            Duration expiryDate = urlRequest.getExpiryDate() != null ? urlRequest.getExpiryDate() : Duration.ofMinutes(5);
            ShortUrl shortUrl = urlShortenerService.createShortUrl(urlRequest.getOriginalUrl(), user, expiryDate);

            model.addAttribute("shortUrl", shortUrl);

            return "url/shorten";
        } else {
            System.out.println("Invalid or expired JWT token");
        }
        return "error";
    }

    @Operation(summary = "Redirect to original URL", description = "Redirects the user to the original URL based on the shortened URL.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "302", description = "Redirects to the original URL"),
            @ApiResponse(responseCode = "404", description = "Shortened URL not found or expired")
    })
    @GetMapping("/url/{shortUrl}")
    public String redirectToOriginalUrl(
            @Parameter(description = "Shortened URL") @PathVariable String shortUrl,
            HttpServletResponse response
    ) throws IOException {
        Optional<ShortUrl> shortUrlEntityOpt = urlShortenerService.getOriginalUrl(shortUrl);

        if (shortUrlEntityOpt.isEmpty() || urlShortenerService.isUrlExpired(shortUrlEntityOpt.get())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }

        ShortUrl shortUrlEntity = shortUrlEntityOpt.get();
        logger.info("Incrementing visit count for URL: {}", shortUrlEntity.getShortUrl());
        urlShortenerService.incrementVisitCount(shortUrlEntity);

        String originalUrl = shortUrlEntity.getOriginalUrl();
        return "redirect:" + originalUrl;
    }

    @Operation(summary = "Redirect to original URL for home", description = "Redirects the user to the original URL from home page.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "302", description = "Redirects to the original URL"),
            @ApiResponse(responseCode = "404", description = "Shortened URL not found")
    })
    @GetMapping("/{shortUrl}")
    public String redirectToOriginalUrlForHome(
            @Parameter(description = "Shortened URL") @PathVariable String shortUrl,
            HttpServletResponse response
    ) throws IOException {
        Optional<ShortUrl> shortUrlEntity = urlShortenerService.getOriginalUrl(shortUrl);
        if (shortUrlEntity.isPresent()) {
            ShortUrl url = shortUrlEntity.get();
            urlShortenerService.incrementVisitCount(url);
            response.sendRedirect(url.getOriginalUrl());
            return null;
        } else {
            return "error/404";
        }
    }

    @Operation(summary = "Get user's shortened URLs", description = "Returns a list of URLs shortened by the current user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of shortened URLs for the user"),
            @ApiResponse(responseCode = "404", description = "No URLs found for the user")
    })
    @GetMapping("/url/my-urls")
    public String getUserShortenedUrls(Model model, HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            System.out.println("Cookies are null, returning error page.");
            return "error";
        }

        User user = getUserFromAuthentication(authentication);
        System.out.println("Authenticated user: " + user.getUsername());

        List<ShortUrl> userUrls = urlShortenerService.getUrlsByUser(user);
        if (userUrls == null || userUrls.isEmpty()) {
            System.out.println("No URLs found for user: " + user.getUsername());
        } else {
            System.out.println("URLs found: " + userUrls);
        }

        model.addAttribute("urls", userUrls);
        return "url/my-urls";
    }

    @Operation(summary = "Delete URL", description = "Deletes a shortened URL based on the shortened URL identifier.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "302", description = "URL deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Unauthorized access"),
            @ApiResponse(responseCode = "404", description = "URL not found")
    })
    @PostMapping("/url/delete/{shortUrl}")
    public String deleteUrl(
            @Parameter(description = "Shortened URL to delete") @PathVariable String shortUrl,
            HttpServletRequest request,
            Model model
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = getUserFromAuthentication(authentication);

        try {
            ShortUrl shortUrlEntity = urlShortenerService.getOriginalUrl(shortUrl)
                    .orElseThrow(() -> new RuntimeException("URL not found"));

            urlShortenerService.deleteURL(shortUrlEntity, user);
            model.addAttribute("message", "URL deleted successfully");
        } catch (AccessDeniedException e) {
            model.addAttribute("error", "You are not authorized to delete this URL");
        } catch (EntityNotFoundException e) {
            model.addAttribute("error", "URL not found");
        }

        return "redirect:/url/my-urls";
    }

    private User getUserFromAuthentication(Authentication authentication) {
        String username = authentication.getName();
        return userService.loadUserByUsername(username);
    }
}


