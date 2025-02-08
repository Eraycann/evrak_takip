package org.kafka.evraktakip.exception;

import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import lombok.Data;

@Data
public class ApiError {
    private HttpStatus status;
    private String message;
    private LocalDateTime timestamp;

    public ApiError(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }
}
