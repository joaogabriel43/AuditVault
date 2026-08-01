package com.auditvault.application.security.config;

import com.auditvault.application.security.UserContextResolver;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SpringSecurityUserContextResolver implements UserContextResolver {

    @Override
    public String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !authentication.getPrincipal().equals("anonymousUser")) {
            return authentication.getName(); // Returns the username
        }
        return "SYSTEM"; // Default for unauthenticated/system operations
    }
}
