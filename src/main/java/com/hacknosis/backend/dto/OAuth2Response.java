package com.hacknosis.backend.dto;

import lombok.Data;

import java.util.List;

@Data
public class OAuth2Response {
    private String refresh_token_expires_in;
    private String refresh_token_status;
    private String api_product_list;
    private List<String> api_product_list_json;
    private String organization_name;
    private String token_type;
    private String issued_at;
    private String client_id;
    private String access_token;
    private String refresh_token;
    private String application_name;
    private String scope;
    private String refresh_token_issued_at;
    private String expires_in;
    private String refresh_count;
    private String status;
}
