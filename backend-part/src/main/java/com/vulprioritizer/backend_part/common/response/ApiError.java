package com.vulprioritizer.backend_part.common.response;

import lombok.Data;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
@Data
public class ApiError {
    private LocalDateTime timeStamp;
    private String error;
    private HttpStatus status;
    private String path;

    ApiError(){
        this.timeStamp=LocalDateTime.now();
    }
    public ApiError(String error, HttpStatus status, String path){
        this();
        this.error=error;
        this.status=status;
        this.path=path;
    }
}
