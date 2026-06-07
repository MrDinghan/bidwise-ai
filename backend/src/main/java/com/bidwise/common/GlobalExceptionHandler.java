package com.bidwise.common;

import com.bidwise.auth.EmailAlreadyUsedException;
import com.bidwise.auth.InvalidCredentialsException;
import com.bidwise.bid.AuctionNotActiveException;
import com.bidwise.bid.BidTooLowException;
import com.bidwise.bid.InvalidBidException;
import com.bidwise.listing.IllegalListingStateException;
import com.bidwise.listing.ListingAccessDeniedException;
import com.bidwise.listing.ListingNotFoundException;
import com.bidwise.payment.DepositRequiredException;
import com.bidwise.payment.PaymentGatewayException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates domain and validation exceptions into uniform {@link ApiError} responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyUsedException.class)
    public ResponseEntity<ApiError> handleEmailAlreadyUsed(EmailAlreadyUsedException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiError> handleInvalidCredentials(InvalidCredentialsException ex) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(ListingNotFoundException.class)
    public ResponseEntity<ApiError> handleListingNotFound(ListingNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ListingAccessDeniedException.class)
    public ResponseEntity<ApiError> handleListingAccessDenied(ListingAccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(IllegalListingStateException.class)
    public ResponseEntity<ApiError> handleIllegalListingState(IllegalListingStateException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(InvalidBidException.class)
    public ResponseEntity<ApiError> handleInvalidBid(InvalidBidException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(AuctionNotActiveException.class)
    public ResponseEntity<ApiError> handleAuctionNotActive(AuctionNotActiveException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(BidTooLowException.class)
    public ResponseEntity<ApiError> handleBidTooLow(BidTooLowException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(DepositRequiredException.class)
    public ResponseEntity<ApiError> handleDepositRequired(DepositRequiredException ex) {
        return build(HttpStatus.PAYMENT_REQUIRED, ex.getMessage());
    }

    @ExceptionHandler(PaymentGatewayException.class)
    public ResponseEntity<ApiError> handlePaymentGateway(PaymentGatewayException ex) {
        return build(HttpStatus.BAD_GATEWAY, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getField() + " " + fieldError.getDefaultMessage())
                .orElse("Validation failed");
        return build(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(ApiError.of(status.value(), status.getReasonPhrase(), message));
    }
}
