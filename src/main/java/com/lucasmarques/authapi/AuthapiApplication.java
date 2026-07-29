package com.lucasmarques.authapi;

import com.lucasmarques.authapi.dto.RegisterRequest;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AuthapiApplication {

	public static void main(String[] args) {
        RegisterRequest req = new RegisterRequest("Lucas", "");

        System.out.println(req);
	}


}
