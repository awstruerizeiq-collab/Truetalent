package com.truerize.dto;

import java.util.Set;

public class LoginResponse {
    private String message;
    private Set<String> roles; 
    private String redirectPage; 

    public LoginResponse(String message, Set<String> roles) {
        this.message = message;
        this.roles = roles;
    }

    public LoginResponse(String message, Set<String> roles, String redirectPage) {
        this.message = message;
        this.roles = roles;
        this.redirectPage = redirectPage;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Set<String> getRoles() { return roles; }
    public void setRoles(Set<String> roles) { this.roles = roles; }

    public String getRedirectPage() { return redirectPage; }
    public void setRedirectPage(String redirectPage) { this.redirectPage = redirectPage; }
}
