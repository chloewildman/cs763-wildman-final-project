package edu.bu.met.wildmancs763.discussions.api;

public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
