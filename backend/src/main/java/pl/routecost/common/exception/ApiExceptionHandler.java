package pl.routecost.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pl.routecost.user.exception.EmailAlreadyRegisteredException;
import pl.routecost.user.exception.InvalidCredentialsException;
import pl.routecost.user.exception.PasswordTooLongException;
import pl.routecost.fuel.exception.FuelEntryChronologyException;
import pl.routecost.fuel.exception.FuelEntryNotFoundException;
import pl.routecost.expense.exception.ExpenseChronologyException;
import pl.routecost.expense.exception.ExpenseDateRangeException;
import pl.routecost.expense.exception.ExpenseNotFoundException;
import pl.routecost.expense.exception.InvalidExpenseCurrencyException;
import pl.routecost.vehicle.exception.OdometerRollbackException;
import pl.routecost.vehicle.exception.VehicleNotFoundException;
import pl.routecost.reminder.exception.InvalidReminderStatusTransitionException;
import pl.routecost.reminder.exception.ReminderChronologyException;
import pl.routecost.reminder.exception.ReminderDateRangeException;
import pl.routecost.reminder.exception.ReminderNotFoundException;

import java.util.Map;
import java.util.TreeMap;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    ProblemDetail handleEmailAlreadyRegistered(EmailAlreadyRegisteredException exception) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "An account with this email address already exists."
        );
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ProblemDetail handleInvalidCredentials(InvalidCredentialsException exception) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "Invalid email or password."
        );
    }

    @ExceptionHandler(PasswordTooLongException.class)
    ProblemDetail handlePasswordTooLong(PasswordTooLongException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Password must not exceed 72 bytes when encoded as UTF-8."
        );
        problem.setTitle("Invalid password");
        return problem;
    }

    @ExceptionHandler(VehicleNotFoundException.class)
    ProblemDetail handleVehicleNotFound(VehicleNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Vehicle was not found.");
    }

    @ExceptionHandler(OdometerRollbackException.class)
    ProblemDetail handleOdometerRollback(OdometerRollbackException exception) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Odometer cannot be lower than the last recorded vehicle mileage."
        );
    }

    @ExceptionHandler(FuelEntryNotFoundException.class)
    ProblemDetail handleFuelEntryNotFound(FuelEntryNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Fuel entry was not found.");
    }

    @ExceptionHandler(FuelEntryChronologyException.class)
    ProblemDetail handleFuelEntryChronology(FuelEntryChronologyException exception) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Fuel entries must not predate vehicle purchase or move the odometer backwards in date order."
        );
    }

    @ExceptionHandler(ExpenseNotFoundException.class)
    ProblemDetail handleExpenseNotFound(ExpenseNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Expense was not found.");
    }

    @ExceptionHandler(ExpenseChronologyException.class)
    ProblemDetail handleExpenseChronology(ExpenseChronologyException exception) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Expense date or odometer conflicts with the vehicle's recorded history."
        );
    }

    @ExceptionHandler(ExpenseDateRangeException.class)
    ProblemDetail handleExpenseDateRange(ExpenseDateRangeException exception) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "The start date must not be after the end date."
        );
    }

    @ExceptionHandler(InvalidExpenseCurrencyException.class)
    ProblemDetail handleInvalidExpenseCurrency(InvalidExpenseCurrencyException exception) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Currency must be a supported ISO 4217 currency code."
        );
    }

    @ExceptionHandler(ReminderNotFoundException.class)
    ProblemDetail handleReminderNotFound(ReminderNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Reminder was not found.");
    }

    @ExceptionHandler(ReminderDateRangeException.class)
    ProblemDetail handleReminderDateRange(ReminderDateRangeException exception) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "The due date start must not be after the due date end."
        );
    }

    @ExceptionHandler(ReminderChronologyException.class)
    ProblemDetail handleReminderChronology(ReminderChronologyException exception) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Reminder due date must not precede the vehicle purchase date."
        );
    }

    @ExceptionHandler(InvalidReminderStatusTransitionException.class)
    ProblemDetail handleInvalidReminderStatusTransition(InvalidReminderStatusTransitionException exception) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "This reminder status transition is not allowed. Use the explicit reactivate operation to reactivate it."
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "One or more request fields are invalid."
        );
        problem.setTitle("Validation failed");

        Map<String, String> errors = new TreeMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                errors.putIfAbsent(error.getField(), error.getDefaultMessage())
        );
        problem.setProperty("errors", errors);
        return problem;
    }
}
