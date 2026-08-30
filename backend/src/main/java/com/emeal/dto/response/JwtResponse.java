package com.emeal.dto.response;

public class JwtResponse {

    private String token;
    private String type = "Bearer";
    private UserDTO user;

    public JwtResponse() {
    }

    public JwtResponse(String token, String type, UserDTO user) {
        this.token = token;
        this.type = type != null ? type : "Bearer";
        this.user = user;
    }

    public static JwtResponseBuilder builder() {
        return new JwtResponseBuilder();
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public UserDTO getUser() { return user; }
    public void setUser(UserDTO user) { this.user = user; }

    public static class JwtResponseBuilder {
        private String token;
        private String type = "Bearer";
        private UserDTO user;

        JwtResponseBuilder() {}

        public JwtResponseBuilder token(String token) { this.token = token; return this; }
        public JwtResponseBuilder type(String type) { this.type = type; return this; }
        public JwtResponseBuilder user(UserDTO user) { this.user = user; return this; }

        public JwtResponse build() {
            return new JwtResponse(token, type, user);
        }
    }
}
